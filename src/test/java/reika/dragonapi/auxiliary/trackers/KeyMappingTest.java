package reika.dragonapi.auxiliary.trackers;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import reika.dragonapi.interfaces.configuration.StringConfig;
import static org.junit.jupiter.api.Assertions.*;

class KeyMappingTest {
    @Test
    void altAndFollowingKeysKeepDistinctWireIdentities() {
        var ids = new HashSet<Integer>();
        for (var key : KeyWatcher.Key.values()) {
            assertTrue(ids.add(key.wireId));
            assertSame(key, KeyWatcher.Key.fromWireId(key.wireId));
        }
        assertSame(KeyWatcher.Key.LALT, KeyWatcher.Key.fromWireId(13));
        assertSame(KeyWatcher.Key.PGUP, KeyWatcher.Key.fromWireId(14));
        assertSame(KeyWatcher.Key.PAUSE, KeyWatcher.Key.fromWireId(27));
        assertThrows(IllegalArgumentException.class, () -> KeyWatcher.Key.fromWireId(-1));
    }

    @Test
    void configEnumsAreAcceptedWithoutOldForgeProperty() {
        assertSame(KeyWatcher.Key.PGUP, KeyWatcher.Key.readFromConfig(null, Config.KEY));
    }

    private enum Config implements StringConfig {
        KEY;
        public boolean isString() { return true; }
        public String getString() { return " pgup "; }
        public String getDefaultString() { return "PGUP"; }
        public Class<?> getPropertyType() { return String.class; }
        public String getLabel() { return "test key"; }
        public boolean isEnforcingDefaults() { return false; }
        public boolean shouldLoad() { return true; }
    }
}
