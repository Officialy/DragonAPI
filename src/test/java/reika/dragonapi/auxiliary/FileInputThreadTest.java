package reika.dragonapi.auxiliary;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import static org.junit.jupiter.api.Assertions.*;

class FileInputThreadTest {
    @TempDir Path directory;

    @Test void exactUtf8TextAndQueuedSnapshotArePublishedOnlyAfterExecution() {
        byte[] bytes = "one\r\né水😀\nlast\r".getBytes(StandardCharsets.UTF_8);
        var load = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setData(bytes);
        java.util.Arrays.fill(bytes, (byte)'x');
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        var future = load.submit(queued::add).toCompletableFuture();
        assertEquals(FileInputThread.State.QUEUED, load.getState()); assertNull(load.getFile()); assertFalse(future.isDone());
        assertThrows(IllegalStateException.class, () -> load.setFiletype(FileInputThread.FileType.XML));
        assertThrows(IllegalStateException.class, load::run);
        queued.getFirst().run();
        assertEquals("one\r\né水😀\nlast\r", future.join()); assertSame(load.getFile(), future.join());
        assertEquals(FileInputThread.State.SUCCEEDED, load.getState()); assertNull(load.getFailure());
        assertThrows(IllegalStateException.class, () -> load.submit(Runnable::run));
    }

    @Test void imageAndNamespaceAwareXmlDecodeIntoOwnedHeapResults() throws IOException {
        var original = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        original.setRGB(0, 0, 0xff123456); original.setRGB(1, 0, 0x80123456);
        var encoded = new ByteArrayOutputStream(); assertTrue(ImageIO.write(original, "png", encoded));
        var imageLoad = new FileInputThread().setFiletype(FileInputThread.FileType.IMAGE).setData(encoded.toByteArray()); imageLoad.run();
        var image = (BufferedImage)imageLoad.getFile();
        assertEquals(2, image.getWidth()); assertEquals(1, image.getHeight());
        assertEquals(original.getRGB(0, 0), image.getRGB(0, 0)); assertEquals(original.getRGB(1, 0), image.getRGB(1, 0));
        var xml = new FileInputThread().setFiletype(FileInputThread.FileType.XML)
                .setData("<r:root xmlns:r='urn:test'><r:value>水</r:value></r:root>".getBytes(StandardCharsets.UTF_8)); xml.run();
        var document = (Document)xml.getFile();
        assertEquals("urn:test", document.getDocumentElement().getNamespaceURI());
        assertEquals("水", document.getElementsByTagNameNS("urn:test", "value").item(0).getTextContent());
    }

    @Test void encodedSoundBytesAndFilesystemSourcesRetainEveryByte() throws IOException {
        byte[] bytes = new byte[]{'O', 'g', 'g', 'S', 0, (byte)255, 42, 0};
        Path file = directory.resolve("sound.ogg"); Files.write(file, bytes);
        var load = new FileInputThread().setFiletype(FileInputThread.FileType.SOUND).setFile(file); load.run();
        assertArrayEquals(bytes, (byte[])load.getFile());
        var fromBytes = new FileInputThread().setFiletype(FileInputThread.FileType.SOUND).setData(bytes); fromBytes.run();
        ((byte[])fromBytes.getFile())[0] = 0;
        assertEquals('O', bytes[0]);
    }

    @Test void malformedFormatsAndMissingResourcesPublishOriginalFailures() {
        for (var type : java.util.List.of(FileInputThread.FileType.IMAGE, FileInputThread.FileType.XML, FileInputThread.FileType.TEXT)) {
            byte[] malformed = type == FileInputThread.FileType.TEXT ? new byte[]{(byte)0xc3, 0x28} : "not a document or image".getBytes(StandardCharsets.UTF_8);
            var load = new FileInputThread().setFiletype(type).setData(malformed); load.run();
            assertEquals(FileInputThread.State.FAILED, load.getState()); assertInstanceOf(IOException.class, load.getFailure());
            assertSame(load.getFailure(), assertThrows(CompletionException.class, load::getFile).getCause());
            assertSame(load.getFailure(), assertThrows(CompletionException.class, () -> load.completion().toCompletableFuture().join()).getCause());
        }
        var missing = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setFilepath(getClass(), "not-present.txt"); missing.run();
        assertInstanceOf(java.io.FileNotFoundException.class, missing.getFailure());
        var unconfigured = new FileInputThread(); unconfigured.run();
        assertInstanceOf(IllegalStateException.class, unconfigured.getFailure());
    }

