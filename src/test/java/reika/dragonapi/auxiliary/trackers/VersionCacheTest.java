package reika.dragonapi.auxiliary.trackers;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionCacheTest {
    @Test
    void malformedLinesDoNotHideLaterValidEntries() {
        var parsed = VersionTransitionTracker.parseCache(List.of("", "broken", "=missing-id", "missing-version=", "  #comment=ignored", " dragonapi = 1.0.3-beta.2 ", "rotarycraft=v33a", "other=release=build"));
        assertEquals(Map.of("dragonapi", "1.0.3-beta.2", "rotarycraft", "v33a", "other", "release=build"), parsed);
    }
}
