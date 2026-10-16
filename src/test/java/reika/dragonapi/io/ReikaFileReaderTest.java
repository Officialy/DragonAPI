package reika.dragonapi.io;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ReikaFileReaderTest {
    @TempDir Path directory;

    @Test void suppliedWriterClosesOnceOnSuccessAndRetainsNullLineAndSeparatorBehavior() {
        int[] closes = {0};
        var output = new java.io.StringWriter() {
            @Override public void close() { closes[0]++; }
        };
        var lines = new java.util.ArrayList<>(java.util.Arrays.asList("first", null, "last"));
        ReikaFileReader.writeLinesToFile(new java.io.BufferedWriter(output), lines, true);
        assertEquals("first" + System.lineSeparator() + "null" + System.lineSeparator() + "last" + System.lineSeparator(), output.toString());
        assertEquals(1, closes[0]);
    }

    @Test void suppliedWriterClosesEvenWhenAnEarlyWriteFails() {
        int[] closes = {0};
        var output = new java.io.Writer() {
            @Override public void write(char[] buffer, int offset, int length) throws java.io.IOException { throw new java.io.IOException("expected write failure"); }
            @Override public void flush() {}
            @Override public void close() { closes[0]++; }
        };
        ReikaFileReader.writeLinesToFile(new java.io.BufferedWriter(output, 1), new java.util.ArrayList<>(List.of("longer than buffer")), false);
        assertEquals(1, closes[0], "Write failure must not leak the owned writer");
    }

    @Test void explicitCharsetFileAndStreamOutputsAgreeIncludingTrailingNewline() throws Exception {
        var lines = new java.util.ArrayList<>(List.of("snowman \u2603", "second"));
        Path file = directory.resolve("output.txt");
        ReikaFileReader.writeLinesToFile(file.toFile(), lines, true, StandardCharsets.UTF_16LE);
        byte[] expected = ("snowman \u2603" + System.lineSeparator() + "second" + System.lineSeparator()).getBytes(StandardCharsets.UTF_16LE);
        assertArrayEquals(expected, Files.readAllBytes(file));
        try (var stream = ReikaFileReader.convertLinesToStream(lines, true, StandardCharsets.UTF_16LE)) {
            assertArrayEquals(expected, stream.readAllBytes());
        }
        var writer = ReikaFileReader.getWriter(file.toFile());
        assertNotNull(writer);
        try (writer) { writer.write("default charset"); }
        assertEquals("default charset", Files.readString(file, java.nio.charset.Charset.defaultCharset()));
    }

    @Test void invalidBinaryPayloadCannotDestroyExistingFileAndReplacementRetainsAllBytes() throws Exception {
        Path file = directory.resolve("output.bin");
        Files.writeString(file, "original");
        var malformed = new java.util.ArrayList<Byte>(); malformed.add((byte)1); malformed.add(null);
        assertThrows(NullPointerException.class, () -> ReikaFileReader.writeDataToFile(file.toFile(), malformed, false));
        assertEquals("original", Files.readString(file));
        byte[] expected = new byte[256];
        var values = new java.util.ArrayList<Byte>();
        for (int i = 0; i < expected.length; i++) { expected[i] = (byte)i; values.add((byte)i); }
        ReikaFileReader.writeDataToFile(file.toFile(), values, true);
        assertArrayEquals(expected, Files.readAllBytes(file));
        Path nested = directory.resolve("nested/bytes.bin");
        ReikaFileReader.writeDataToFile(nested.toFile(), values, true);
        assertArrayEquals(expected, Files.readAllBytes(nested));
        try (var paths = Files.list(directory)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().startsWith("reika-data-")));
        }
    }

    @Test void unfilteredTraversalIncludesNestedFilesWithoutNullExtensionFailures() throws Exception {
        Files.writeString(directory.resolve("first.txt"), "first");
        Files.createDirectory(directory.resolve("child"));
        Files.writeString(directory.resolve("child/second.bin"), "second");
        var result = ReikaFileReader.getAllFilesInFolder(directory.toFile());
        assertEquals(2, result.size());
        assertTrue(result.contains(directory.resolve("first.txt").toFile()));
        assertTrue(result.contains(directory.resolve("child/second.bin").toFile()));
        assertEquals(2, ReikaFileReader.getAllFilesInFolder(directory.toFile(), new String[0]).size());
        assertTrue(ReikaFileReader.getAllFilesInFolder(directory.resolve("absent").toFile()).isEmpty());
    }

    @Test void overlappingExtensionFiltersDoNotDuplicateFilesAndRetainCaseSensitivity() throws Exception {
        Files.writeString(directory.resolve("match.txt"), "yes");
        Files.writeString(directory.resolve("different.TXT"), "case sensitive");
        Files.writeString(directory.resolve("other.bin"), "no");
        assertEquals(List.of(directory.resolve("match.txt").toFile()),
                ReikaFileReader.getAllFilesInFolder(directory.toFile(), "txt", ".txt"));
    }

    @Test void directoryIdentityCyclesAreBoundedWithoutDependingOnWindowsLinkPrivileges() throws Exception {
        File child = Files.writeString(directory.resolve("child.txt"), "data").toFile();
        File cycle = new File(directory.toString()) {
            @Override public File[] listFiles() { return new File[]{this, child}; }
        };
        assertEquals(List.of(child), ReikaFileReader.getAllFilesInFolder(cycle));
    }

    @Test void explicitCharsetAndReaderOwnershipSurviveSuccessAndWrappingFailure() throws Exception {
        Path file = directory.resolve("unicode.txt");
        Files.writeString(file, "snowman \u2603\nsecond", StandardCharsets.UTF_16LE);
        assertEquals(List.of("snowman \u2603", "second"),
                ReikaFileReader.getFileAsLines(file.toFile(), true, StandardCharsets.UTF_16LE));
        Files.write(file, new byte[]{(byte)0xC3, 0x28});
        assertEquals(List.of("\uFFFD("), ReikaFileReader.getFileAsLines(file.toFile(), true, StandardCharsets.UTF_8),
                "Malformed input must retain the original replacement behavior");
        boolean[] closed = {false};
        var input = new java.io.ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8)) {
            @Override public void close() { closed[0] = true; }
        };
        assertNull(ReikaFileReader.getReader(input, null));
        assertFalse(closed[0], "Failed wrapping must leave the caller's stream open");
        var reader = ReikaFileReader.getReader(input, StandardCharsets.UTF_8);
        assertNotNull(reader);
        assertEquals("text", reader.readLine());
        assertFalse(closed[0]);
        reader.close();
        assertTrue(closed[0]);
        assertNull(ReikaFileReader.getReader(directory.resolve("missing").toFile(), StandardCharsets.UTF_8));
    }
}
