/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** One-shot, common-side file loading. Configuration freezes before scheduling; input streams are owned and closed. */
public class FileInputThread implements Runnable {
    public enum State { NEW, QUEUED, RUNNING, SUCCEEDED, FAILED }
    private record Outcome(State state, Object result, Throwable failure) {}
    private record Configuration(FileType type, StreamSource input) {}
    private volatile Outcome outcome = new Outcome(State.NEW, null, null);
    private final CompletableFuture<Object> completion = new CompletableFuture<>();
    private StreamSource input;
    private FileType type;

    /** Opens a fresh, worker-safe stream. Ownership transfers to the loader only after open returns. */
    @FunctionalInterface public interface StreamSource { InputStream open() throws IOException; }

    @Override
    public void run() {
        Configuration configuration;
        synchronized (this) {
            ensureNew();
            configuration = new Configuration(type, input);
            input = null;
            outcome = new Outcome(State.RUNNING, null, null);
        }
        load(configuration);
    }

    /** Schedule exactly once on the caller's executor; rejection is reported through the same result contract. */
    public CompletionStage<Object> submit(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        Configuration configuration;
        synchronized (this) {
            ensureNew();
            configuration = new Configuration(type, input);
            input = null;
            outcome = new Outcome(State.QUEUED, null, null);
        }
        try {
            executor.execute(() -> {
                synchronized (this) {
                    if (outcome.state != State.QUEUED) return;
                    outcome = new Outcome(State.RUNNING, null, null);
                }
                load(configuration);
            });
        } catch (Throwable failure) {
            boolean rejected;
            synchronized (this) {
                rejected = outcome.state == State.QUEUED;
                if (rejected) outcome = new Outcome(State.FAILED, null, failure);
            }
            if (rejected) completion.completeExceptionally(failure);
            if (failure instanceof Error fatal) throw fatal;
        }
        return completion();
    }

    private void load(Configuration configuration) {
        try {
            if (configuration.type == null || configuration.input == null)
                throw new IllegalStateException("Configure a file type and input source before loading");
            checkInterrupted();
            byte[] bytes;
            try (InputStream stream = configuration.input.open()) {
                if (stream == null) throw new FileNotFoundException("File input source returned no stream");
                bytes = stream.readAllBytes();
            }
            checkInterrupted();
            Object result = decode(configuration.type, bytes);
            checkInterrupted();
            outcome = new Outcome(State.SUCCEEDED, result, null);
            completion.complete(result);
        } catch (Throwable failure) {
            outcome = new Outcome(State.FAILED, null, failure);
            completion.completeExceptionally(failure);
            if (failure instanceof Error fatal) throw fatal;
        }
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("File loading interrupted");
    }

    private static Object decode(FileType type, byte[] bytes) throws IOException {
        return switch (type) {
            case TEXT -> StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            case SOUND -> bytes; // Encoded bytes, detached from the source; audio-engine decoding/playback is explicitly separate.
            case IMAGE -> {
                try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers = ImageIO.getImageReaders(imageInput);
                    if (!readers.hasNext()) throw new IOException("Unsupported or malformed image data");
                    var reader = readers.next();
                    try {
                        reader.setInput(imageInput, true, true);
                        BufferedImage image = reader.read(0);
                        if (image == null) throw new IOException("Image reader returned no image");
                        yield image;
                    } finally { reader.dispose(); }
                }
            }
            case XML -> parseXml(bytes);
        };
    }

    private static Document parseXml(byte[] bytes) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException error) throws SAXException { throw error; }
                @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
            });
            return builder.parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException | SAXException failure) {
            throw new IOException("Cannot decode XML file input", failure);
        }
    }

    /** Null while pending; failures are observable rather than indistinguishable from an unfinished load. */
    public Object getFile() {
        Outcome current = outcome;
        if (current.state == State.FAILED) throw new CompletionException(current.failure);
        return current.result;
    }

    public State getState() { return outcome.state; }
    public Throwable getFailure() { return outcome.failure; }
    public CompletionStage<Object> completion() { return completion.minimalCompletionStage(); }

    public synchronized FileInputThread setFiletype(FileType type) {
        ensureNew();
        this.type = Objects.requireNonNull(type, "file type");
        return this;
    }

    /** Retains Class.getResourceAsStream's package-relative / leading-slash absolute path semantics. */
    public synchronized FileInputThread setFilepath(Class<?> root, String path) {
        ensureNew();
        Objects.requireNonNull(root, "resource root"); Objects.requireNonNull(path, "resource path");
        if (path.isEmpty()) throw new IllegalArgumentException("Resource path cannot be empty");
        return setSource(() -> {
            InputStream stream = root.getResourceAsStream(path);
            if (stream == null) throw new FileNotFoundException(root.getName() + ":" + path);
            return stream;
        });
    }

    public synchronized FileInputThread setFile(Path file) {
        ensureNew();
        Path captured = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        return setSource(() -> Files.newInputStream(captured));
    }

    /** Capture bytes on their owner thread; caller mutation cannot change the queued input. */
    public synchronized FileInputThread setData(byte[] bytes) {
        ensureNew();
        byte[] captured = Objects.requireNonNull(bytes, "bytes").clone();
        return setSource(() -> new ByteArrayInputStream(captured));
    }

    /** Source acquisition runs on the loading executor: do not look up live world/client resource-manager state here. */
    public synchronized FileInputThread setSource(StreamSource source) {
        ensureNew();
        input = Objects.requireNonNull(source, "input source");
        return this;
    }

    private void ensureNew() {
        if (outcome.state != State.NEW) throw new IllegalStateException("File input is already scheduled or completed: " + outcome.state);
    }

    public enum FileType {
        IMAGE, // BufferedImage (heap data; no GPU texture)
        TEXT,  // UTF-8 String, with original line endings
        XML,   // Namespace-aware DOM Document, without external resource resolution
        SOUND  // byte[] of original encoded sound data; no native audio ownership
    }

}
