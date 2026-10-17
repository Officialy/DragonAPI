package reika.dragonapi.auxiliary.trackers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Client-owned persistence, independent of world state and physical-client classes. */
public final class SettingWarningCache {
    private static final String VERSION_PREFIX = "# version ";

    public enum Policy { EVERYLOAD, SETTINGCHANGE, VERSION, ONCE }

    public record Snapshot(Set<String> enabled, Map<String, String> versions) {
        public Snapshot {
            enabled = Set.copyOf(enabled);
            versions = Map.copyOf(versions);
            enabled.forEach(SettingWarningCache::validateLine);
            versions.forEach((id, version) -> {
                validateLine(id);
                validateLine(version);
                if (id.contains("=")) throw new IllegalArgumentException("Version ID contains '=': " + id);
            });
        }
    }

    public static boolean shouldWarn(Policy policy, @Nullable Snapshot previous, Snapshot current) {
        if (previous == null) return true;
        return switch (policy) {
            case EVERYLOAD -> true;
            case ONCE -> false;
            case SETTINGCHANGE -> !previous.enabled().equals(current.enabled());
            case VERSION -> !previous.versions().equals(current.versions());
        };
    }

    public static @Nullable Snapshot read(Path path, Set<String> registered) throws IOException {
        java.util.List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException missing) {
            return null;
        }
        Set<String> enabled = new HashSet<>();
        Map<String, String> versions = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.startsWith(VERSION_PREFIX)) {
                int separator = line.indexOf('=', VERSION_PREFIX.length());
                if (separator > VERSION_PREFIX.length() && separator < line.length() - 1) {
                    String id = line.substring(VERSION_PREFIX.length(), separator);
                    String version = line.substring(separator + 1);
                    try {
                        validateLine(id);
                        validateLine(version);
                        versions.put(id, version);
                    } catch (IllegalArgumentException malformedComment) {
                        // Missing metadata triggers VERSION again; malformed comments cannot crash login.
                    }
                }
            } else if (registered.contains(line)) {
                enabled.add(line);
            }
        }
        return new Snapshot(enabled, versions);
    }

    /** Keep the original plain ID lines; comment metadata is ignored by legacy readers. */
    public static void write(Path path, Snapshot state) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("# DragonAPI setting warning cache");
        state.versions().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add(VERSION_PREFIX + entry.getKey() + "=" + entry.getValue()));
        state.enabled().stream().sorted().forEach(lines::add);
        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path pending = Files.createTempFile(target.getParent(), "setting-interference-", ".tmp");
        try {
            Files.write(pending, lines, StandardCharsets.UTF_8);
            try {
                Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private static void validateLine(String value) {
        if (value.isBlank() || value.startsWith("#") || !value.equals(value.strip()) || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Invalid setting warning cache value: " + value);
    }
}
