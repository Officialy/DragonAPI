package reika.dragonapi.instantiable.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.interfaces.configuration.IntegerConfig;
import reika.dragonapi.interfaces.configuration.ConfigList;
import reika.dragonapi.interfaces.configuration.CustomCategoryConfig;
import reika.dragonapi.interfaces.configuration.SegmentedConfigList;
import static org.junit.jupiter.api.Assertions.*;

class ControlledConfigTest {
    private static final String MARKER = "Config ID - Edit to have your config auto-deleted";

    private enum Option implements IntegerConfig, CustomCategoryConfig {
        VALUE, CONTROL_VALUE;
        public Class<?> getPropertyType() { return int.class; }
        public String getLabel() { return this == VALUE ? "Value" : "Control Value"; }
        public String getCategory() { return this == VALUE ? "Control Setup" : "Control"; }
        public boolean isEnforcingDefaults() { return false; }
        public boolean shouldLoad() { return true; }
        public boolean isNumeric() { return true; }
        public int getValue() { return 10; }
        public int getDefaultValue() { return 10; }
    }

    private static class TestMod extends DragonAPIMod {
        public String getDisplayName() { return "ConfigResetTest" + System.identityHashCode(this); }
        public String getModId() { return "dragonapi"; }
        public String getModAuthorName() { return "Reika"; }
        public String getUpdateCheckURL() { return null; }
        public URL getDocumentationSite() { return null; }
        public URL getBugSite() { return null; }
        public File getConfigFolder() { return null; }
    }

    private static class Fixture extends ControlledConfig {
        final DataElement<String> extra = registerAdditionalOption("Other", "Extra", "default");
        Fixture(int version) {
            this(version, Option.values());
        }
        Fixture(int version, ConfigList[] options) {
            super(new TestMod(), options, null, version);
        }
        void reset() { resetConfigFile(); }
    }

    private record SegmentedOption(Path file) implements IntegerConfig, SegmentedConfigList {
        public Class<?> getPropertyType() { return int.class; }
        public String getLabel() { return "Segment"; }
        public boolean isEnforcingDefaults() { return false; }
        public boolean shouldLoad() { return true; }
        public boolean isNumeric() { return true; }
        public int getValue() { return 20; }
        public int getDefaultValue() { return 20; }
        public int ordinal() { return 1; }
        public boolean saveIfUnspecified() { return true; }
        public boolean isAccessible() { return true; }
        public String getCustomConfigFile() { return file.toString(); }
    }

    private static void field(ControlledConfig target, String name, Object value) throws Exception {
        var field = ControlledConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static ControlledConfig fixture(Path file, Integer version) throws Exception {
        ControlledConfig config = version == null ? new ControlledConfig(new TestMod(), Option.values(), null) : new Fixture(version);
        // The test owns registration and the loaded spec below; never register fixture configs on the real mod.
        field(config, "modernRegistered", true);
        config.loadCustomConfigFile(file.toString());
        return config;
    }

    private static byte[] legacy(Path file, String marker, int value) throws Exception {
        String text = "# exact original comment\r\n" + (marker == null ? "" : "control {\r\n I:\"" + MARKER + "\"=" + marker + "\r\n}\r\n")
                + "\"control setup\" {\r\n I:Value=" + value + "\r\n}\r\nother {\r\n S:Extra=custom\r\n}\r\n";
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, bytes);
        return bytes;
    }

