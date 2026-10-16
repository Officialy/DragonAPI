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
