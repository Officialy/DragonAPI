package reika.dragonapi.extras;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModVersionTest {
    @Test
    void artifactVersionsKeepPatchPrereleaseAndMetadata() {
        var release = ModVersion.getFromString("1.0.3");
        var beta = ModVersion.fromSemanticVersion("1.0.3-beta.2+build.7");
        assertEquals("1.0.3", release.toString());
        assertEquals("1.0.3-beta.2+build.7", beta.toSemanticVersion());
        assertTrue(release.compareTo(ModVersion.getFromString("1.0.2")) > 0);
        assertTrue(ModVersion.getFromString("1.0.3-beta.2").compareTo(release) < 0);
        assertNotEquals(release, ModVersion.getFromString("1.0.2"));
    }

    @Test
    void legacyVersionsAndSentinelsKeepTheirContract() {
        assertEquals("v33a", ModVersion.getFromString("V33A").toString());
        assertTrue(ModVersion.getFromString("v2147483647a").compareTo(ModVersion.getFromString("v1z")) > 0);
        assertEquals("33.0", ModVersion.getFromString("33").toSemanticVersion());
        assertSame(ModVersion.source, ModVersion.getFromString("Source Code"));
        assertFalse(ModVersion.getFromString("invalid").verify());
        assertFalse(ModVersion.getFromString("").verify());
    }

    @Test
    void suppliedJarVersionIsActuallyRead(@TempDir Path directory) throws Exception {
        Path artifact = directory.resolve("mod.jar");
        try (var output = new ZipOutputStream(java.nio.file.Files.newOutputStream(artifact))) {
            output.putNextEntry(new ZipEntry("version_DragonAPI.properties"));
            output.write("Major=33\nMinor=b\n".getBytes(StandardCharsets.ISO_8859_1));
            output.closeEntry();
        }
        try (var jar = new ZipFile(artifact.toFile())) {
            assertEquals("v33b", ModVersion.readFromJar(jar, "Dragon API").toString());
            assertFalse(ModVersion.readFromJar(jar, "Missing").verify());
        }
    }
}
