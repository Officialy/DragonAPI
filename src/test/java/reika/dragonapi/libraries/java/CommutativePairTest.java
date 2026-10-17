package reika.dragonapi.libraries.java;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.CommutativePair;

import static org.junit.jupiter.api.Assertions.*;

class CommutativePairTest {
    private record Value(String name, int hash) {
        @Override public int hashCode() { return hash; }
    }

    @Test void reversedAndEqualDistinctValuesWorkAsHashKeysIncludingOverflow() {
        for (int first : List.of(0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
            for (int second : List.of(0, 37, -19, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                var forward = new CommutativePair<>(new Value("a", first), new Value("b", second));
                var reverse = new CommutativePair<>(new Value("b", second), new Value("a", first));
                assertEquals(forward, reverse);
                assertEquals(reverse, forward);
                assertEquals(first + second, forward.hashCode());
                assertEquals(forward.hashCode(), reverse.hashCode());
                var map = new HashMap<CommutativePair<Value>, String>();
                map.put(forward, "before");
                assertEquals("before", map.get(reverse));
                assertEquals("before", map.put(reverse, "after"));
                assertEquals(1, map.size());
                assertEquals("after", map.get(forward));
                var set = new HashSet<CommutativePair<Value>>();
                assertTrue(set.add(forward));
                assertFalse(set.add(reverse));
                assertTrue(set.remove(reverse));
                assertTrue(set.isEmpty());
            }
        }
    }

    @Test void equalityRetainsMultiplicityAndDoesNotConfuseHashCollisionsOrOtherTypes() {
        var aa = new CommutativePair<>("a", "a");
        assertEquals(aa, new CommutativePair<>(new String("a"), new String("a")));
        assertNotEquals(aa, new CommutativePair<>("a", "b"));
        assertNotEquals(aa, null);
        assertNotEquals(aa, List.of("a", "a"));
        var first = new CommutativePair<>(new Value("a", 1), new Value("b", 2));
        var collision = new CommutativePair<>(new Value("c", 2), new Value("d", 1));
        assertEquals(first.hashCode(), collision.hashCode());
        assertNotEquals(first, collision);
        assertEquals(2, new HashSet<>(List.of(first, collision)).size());
    }

    @Test void originalNullRejectionIsPreserved() {
        assertThrows(MisuseException.class, () -> new CommutativePair<>(null, "value"));
        assertThrows(MisuseException.class, () -> new CommutativePair<>("value", null));
        assertThrows(MisuseException.class, () -> new CommutativePair<>(null, null));
    }
}