    private static List<Path> backups(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(p -> p.getFileName().toString().contains(".reset-backup-")).toList();
        }
    }

    @Test void absentAndMatchingIdsKeepCustomValuesAndMarkerSurvivesReload(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg");
        byte[] original = legacy(file, null, 37);
        var config = fixture(file, 4);
        config.initProps();
        assertEquals(37, config.getControl(0));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("settings.cfg.pre-toml")));
        config.reload();
        assertEquals(37, config.getControl(0));
        assertTrue(Files.readString(file).contains(MARKER));
        assertTrue(backups(directory).isEmpty());
    }

    @Test void mismatchedIdsResetDefaultsAndPreserveEveryOriginalByte(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg");
        byte[] original = legacy(file, "3", 37);
        var config = (Fixture)fixture(file, 4);
        config.initProps();
        assertEquals(10, config.getControl(0));
        assertEquals("default", config.extra.getData());
        assertArrayEquals(original, Files.readAllBytes(backups(directory).getFirst()));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("settings.cfg.pre-toml")));
        config.reload();
        assertEquals(1, backups(directory).size(), "Reset marker must not cause a reload loop");
        assertEquals(10, config.getControl(0));
    }

    @Test void repeatedExplicitResetsKeepSeparateBackupsAndRestoreAdditionalDefaults(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg");
        legacy(file, "4", 37);
        var config = (Fixture)fixture(file, 4);
        config.initProps();
        assertEquals("custom", config.extra.getData());
        byte[] first = Files.readAllBytes(file);
        config.reset();
        assertEquals(10, config.getControl(0));
        assertEquals("default", config.extra.getData(), "Reset must not reuse the previously loaded extra value as a default");
        legacy(file, "4", 51);
        config.reload();
        byte[] second = Files.readAllBytes(file);
        config.reset();
        var saved = backups(directory);
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(p -> sameBytes(p, first)));
        assertTrue(saved.stream().anyMatch(p -> sameBytes(p, second)));
        config.reload();
        assertEquals(10, config.getControl(0));
    }

    private static boolean sameBytes(Path file, byte[] bytes) {
        try { return java.util.Arrays.equals(bytes, Files.readAllBytes(file)); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }

    @Test void unversionedCallersDoNotInventAResetPolicy(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg");
        legacy(file, "9876", 37);
        var config = fixture(file, null);
        config.initProps(); config.reload();
        assertEquals(37, config.getControl(0));
        assertTrue(Files.readString(file).contains("9876"));
        assertTrue(backups(directory).isEmpty());
    }

    @Test void malformedMarkerAndBackupFailureDoNotEraseConfigOrLoadedValues(@TempDir Path directory) throws Exception {
        Path malformed = directory.resolve("malformed.cfg");
        byte[] original = legacy(malformed, "not-an-id", 37);
        var invalid = fixture(malformed, 4);
        assertThrows(RegistrationException.class, invalid::initProps);
        assertArrayEquals(original, Files.readAllBytes(malformed));
        assertTrue(backups(directory).isEmpty());

        Path file = directory.resolve("settings.cfg");
        legacy(file, "4", 37);
        var config = (Fixture)fixture(file, 4);
        config.initProps();
        // A non-file source is a deterministic preservation failure, independent of OS permissions.
        config.configFile = directory.toFile();
        assertThrows(RegistrationException.class, config::reset);
        assertEquals(37, config.getControl(0));
        assertEquals("custom", config.extra.getData());
        assertTrue(backups(directory).isEmpty());
        assertTrue(Files.isRegularFile(file));
    }

    @Test void loadedTomlRemainsAuthoritativeAcrossLegacyReset(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg"), toml = directory.resolve("dragonapi-synced-options.toml");
        byte[] original = legacy(file, "3", 37);
        byte[] tomlBytes = "[\"Control Setup\"]\nValue = 83\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(toml, tomlBytes);
        Path segmented = directory.resolve("segmented.txt");
        Files.writeString(segmented, "[\"Segment\"=\"77\"\n");
        var builder = new ModConfigSpec.Builder();
        var value = builder.define(List.of("Control Setup", "Value"), 10);
        var spec = builder.build();
        CommentedConfig data = new TomlParser().parse(Files.readString(toml));
        spec.correct(data);
        // FML's loaded-config implementation is package-private; instantiate its real record for this isolated spec.
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                .getDeclaredConstructor(CommentedConfig.class, Path.class, net.neoforged.fml.config.ModConfig.class);
        constructor.setAccessible(true);
        spec.acceptConfig((IConfigSpec.ILoadedConfig)constructor.newInstance(data, toml, null));
        var config = new Fixture(4, new ConfigList[] {Option.VALUE, new SegmentedOption(segmented)});
        field(config, "modernRegistered", true);
        config.loadCustomConfigFile(file.toString());
        field(config, "modernValues", new java.util.HashMap<>(Map.of(0, value)));
        field(config, "modernSpecs", new java.util.HashMap<>(Map.of(0, spec)));
        config.initProps();
        assertEquals(83, config.getControl(0));
        assertEquals(77, config.getControl(1));
        assertEquals(10, config.controls[0]);
        config.reset(); config.reload();
        legacy(file, "2", 42);
        config.reload(); // A later format mismatch must not re-register or clear the already loaded spec.
        assertEquals(83, config.getControl(0));
        assertEquals(77, config.getControl(1));
        assertEquals(83, value.get());
        assertArrayEquals(tomlBytes, Files.readAllBytes(toml));
        assertTrue(Files.readString(segmented).contains("\"77\""));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("settings.cfg.pre-toml")));
    }

    @Test void unusableMigrationBackupAbortsBeforeParserRewritesTheOriginal(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("settings.cfg");
        byte[] original = legacy(file, "3", 37);
        Files.createDirectory(directory.resolve("settings.cfg.pre-toml"));
        var config = fixture(file, 4);
        assertThrows(RegistrationException.class, config::initProps);
        assertArrayEquals(original, Files.readAllBytes(file));
        assertTrue(backups(directory).isEmpty());
    }
}
