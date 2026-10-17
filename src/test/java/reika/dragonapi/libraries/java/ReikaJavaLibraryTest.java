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
    @Test void rotationsMatchIndexMappingForBothListTypesAndExtremeDistancesWithBoundedWork() {
        for (int size : List.of(0, 1, 3, 137)) {
            var input = new ArrayList<Integer>();
            for (int i = 0; i < size; i++) input.add(i);
            for (int distance : List.of(0, 1, -1, 2, -2, 137, -137, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                var expected = new ArrayList<>(input);
                for (int i = 0; i < size; i++) expected.set((int)Math.floorMod((long)i + distance, size), input.get(i));
                int[] writes = {0};
                var array = new ArrayList<>(input) {
                    @Override public Integer set(int index, Integer value) { writes[0]++; return super.set(index, value); }
                    @Override public Integer remove(int index) { throw new AssertionError("Rotation should not shift/remove each step"); }
                };
                ReikaJavaLibrary.cycleList(array, distance);
                assertEquals(expected, array, () -> "size=" + size + ", distance=" + distance);
                assertTrue(writes[0] <= size);
                var linked = new java.util.LinkedList<>(input);
                ReikaJavaLibrary.cycleLinkedList(linked, distance);
                assertEquals(expected, linked, () -> "linked size=" + size + ", distance=" + distance);
            }
        }
    }

    @Test void rotationsPreserveNullsDuplicatesAndFixedSizeListsAndSkipLogicalNoOps() {
        List<Integer> fixed = Arrays.asList(1, null, 1, 2);
        ReikaJavaLibrary.cycleList(fixed, 1);
        assertEquals(Arrays.asList(2, 1, null, 1), fixed);
        ReikaJavaLibrary.cycleList(fixed, -1);
        assertEquals(Arrays.asList(1, null, 1, 2), fixed);
        assertDoesNotThrow(() -> ReikaJavaLibrary.cycleList(List.of(), Integer.MIN_VALUE));
        assertDoesNotThrow(() -> ReikaJavaLibrary.cycleList(List.of("only"), Integer.MAX_VALUE));
        assertDoesNotThrow(() -> ReikaJavaLibrary.cycleList(List.of("a", "b", "c"), 3));
    }

    @Test void classLocationsUseBinaryResourceNamesAndDecodeFileUrisAndRetainModuleUrls(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var compiled = compileScanFixtures(directory);
        String binaryName = "portfixtures.scanbase.PackageScanFixture$Nested";
        byte[] bytes = java.nio.file.Files.readAllBytes(compiled.resolve(binaryName.replace('.', '/') + ".class"));
        var file = directory.resolve("compiled + caf\u00e9 #%.class");
        java.nio.file.Files.write(file, bytes);
        String expectedResource = binaryName.replace('.', '/') + ".class";
        var requests = new ArrayList<String>();
        class ResourceLoader extends ClassLoader {
            private final java.net.URL resource;
            ResourceLoader(java.net.URL resource) { this.resource = resource; }
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
            @Override public java.net.URL getResource(String name) { requests.add(name); return resource; }
        }
        assertEquals(file.toString(), ReikaJavaLibrary.getClassLocation(new ResourceLoader(file.toUri().toURL()).define()));
        assertEquals(List.of(expectedResource), requests);
        assertNull(ReikaJavaLibrary.getClassLocation(new ResourceLoader(null).define()));
        assertNull(ReikaJavaLibrary.getClassLocation(null));
        assertNull(ReikaJavaLibrary.getClassLocation(int.class));
        assertEquals(String.class.getResource("String.class").toExternalForm(), ReikaJavaLibrary.getClassLocation(String.class));
    }

    /** Runtime fixtures avoid NeoForge's guardian replacing mod-owned classes in foreign loaders. */
    private static java.nio.file.Path compileScanFixtures(java.nio.file.Path directory) throws Exception {
        var output = java.nio.file.Files.createDirectories(directory.resolve("compiled"));
        var sources = List.of(
                "package portfixtures.scanbase; class PackageScanFixture extends java.util.ArrayList<String> { static class Nested {} } abstract class AbstractScanFixture {} @Deprecated class DeprecatedScanFixture {}",
                "package portfixtures.scanbase.child; class ChildScanFixture {}",
                "package portfixtures.scanbaseextra; class OtherScanFixture {}");
        var arguments = new ArrayList<>(List.of("-proc:none", "-d", output.toString()));
        for (int i = 0; i < sources.size(); i++) {
            var source = directory.resolve("ScanFixture" + i + ".java");
            java.nio.file.Files.writeString(source, sources.get(i));
            arguments.add(source.toString());
        }
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "The project requires a JDK");
        var diagnostics = new java.io.ByteArrayOutputStream();
        assertEquals(0, compiler.run(null, diagnostics, diagnostics, arguments.toArray(String[]::new)), diagnostics::toString);
        return output;
    }

    private static java.nio.file.Path fixtureJar(java.nio.file.Path destination, java.nio.file.Path compiled, List<String> classes) throws Exception {
        try (var jar = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(destination))) {
            for (String name : classes) {
                String entry = name.replace('.', '/') + ".class";
                jar.putNextEntry(new java.util.jar.JarEntry(entry));
                try (var input = java.nio.file.Files.newInputStream(compiled.resolve(entry))) {
                    input.transferTo(jar);
                }
                jar.closeEntry();
            }
        }
        return destination;
    }

    @Test void classLocationsPreserveCompleteJarUrls(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        String name = "portfixtures.scanbase.PackageScanFixture";
        var jar = fixtureJar(directory.resolve("classes + #%.jar"), compileScanFixtures(directory), List.of(name));
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
            Class<?> type = Class.forName(name, false, loader);
            String location = ReikaJavaLibrary.getClassLocation(type);
            assertEquals(type.getResource("/" + name.replace('.', '/') + ".class").toExternalForm(), location);
            assertTrue(location.startsWith("jar:file:"));
            assertTrue(location.endsWith("!/" + name.replace('.', '/') + ".class"));
            assertTrue(location.contains("%23"));
        }
    }

    @Test void packageScanningHonorsBoundariesSubpackagesFiltersAndContextFallback(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        String pack = "portfixtures.scanbase";
        String regular = pack + ".PackageScanFixture", child = pack + ".child.ChildScanFixture";
        var classes = List.of(regular, child, pack + ".AbstractScanFixture", pack + ".DeprecatedScanFixture",
                pack + "extra.OtherScanFixture");
        var jar = fixtureJar(directory.resolve("package scan.jar"), compileScanFixtures(directory), classes);
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
            thread.setContextClassLoader(loader);
            Set<String> expected = Set.copyOf(classes.subList(0, 4));
            assertEquals(expected, classNames(ReikaJavaLibrary.getAllClassesFromPackage(pack, null, false, false)));
            assertEquals(expected, classNames(ReikaJavaLibrary.getAllClassesFromPackage(pack + ".", null, false, false)));
            Class<?> abstractType = Class.forName(pack + ".AbstractScanFixture", false, loader);
            assertTrue(java.lang.reflect.Modifier.isAbstract(abstractType.getModifiers()), "Jar must retain abstract metadata");
            assertTrue(Class.forName(pack + ".DeprecatedScanFixture", false, loader).isAnnotationPresent(Deprecated.class), "Jar must retain annotation metadata");
            assertEquals(Set.of(regular, child), classNames(ReikaJavaLibrary.getAllClassesFromPackage(pack, null, true, true)));
            assertEquals(Set.of(regular), classNames(ReikaJavaLibrary.getAllClassesFromPackage(pack, List.class, false, false)));
            assertTrue(ReikaJavaLibrary.getAllClassesFromPackage("portfixtures.scanbas", null, false, false).isEmpty());
            assertEquals(Set.copyOf(classes), classNames(ReikaJavaLibrary.getAllClassesFromPackage("", null, false, false)));
            thread.setContextClassLoader(null);
            assertEquals(Set.of(ReikaJavaLibrary.class), Set.copyOf(ReikaJavaLibrary.getAllClassesFromPackage(
                    "reika.dragonapi.libraries.java", ReikaJavaLibrary.class, false, false)));
            assertEquals(Set.of(String.class), Set.copyOf(ReikaJavaLibrary.getAllClassesFromPackage(
                    "java.lang", String.class, false, false)));
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private static Set<String> classNames(Collection<Class<?>> classes) {
        return classes.stream().map(Class::getName).collect(java.util.stream.Collectors.toSet());
    }

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
