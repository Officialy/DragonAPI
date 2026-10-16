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
