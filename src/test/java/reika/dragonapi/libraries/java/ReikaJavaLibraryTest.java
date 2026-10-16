package reika.dragonapi.libraries.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaJavaLibraryTest {
    @Test void streamReadsPreserveAllByteValuesAndShortReadsWithoutTakingOwnership() {
        byte[] bytes = new byte[10_000];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte)i;
        var source = new java.io.ByteArrayInputStream(bytes);
        boolean[] closed = {false};
        int[] reads = {0};
        var stream = new java.io.InputStream() {
            @Override public int read() { throw new AssertionError("Use bulk reads"); }
            @Override public int read(byte[] buffer, int offset, int length) {
                reads[0]++;
                return source.read(buffer, offset, Math.min(length, 17));
            }
            @Override public void close() { closed[0] = true; }
        };
        assertArrayEquals(bytes, ReikaJavaLibrary.streamToBytes(stream));
        assertEquals(0, source.available());
        assertTrue(reads[0] > 0);
        assertFalse(closed[0]);
    }

    @Test void failedStreamReadRetainsNullResultAndCallerOwnership() {
        boolean[] closed = {false};
        var stream = new java.io.InputStream() {
            @Override public int read() throws java.io.IOException { throw new java.io.IOException("expected stream failure"); }
            @Override public void close() { closed[0] = true; }
        };
        assertNull(ReikaJavaLibrary.streamToBytes(stream));
        assertFalse(closed[0]);
    }

    public record Constructed(String value) {}
    public static class FailingConstructor {
        public FailingConstructor(String value) throws java.io.IOException { throw new java.io.IOException("expected constructor failure"); }
    }

    @Test void collectionConstructionPreservesOrderAndReportsActualConstructorFailure() throws Exception {
        assertEquals(List.of(new Constructed("first"), new Constructed("second")),
                ReikaJavaLibrary.getConstructedCollection(List.of("first", "second"), String.class, Constructed.class));
        assertNull(ReikaJavaLibrary.getConstructedCollection(List.of(new Object()), Object.class, Constructed.class));
        var error = assertThrows(IllegalStateException.class, () -> ReikaJavaLibrary.getConstructedCollection(
                List.of("broken"), FailingConstructor.class.getConstructor(String.class)));
        assertInstanceOf(java.io.IOException.class, error.getCause());
        assertTrue(error.getMessage().contains("entry 0"));
        assertNull(ReikaJavaLibrary.getConstructedCollection(List.of("broken"), String.class, FailingConstructor.class));
    }

    @Test void arrayFactoryPreservesRuntimeTypeEmptyArraysAndNullElements() {
        String[] values = ReikaJavaLibrary.collectionToArray(Arrays.asList("first", null, "last"), String[]::new);
        assertArrayEquals(new String[]{"first", null, "last"}, values);
        assertEquals(String[].class, ReikaJavaLibrary.collectionToArray(List.of(), String[]::new).getClass());
        Number[] numbers = ReikaJavaLibrary.collectionToArray(List.of(1, 2L), Number[]::new);
        assertArrayEquals(new Number[]{1, 2L}, numbers);
        assertEquals(Object[].class, ReikaJavaLibrary.collectionToArray(List.of("one")).getClass());
    }

    @Test void emptyRandomRemovalNeverDrawsOrMutatesImmutableCollections() {
        Random noDraw = new Random() {
            @Override public int nextInt(int bound) { throw new AssertionError("Empty collection consumed a random draw"); }
        };
        assertNull(ReikaJavaLibrary.getAndRemoveRandomCollectionEntry(noDraw, List.of()));
        assertNull(ReikaJavaLibrary.getAndRemoveRandomCollectionEntry(noDraw, Set.of()));
        assertNull(ReikaJavaLibrary.getRandomCollectionEntry(noDraw, Set.of()));
    }

    @Test void nonemptyRemovalPreservesSeededSelectionAndDrawCountForListsAndSets() {
        for (boolean list : List.of(true, false)) {
            var values = list ? new ArrayList<>(List.of("a", "b", "c")) : new LinkedHashSet<>(List.of("a", "b", "c"));
            Random actual = new Random(123), expected = new Random(123);
            String chosen = List.of("a", "b", "c").get(expected.nextInt(3));
            assertEquals(chosen, ReikaJavaLibrary.getAndRemoveRandomCollectionEntry(actual, values));
            assertFalse(values.contains(chosen));
            assertEquals(2, values.size());
            assertEquals(expected.nextLong(), actual.nextLong());
        }
    }

    @Test void reverseLookupSupportsNullValuesAndRetainsFirstMatch() {
        var values = new LinkedHashMap<String, Integer>();
        values.put("first", null); values.put("second", null); values.put("number", 3);
        assertEquals("first", ReikaJavaLibrary.getHashMapKeyByValue(values, null));
        assertEquals("number", ReikaJavaLibrary.getHashMapKeyByValue(values, 3));
        assertNull(ReikaJavaLibrary.getHashMapKeyByValue(values, 4));
    }

    public static class LoaderFixture {
        static { System.setProperty("dragonapi.test.definingLoader", LoaderFixture.class.getClassLoader().toString()); }
    }

    @Test void classInitializationUsesTheSuppliedClassDefiningLoader() {
        class IsolatedLoader extends ClassLoader {
            Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
        }
        var loader = new IsolatedLoader();
        Class<?> type = loader.define(ReikaJavaLibrary.getClassBytes(LoaderFixture.class));
        String previous = System.getProperty("dragonapi.test.definingLoader");
        try {
            ReikaJavaLibrary.initClass(type);
            assertEquals(loader.toString(), System.getProperty("dragonapi.test.definingLoader"));
        } finally {
            if (previous == null) System.clearProperty("dragonapi.test.definingLoader");
            else System.setProperty("dragonapi.test.definingLoader", previous);
        }
    }
}