    @Test void sourceClosesBeforeSuccessAndReadFailureRetainsSuppressedCloseFailure() {
        var closed = new AtomicInteger();
        var text = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setSource(() -> new ByteArrayInputStream(new byte[]{'x'}) {
            @Override public void close() { closed.incrementAndGet(); }
        });
        var observed = text.completion().thenAccept(value -> assertEquals(1, closed.get())).toCompletableFuture(); text.run(); observed.join();
        assertEquals("x", text.getFile()); assertEquals(1, closed.get());
        var readFailure = new IOException("read failure"); var closeFailure = new IOException("close failure");
        var failed = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setSource(() -> new InputStream() {
            public int read() throws IOException { throw readFailure; }
            public void close() throws IOException { closed.incrementAndGet(); throw closeFailure; }
        }); failed.run();
        assertSame(readFailure, failed.getFailure()); assertArrayEquals(new Throwable[]{closeFailure}, readFailure.getSuppressed());
        assertEquals(2, closed.get());
    }

    @Test void concurrentRunAndConfigurationCannotReplaceAnInFlightLoad() throws Exception {
        var opened = new CountDownLatch(1); var release = new CountDownLatch(1); var closes = new AtomicInteger();
        var load = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setSource(() -> {
            opened.countDown();
            try { if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("fixture timed out"); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException(error); }
            return new ByteArrayInputStream(new byte[]{'a'}) { @Override public void close() { closes.incrementAndGet(); } };
        });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var result = load.submit(executor).toCompletableFuture(); assertTrue(opened.await(10, TimeUnit.SECONDS));
            assertEquals(FileInputThread.State.RUNNING, load.getState()); assertNull(load.getFile());
            assertThrows(IllegalStateException.class, load::run);
            assertThrows(IllegalStateException.class, () -> load.setSource(() -> InputStream.nullInputStream()));
            release.countDown(); assertEquals("a", result.get(10, TimeUnit.SECONDS)); assertEquals(1, closes.get());
        } finally { release.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    @Test void executorRejectionAndObserverMutationCannotHideTheActualOutcome() {
        var opens = new AtomicInteger(); var rejection = new RejectedExecutionException("test rejection");
        var rejected = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setSource(() -> { opens.incrementAndGet(); return InputStream.nullInputStream(); });
        rejected.submit(task -> { throw rejection; });
        assertSame(rejection, rejected.getFailure()); assertEquals(0, opens.get());
        var load = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setData(new byte[]{'a'});
        load.completion().toCompletableFuture().complete("observer value"); load.run();
        assertEquals("a", load.getFile()); assertEquals("a", load.completion().toCompletableFuture().join());
    }

    @Test void classpathRootRetainsRelativeAndAbsoluteResourceSemantics() {
        for (String path : java.util.List.of("file-input-fixture.txt", "/reika/dragonapi/auxiliary/file-input-fixture.txt")) {
            var load = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setFilepath(getClass(), path);
            load.run(); assertEquals("File input fixture αβ", ((String)load.getFile()).stripTrailing());
        }
    }

    @Test void xmlExternalReferencesAreRejectedAndInterruptStatusIsPreserved() {
        var xml = new FileInputThread().setFiletype(FileInputThread.FileType.XML)
                .setData("<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///not-an-authorized-input'>]><root>&x;</root>".getBytes(StandardCharsets.UTF_8)); xml.run();
        assertInstanceOf(IOException.class, xml.getFailure());
        try {
            Thread.currentThread().interrupt();
            var load = new FileInputThread().setFiletype(FileInputThread.FileType.TEXT).setData(new byte[]{'x'}); load.run();
            assertInstanceOf(java.io.InterruptedIOException.class, load.getFailure()); assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
