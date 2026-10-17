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
