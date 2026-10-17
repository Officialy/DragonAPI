package reika.dragonapi.libraries.java;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaJVMParserTest {
    @Test void startupArgumentsAreAnImmutableSnapshotInRuntimeOrder() {
        var expected = new LinkedHashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        var actual = ReikaJVMParser.getAllArguments();
        assertIterableEquals(expected, actual);
        assertThrows(UnsupportedOperationException.class, () -> actual.add("dragonapi-test"));
        assertThrows(UnsupportedOperationException.class, actual::clear);
        for (String argument : expected) assertTrue(ReikaJVMParser.isArgumentPresent(argument));
        String missing = "dragonapi-missing-" + UUID.randomUUID();
        assertFalse(ReikaJVMParser.isArgumentPresent(missing));
        assertNull(ReikaJVMParser.getArgumentModifier(missing));
        assertEquals(-1, ReikaJVMParser.getArgumentInteger(missing));
        if (!expected.isEmpty()) {
            String first = expected.iterator().next();
            assertEquals("", ReikaJVMParser.getArgumentModifier(first));
        }
    }

    @Test void versionComponentsMatchTheInstalledRuntimeAndRejectUnavailableIndices() {
        var components = Runtime.version().version();
        for (int i = 0; i < components.size(); i++) assertEquals(components.get(i), ReikaJVMParser.getJavaVersion(i));
        assertThrows(IllegalArgumentException.class, () -> ReikaJVMParser.getJavaVersion(-1));
        assertThrows(IllegalArgumentException.class, () -> ReikaJVMParser.getJavaVersion(components.size()));
        assertTrue(ReikaJVMParser.getAllocatedHeapMemory() > 0);
        assertTrue(ReikaJVMParser.getFullJavaInfo().contains(System.getProperty("java.version")));
    }
}
