package reika.dragonapi.auxiliary.trackers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SettingWarningCacheTest {
    @TempDir Path directory;

    @Test void everyPersistenceModeDistinguishesSettingsVersionsAndFirstRun() {
        var original = new SettingWarningCache.Snapshot(Set.of("soundmute"), Map.of("dragonapi", "1"));
        var settings = new SettingWarningCache.Snapshot(Set.of(), original.versions());
        var version = new SettingWarningCache.Snapshot(original.enabled(), Map.of("dragonapi", "2"));
        for (var policy : SettingWarningCache.Policy.values()) {
            assertTrue(SettingWarningCache.shouldWarn(policy, null, original));
            assertEquals(policy == SettingWarningCache.Policy.EVERYLOAD, SettingWarningCache.shouldWarn(policy, original, original));
            assertEquals(policy == SettingWarningCache.Policy.EVERYLOAD || policy == SettingWarningCache.Policy.SETTINGCHANGE,
                    SettingWarningCache.shouldWarn(policy, original, settings));
            assertEquals(policy == SettingWarningCache.Policy.EVERYLOAD || policy == SettingWarningCache.Policy.VERSION,
                    SettingWarningCache.shouldWarn(policy, original, version));
        }
    }

    @Test void legacyUtf8IdsAndUnknownHandlersRemainCompatibleWithVersionMetadata() throws Exception {
        Path file = directory.resolve("cache");
        assertNull(SettingWarningCache.read(file, Set.of("soundmute")));
        Files.write(file, List.of("soundmute", "retired", "soundmute", "# legacy comment", "", "# version bad= value", "# version =", "# version other=2"), StandardCharsets.UTF_8);
        var read = SettingWarningCache.read(file, Set.of("soundmute"));
        assertEquals(Set.of("soundmute"), read.enabled());
        assertEquals(Map.of("other", "2"), read.versions());
        assertTrue(SettingWarningCache.shouldWarn(SettingWarningCache.Policy.VERSION, read,
                new SettingWarningCache.Snapshot(read.enabled(), Map.of("other", "3"))));
    }

    @Test void cacheWritesAreDeterministicRoundTripUtf8AndReplaceCompleteSnapshots() throws Exception {
        Path file = directory.resolve("nested/cache");
        var state = new SettingWarningCache.Snapshot(Set.of("zulu", "soundmute", "éclairage"), Map.of("zmod", "2", "amod", "1"));
        SettingWarningCache.write(file, state);
        byte[] first = Files.readAllBytes(file);
        assertEquals(state, SettingWarningCache.read(file, state.enabled()));
        SettingWarningCache.write(file, state);
        assertArrayEquals(first, Files.readAllBytes(file));
        var clear = new SettingWarningCache.Snapshot(Set.of(), state.versions());
        SettingWarningCache.write(file, clear);
        assertEquals(clear, SettingWarningCache.read(file, state.enabled()));
        try (var files = Files.list(file.getParent())) { assertEquals(1, files.count()); }
    }

    @Test void failedReplacementPreservesExistingTargetAndCleansTemporaryFiles() throws Exception {
        Path target = directory.resolve("cache");
        Files.createDirectories(target);
        Path retained = target.resolve("retained");
        Files.writeString(retained, "keep me");
        assertThrows(java.io.IOException.class, () -> SettingWarningCache.write(target,
                new SettingWarningCache.Snapshot(Set.of("soundmute"), Map.of())));
        assertEquals("keep me", Files.readString(retained));
        try (var files = Files.list(directory)) { assertEquals(List.of(target), files.toList()); }
        assertThrows(java.io.IOException.class, () -> SettingWarningCache.read(target, Set.of("soundmute")));
    }

    @Test void invalidOrMutableSnapshotInputsCannotChangeOrInjectPersistedLines() {
        var enabled = new java.util.HashSet<>(Set.of("soundmute"));
        var versions = new java.util.HashMap<>(Map.of("dragonapi", "1"));
        var state = new SettingWarningCache.Snapshot(enabled, versions);
        enabled.clear(); versions.clear();
        assertEquals(Set.of("soundmute"), state.enabled());
        assertEquals(Map.of("dragonapi", "1"), state.versions());
        assertThrows(UnsupportedOperationException.class, () -> state.enabled().clear());
        assertThrows(UnsupportedOperationException.class, () -> state.versions().clear());
        for (String invalid : List.of("", " ", "#comment", "bad\nid", "bad\rid", " leading"))
            assertThrows(IllegalArgumentException.class, () -> new SettingWarningCache.Snapshot(Set.of(invalid), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SettingWarningCache.Snapshot(Set.of(), Map.of("id", "1\n2")));
    }
}
