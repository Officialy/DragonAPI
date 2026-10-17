package reika.dragonapi.libraries.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaJavaLibraryTest {
    private static class LookupState { static boolean initialized; }
    public static class LookupFixture {
        static { LookupState.initialized = true; }
    }
    public static class BrokenLookupFixture {
        static { if (failInitialization()) throw new IllegalStateException("expected initializer failure"); }
        private static boolean failInitialization() { return true; }
    }

    @Test void classProbesUseContextOrDefiningLoaderWithoutInitializingAndLookupRetainsInitialization() {
        String name = LookupFixture.class.getName();
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        int[] lookups = {0};
        var context = new ClassLoader(ReikaJavaLibrary.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String type, boolean resolve) throws ClassNotFoundException {
                if (type.equals(name)) lookups[0]++;
                return super.loadClass(type, resolve);
            }
        };
        try {
            assertFalse(LookupState.initialized);
            thread.setContextClassLoader(context);
            assertTrue(ReikaJavaLibrary.doesClassExist(name));
            assertEquals(1, lookups[0]);
            assertFalse(LookupState.initialized);
            thread.setContextClassLoader(null);
            assertTrue(ReikaJavaLibrary.doesClassExist(name));
            assertFalse(LookupState.initialized);
            assertSame(LookupFixture.class, ReikaJavaLibrary.getClassNoException(name));
            assertTrue(LookupState.initialized);
            assertFalse(ReikaJavaLibrary.doesClassExist("dragonapi.test.MissingClass"));
            assertNull(ReikaJavaLibrary.getClassNoException("dragonapi.test.MissingClass"));
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Test void availabilityTreatsLinkageFailuresAsUnavailableAndDoesNotSwallowVmErrors() {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try {
            for (Error failure : List.of(new NoClassDefFoundError("expected absent dependency"),
                    new UnsupportedClassVersionError("expected incompatible class"),
                    new ClassFormatError("expected malformed class"), new StackOverflowError("simulated VM error"))) {
                thread.setContextClassLoader(new ClassLoader() {
                    @Override protected Class<?> loadClass(String name, boolean resolve) { throw failure; }
                });
                if (failure instanceof LinkageError) assertFalse(ReikaJavaLibrary.doesClassExist("dragonapi.test.Unavailable"));
                else assertSame(failure, assertThrows(StackOverflowError.class,
                        () -> ReikaJavaLibrary.doesClassExist("dragonapi.test.Unavailable")));
            }
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Test void nullableClassLookupRetainsFailureFallbackForFirstAndSubsequentInitializerFailure() {
        String name = BrokenLookupFixture.class.getName();
        assertNull(ReikaJavaLibrary.getClassNoException(name));
        assertNull(ReikaJavaLibrary.getClassNoException(name));
    }

    @Test void nestedMapHelpersAcceptConcreteTypesAndNeverReorderAccessOrderedMaps() {
        var outer = new LinkedHashMap<String, LinkedHashMap<String, Integer>>(16, .75F, true);
        var first = new LinkedHashMap<String, Integer>(16, .75F, true);
        first.put("first", 1); first.put("null", null);
        var second = new LinkedHashMap<String, Integer>(16, .75F, true);
        second.put("second", 2);
        outer.put("first", first); outer.put("second", second); outer.put("empty", new LinkedHashMap<>());
        Collection<Number> values = ReikaJavaLibrary.getValuesForMapOfMaps(outer);
        assertEquals(Arrays.asList(1, null, 2), values);
        assertEquals(3, ReikaJavaLibrary.getNestedMapSize(outer));
        assertEquals(List.of("first", "second", "empty"), new ArrayList<>(outer.keySet()));
        assertEquals(List.of("first", "null"), new ArrayList<>(first.keySet()));
        values.clear();
        assertEquals(2, first.size());
        var mixed = new LinkedHashMap<String, Object>(16, .75F, true);
        mixed.put("nested", outer); mixed.put("scalar", "value"); mixed.put("null", null);
        assertEquals(5, ReikaJavaLibrary.getNestedMapSize(mixed)); // Shallow size: 3 outer entries + 2 scalar entries.
        assertEquals(List.of("nested", "scalar", "null"), new ArrayList<>(mixed.keySet()));
    }

    @Test void flattenedCollectionsPreserveCovariantElementsDuplicatesNullsOrderAndIndependentResults() {
        var first = new ArrayList<Integer>(Arrays.asList(1, null, 1));
        List<ArrayList<Integer>> nested = List.of(first, new ArrayList<>(List.of(2)));
        Collection<Number> compound = ReikaJavaLibrary.getCompoundCollection(nested);
        assertEquals(Arrays.asList(1, null, 1, 2), compound);
        compound.clear();
        assertEquals(Arrays.asList(1, null, 1), first);
        Collection<Number> combined = ReikaJavaLibrary.combineCollections(first, List.of(2.5));
        assertEquals(Arrays.asList(1, null, 1, 2.5), combined);
        var map = new LinkedHashMap<String, ArrayList<Integer>>(16, .75F, true);
        map.put("first", first); map.put("empty", new ArrayList<>()); map.put("last", new ArrayList<>(List.of(3)));
        Collection<Number> flattened = ReikaJavaLibrary.getValuesForMapOfCollections(map);
        assertEquals(Arrays.asList(1, null, 1, 3), flattened);
        assertEquals(List.of("first", "empty", "last"), new ArrayList<>(map.keySet()));
        assertTrue(ReikaJavaLibrary.getValuesForMapOfMaps(java.util.Map.of()).isEmpty());
        assertTrue(ReikaJavaLibrary.getValuesForMapOfCollections(java.util.Map.of()).isEmpty());
        assertTrue(ReikaJavaLibrary.getCompoundCollection(List.of()).isEmpty());
        assertTrue(ReikaJavaLibrary.combineCollections().isEmpty());
        assertThrows(NullPointerException.class, () -> ReikaJavaLibrary.getCompoundCollection(Arrays.asList(first, null)));
    }

    private record HashValue(String name, int hash) { @Override public int hashCode() { return hash; } }
    private static class ComparableParent implements Comparable<ComparableParent> {
        final int rank;
        ComparableParent(int rank) { this.rank = rank; }
        @Override public int compareTo(ComparableParent other) { return Integer.compare(rank, other.rank); }
    }
    private static class ComparableChild extends ComparableParent { ComparableChild(int rank) { super(rank); } }

    @Test void classSelectedSortingPreservesNaturalAndHashOrderingTiesAndInput() {
        var low = new ComparableChild(1); var high = new ComparableChild(2);
        List<ComparableChild> input = List.of(high, low);
        ArrayList<ComparableChild> sorted = ReikaJavaLibrary.makeSortedListFromCollection(input, ComparableParent.class);
        assertEquals(List.of(low, high), sorted);
        assertEquals(List.of(high, low), input);
        var minimum = new HashValue("minimum", Integer.MIN_VALUE);
        var maximum = new HashValue("maximum", Integer.MAX_VALUE);
        var firstTie = new HashValue("first tie", 0); var secondTie = new HashValue("second tie", 0);
        var hashes = List.of(maximum, firstTie, minimum, secondTie);
        assertEquals(List.of(minimum, firstTie, secondTie, maximum),
                ReikaJavaLibrary.makeSortedListFromCollection(hashes, HashValue.class));
        assertEquals(List.of(maximum, firstTie, minimum, secondTie), hashes);
        var empty = ReikaJavaLibrary.<String>makeSortedListFromCollection(List.of(), null);
        empty.add("mutable copy");
        assertEquals(List.of("mutable copy"), empty);
    }

    @Test void safeDecimalParsingPreservesSyntaxAndHandlesBoundariesAndOverflow() {
        assertEquals(Integer.MIN_VALUE, ReikaJavaLibrary.safeIntParse("-2147483648"));
        assertEquals(Integer.MAX_VALUE, ReikaJavaLibrary.safeIntParse("2147483647"));
        assertEquals(Long.MIN_VALUE, ReikaJavaLibrary.safeLongParse("-9223372036854775808"));
        assertEquals(Long.MAX_VALUE, ReikaJavaLibrary.safeLongParse("9223372036854775807"));
        for (String invalid : Arrays.asList(null, "", "-", "+1", " 1", "1 ", "--1", "1.0", "1e3", "0x10", "\u0661", "\uff11")) {
            assertEquals(0, ReikaJavaLibrary.safeIntParse(invalid), () -> "int: " + invalid);
            assertEquals(0L, ReikaJavaLibrary.safeLongParse(invalid), () -> "long: " + invalid);
        }
        for (String overflow : List.of("2147483648", "-2147483649", "99999999999999999999999999999999")) {
            assertEquals(0, ReikaJavaLibrary.safeIntParse(overflow));
        }
        for (String overflow : List.of("9223372036854775808", "-9223372036854775809", "99999999999999999999999999999999")) {
            assertEquals(0L, ReikaJavaLibrary.safeLongParse(overflow));
        }
        assertEquals(-12, ReikaJavaLibrary.safeIntParse("-00012"));
        assertEquals(12L, ReikaJavaLibrary.safeLongParse("00012"));
        assertEquals(0, ReikaJavaLibrary.safeIntParse("-0"));
        Random random = new Random(9182);
        for (int i = 0; i < 1_000; i++) {
            int integer = random.nextInt(); long large = random.nextLong();
            assertEquals(integer, ReikaJavaLibrary.safeIntParse(Integer.toString(integer)));
            assertEquals(large, ReikaJavaLibrary.safeLongParse(Long.toString(large)));
        }
    }

    public static class MetadataDependency {}
    public static class MetadataFixture {
        public MetadataDependency field;
        public MetadataFixture() {}
        public MetadataDependency method(MetadataDependency value) { return value; }
    }

    @Test void classResourceReadsCloseOwnedStreamsOnSuccessAndFailureAndRetainEmptyFallback() {
        byte[] bytes = ReikaJavaLibrary.getClassBytes(MetadataFixture.class);
        assertTrue(bytes.length > 0);
        for (boolean fail : List.of(false, true)) {
            int[] closes = {0};
            var stream = new java.io.ByteArrayInputStream(bytes) {
                @Override public byte[] readAllBytes() {
                    if (fail) throw new IllegalStateException("expected resource failure");
                    return super.readAllBytes();
                }
                @Override public void close() { closes[0]++; }
            };
            class ResourceLoader extends ClassLoader {
                Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
                @Override public java.io.InputStream getResourceAsStream(String name) { return stream; }
            }
            assertArrayEquals(fail ? new byte[0] : bytes, ReikaJavaLibrary.getClassBytes(new ResourceLoader().define()));
            assertEquals(1, closes[0]);
        }
        class MissingResourceLoader extends ClassLoader {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
            @Override public java.io.InputStream getResourceAsStream(String name) { return null; }
        }
        assertArrayEquals(new byte[0], ReikaJavaLibrary.getClassBytes(new MissingResourceLoader().define()));
        assertArrayEquals(new byte[0], ReikaJavaLibrary.getClassBytes(null));
    }

    @Test void metadataRetainsHealthySectionsAndDependencyFailureDetailsAndPropagatesWriteErrors() throws Exception {
        byte[] bytes = ReikaJavaLibrary.getClassBytes(MetadataFixture.class);
        class MissingDependencyLoader extends ClassLoader {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(MetadataDependency.class.getName())) throw new ClassNotFoundException("expected missing dependency: " + name);
                return super.loadClass(name, resolve);
            }
        }
        var exporter = ReikaJavaLibrary.class.getDeclaredMethod("printClassMetadata", java.io.BufferedWriter.class, Class.class);
        assertTrue(exporter.trySetAccessible());
        for (Class<?> type : List.of(MetadataFixture.class, new MissingDependencyLoader().define())) {
            var text = new java.io.StringWriter();
            try (var writer = new java.io.BufferedWriter(text)) { exporter.invoke(null, writer, type); }
            String report = text.toString();
            for (String section : List.of("General:", "Internal Classes:", "Constructors:", "Fields:", "Methods:")) {
                assertTrue(report.contains(section), section);
            }
            if (type != MetadataFixture.class) {
                assertTrue(report.contains("ERRORED FIELDS"));
                assertTrue(report.contains("ERRORED METHODS"));
                assertTrue(report.contains("NoClassDefFoundError"));
                assertTrue(report.contains("expected missing dependency"));
            } else {
                assertTrue(report.contains("\tfield\n"));
                assertTrue(report.contains("\tmethod\n"));
                assertFalse(report.contains("ERRORED"));
            }
        }
        var failure = new java.io.IOException("expected disk failure");
        var failingWriter = new java.io.BufferedWriter(new java.io.StringWriter()) {
            @Override public void write(String value) throws java.io.IOException { throw failure; }
        };
        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> exporter.invoke(null, failingWriter, MetadataFixture.class));
        assertSame(failure, thrown.getCause());
    }

    @Test void classDumpWritesAllBytesAndRejectsNullBeforeTruncation(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var base = directory.resolve("class-dump");
        var output = directory.resolve("class-dump.class");
        byte[] bytes = ReikaJavaLibrary.getClassBytes(MetadataFixture.class);
        ReikaJavaLibrary.printClassSource(base.toString(), bytes);
        assertArrayEquals(bytes, java.nio.file.Files.readAllBytes(output));
        assertThrows(NullPointerException.class, () -> ReikaJavaLibrary.printClassSource(base.toString(), null));
        assertArrayEquals(bytes, java.nio.file.Files.readAllBytes(output));
    }

    private static class WrapperNames {
        static class Integer {}
        static class Boolean {}
        static class Character {}
        static class Void {}
    }

    @Test void primitiveInferenceUsesWrapperIdentityAndRejectsUntypedNulls() {
        assertArrayEquals(new Class<?>[]{byte.class, short.class, int.class, long.class, char.class,
                        float.class, double.class, boolean.class, String.class, int[].class, Class.class},
                ReikaJavaLibrary.getObjectClasses((byte)1, (short)2, 3, 4L, 'a', 5F, 6D, true,
                        "text", new int[0], void.class));
        for (Object value : List.of(new WrapperNames.Integer(), new WrapperNames.Boolean(),
                new WrapperNames.Character(), new WrapperNames.Void())) {
            assertSame(value.getClass(), ReikaJavaLibrary.getClassOf(value));
        }
        assertArrayEquals(new Class<?>[0], ReikaJavaLibrary.getObjectClasses());
        assertThrows(NullPointerException.class, () -> ReikaJavaLibrary.getClassOf(null));
        assertThrows(NullPointerException.class, () -> ReikaJavaLibrary.getObjectClasses("text", null));
    }

    @Test void packageHelpersHandleArraysNestedClassesPrimitivesAndUnnamedPackages() throws Exception {
        for (Class<?> type : List.of(String.class, String[][].class, int.class, int[].class, void.class)) {
            assertEquals("java.lang", ReikaJavaLibrary.getPackageName(type));
            assertEquals("java", ReikaJavaLibrary.getTopLevelPackage(type));
        }
        assertEquals("reika.dragonapi.libraries.java", ReikaJavaLibrary.getPackageName(WrapperNames.Integer.class));
        assertEquals("reika", ReikaJavaLibrary.getTopLevelPackage(WrapperNames.Integer.class));
        Class<?> unpackaged = Class.forName("UnpackagedTypeFixture");
        assertEquals("", ReikaJavaLibrary.getPackageName(unpackaged));
        assertEquals("", ReikaJavaLibrary.getTopLevelPackage(unpackaged));
        assertEquals("", ReikaJavaLibrary.getPackageName(unpackaged.arrayType()));
        assertEquals("", ReikaJavaLibrary.getTopLevelPackage(unpackaged.arrayType()));
    }

    @Test void valueSortingRetainsTypesStableTiesAndInputAndSupportsCustomNullOrdering() {
        var input = new LinkedHashMap<String, Integer>();
        input.put("large", 3); input.put("first tie", 1); input.put("second tie", 1); input.put("middle", 2);
        var sorted = ReikaJavaLibrary.sortMapByValues(input);
        assertEquals(List.of("first tie", "second tie", "middle", "large"), new ArrayList<>(sorted.keySet()));
        assertEquals(List.of("large", "first tie", "second tie", "middle"), new ArrayList<>(input.keySet()));
        assertNotSame(input, sorted);
        assertEquals(input, sorted);
        assertTrue(ReikaJavaLibrary.sortMapByValues(new LinkedHashMap<String, Integer>()).isEmpty());
        assertEquals(List.of("large", "middle", "first tie", "second tie"), new ArrayList<>(
                ReikaJavaLibrary.sortMapByValues(input, new ReikaJavaLibrary.ReverseComparator<>()).keySet()));
        var nullable = new LinkedHashMap<String, Constructed>();
        nullable.put("present", new Constructed("value")); nullable.put("absent", null);
        assertEquals(List.of("absent", "present"), new ArrayList<>(ReikaJavaLibrary.sortMapByValues(nullable,
                java.util.Comparator.nullsFirst(java.util.Comparator.comparing(Constructed::value))).keySet()));
        assertThrows(NullPointerException.class, () -> ReikaJavaLibrary.sortMapByValues(input, null));
    }

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
