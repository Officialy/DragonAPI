/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.java;

import com.google.common.reflect.ClassPath;
import org.jspecify.annotations.Nullable;
import java.util.function.IntFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.classloading.ModuleClassLoader;
import org.apache.logging.log4j.Level;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.exception.MisuseException;

import java.io.*;
import java.lang.module.Configuration;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

public final class ReikaJavaLibrary {

    private static final HashMap<String, Object> threadLock = new HashMap<>();
    private static final char[] IDChars = new char[]{
            '\u03b1', '\u03b2', '\u03b3', '\u03b4', '\u03b5', '\u03b6', '\u03b7', '\u03b8', '\u03b9', '\u03ba', '\u03bb', '\u03bc', '\u03bd', '\u03be',
            '\u03bf', '\u03c0', '\u03c1', '\u03c2', '\u03c3', '\u03c4', '\u03c5', '\u03c6', '\u03c7', '\u03c8', '\u03c9', '\u0393', '\u0394', '\u0398',
            '\u039b', '\u03a0', '\u03a3', '\u03a6', '\u03a8', '\u03a9', '\u0414', '\u0416', '\u0418', '\u0428', '\u042c', '\u042d', '\u042e', '\u042f',
            '\u05d0', '\u05d1', '\u05d2', '\u05d3', '\u05d4', '\u05d7', '\u05d8', '\u05da', '\u05db', '\u05dc', '\u05dd', '\u05de', '\u05e1', '\u05e2',
            '\u05e3', '\u05e4', '\u05e6', '\u05e7', '\u05e8', '\u05e9', '\u05ea'
    };
    public static boolean dumpStack = false;

    //private static final boolean printClasses = ReikaJVMParser.isArgumentPresent("-DragonAPI_printClassInit");
    public static boolean silent = false;
    private static int maxRecurse = -1;

    /**
     * Generic write-to-console function. Args: Object
     */
    public static void pConsole(Object obj) {
        DragonAPI.LOGGER.log(Level.INFO, obj);
    }

   public static void dumpStack() {
       var logger = DragonAPI.LOGGER;
       if (logger.isDebugEnabled()) {
           StringBuilder sb = new StringBuilder("Stack Trace:\n");
           StackTraceElement[] s = new Exception("Stack Trace").getStackTrace();
           for (int i = 1; i < s.length; i++) {
               sb.append("\t").append(s[i].toString()).append("\n");
           }
           logger.debug(sb.toString());
       }
   }

    public static void spamConsole(Object obj) {
        String sg = String.valueOf(obj);
        for (int i = 0; i < 16; i++)
            pConsole(sg);
    }

    public static void pConsole(String obj, Dist s) {
        if (FMLEnvironment.getDist() == s)
            pConsole(obj);
    }

    public static void pConsole(String obj, Dist s, boolean con) {
        if (con)
            pConsole(obj, s);
    }

    public static void pConsole(Object obj, boolean con) {
        if (con)
            pConsole(obj);
    }

    /**
     * A complement to Java's built-in List-to-Array. Args: Array of any object (ints, strings, etc).
     */
    public static <E> HashSet<E> makeSetFromArray(E[] obj) {
        HashSet<E> li = new HashSet<>();
        Collections.addAll(li, obj);
        return li;
    }

    /**
     * A complement to Java's built-in List-to-Array. Args: Array of any object (ints, strings, etc).
     */
    public static <E> ArrayList<E> makeListFromArray(E[] obj) {
        ArrayList<E> li = new ArrayList<>();
        Collections.addAll(li, obj);
        return li;
    }

    public static ArrayList<Integer> makeIntListFromArray(int... obj) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int j : obj) {
            li.add(j);
        }
        return li;
    }

    public static ArrayList<Byte> makeIntListFromArray(byte... obj) {
        ArrayList<Byte> li = new ArrayList<>();
        for (byte b : obj) {
            li.add(b);
        }
        return li;
    }

    public static <E> ArrayList<E> makeListFrom(E obj) {
        ArrayList<E> li = new ArrayList<>();
        li.add(obj);
        return li;
    }

    @SafeVarargs
    public static <E> ArrayList<E> makeListFrom(E... obj) {
        ArrayList<E> li = new ArrayList<>();
        Collections.addAll(li, obj);
        return li;
    }

	public static boolean isValidInteger(String s) {
		if (s == null || s.isEmpty() || s.contentEquals("-"))
			return false;
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /** Parses the historical ASCII decimal syntax, returning zero for invalid or out-of-range input. */
    public static int safeIntParse(@Nullable String s) {
        if (!isSignedDecimal(s)) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    /** Parses the historical ASCII decimal syntax, returning zero for invalid or out-of-range input. */
    public static long safeLongParse(@Nullable String s) {
        if (!isSignedDecimal(s)) return 0;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static boolean isSignedDecimal(@Nullable String s) {
        if (s == null || s.isEmpty()) return false;
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start == s.length()) return false;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        }
        return true;
    }

    public static void printLine(int length) {
        pConsole("-".repeat(Math.max(0, length)));
    }

    public static <T, E> @Nullable T getHashMapKeyByValue(HashMap<T, E> map, @Nullable E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Probes availability without initialization, using the context loader when one is installed. */
    public static boolean doesClassExist(String cl) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = ReikaJavaLibrary.class.getClassLoader();
        try {
            Class.forName(cl, false, loader);
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        } catch (LinkageError error) {
            DragonAPI.LOGGER.debug("Class {} is unavailable in loader {}", cl, loader, error);
            return false;
        }
    }

    /** Retains initialization in this library's defining loader; missing or unlinkable classes return null. */
    public static @Nullable Class<?> getClassNoException(String cl) {
        try {
            return Class.forName(cl);
        } catch (ClassNotFoundException missing) {
            return null;
        } catch (LinkageError error) {
            DragonAPI.LOGGER.error("Could not load and initialize class {}", cl, error);
            return null;
        }
    }

    public static void initClass(String className) {
        try {
            Class.forName(className, true, ReikaJavaLibrary.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not initialize class {}", className, error);
        }
    }

    /** Initializes the supplied class in its own defining loader. */
    public static void initClass(@Nullable Class<?> type) {
        if (type == null) {
            DragonAPI.LOGGER.error("Cannot initialize a null class", new IllegalArgumentException("class"));
            return;
        }
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException | LinkageError error) {
            DragonAPI.LOGGER.error("Could not initialize class {} in loader {}", type.getName(), type.getClassLoader(), error);
            printClassMetadata(type);
        } catch (RuntimeException error) {
            DragonAPI.LOGGER.error("Could not initialize class {} in loader {}", type.getName(), type.getClassLoader(), error);
        }
    }

    public static void printClassSource(Class<?> c, String path) {
        printClassSource(path + c.getName(), getClassBytes(c));
    }

    public static void printClassMetadata(Class<?> c) {
        printClassMetadata(c.getName(), c);
    }

    public static void printClassSource(Class<?> c) {
        printClassSource(c.getName(), getClassBytes(c));
    }

    /** Returns an empty array when unavailable; any resource opened here is owned and closed here. */
	public static byte[] getClassBytes(@Nullable Class<?> c) {
		if (c == null)
			return new byte[0];
		String className = c.getName();
		String classAsPath = "/" + className.replace('.', '/') + ".class";
		try (InputStream stream = c.getResourceAsStream(classAsPath)) {
			if (stream == null) {
				DragonAPI.LOGGER.warn("Class resource {} not found for {} in loader {}", classAsPath, className, c.getClassLoader());
				return new byte[0];
			}
			return stream.readAllBytes();
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not read class resource {} for {}", classAsPath, className, error);
            return new byte[0];
        }
    }

	public static void printClassMetadata(String path, Class<?> c) {
        Objects.requireNonNull(c, "class");
		Path destination = Path.of("FailedClasses", path + ".classdata");
		try {
			Files.createDirectories(destination.toAbsolutePath().getParent());
			try (BufferedWriter p = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination), Charset.defaultCharset()))) {
				printClassMetadata(p, c);
			}
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not write metadata for {} to {}", c.getName(), destination.toAbsolutePath(), error);
        }
    }

    private static void printClassMetadata(BufferedWriter p, Class<?> c) throws IOException {
        try {
            p.write("General:\n");
            p.write("\t" + c.getName() + "\n");
            p.write("\tAnnotations: " + Arrays.toString(c.getAnnotations()) + "\n");
            p.write("\tModifiers: " + parseModifiers(c.getModifiers()) + "\n");
            p.write("\tSuperclass: " + c.getSuperclass() + "\n");
            p.write("\tInterfaces: " + Arrays.toString(c.getInterfaces()) + "\n");
            p.write("\tSynthetic: " + c.isSynthetic() + "\n");
            p.write("\n\n");
        } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
            reportMetadataFailure(p, c, "ROOT DATA", error);
        }

        try {
            p.write("Internal Classes:\n");
            for (Class<?> cs : c.getDeclaredClasses()) {
                try {
                    p.write("-------------------------\n");
                    printClassMetadata(p, cs);
                    p.write("-------------------------\n");
                } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
                    reportMetadataFailure(p, c, "INTERNAL CLASS", error);
                }
            }
            p.write("\n\n");
        } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
            reportMetadataFailure(p, c, "INTERNAL CLASSES", error);
        }

        try {
            p.write("Constructors:\n");
            for (Constructor<?> cs : c.getDeclaredConstructors()) {
                try {
                    p.write("\t\tAnnotations: " + Arrays.toString(cs.getAnnotations()) + "\n");
                    p.write("\t\tModifiers: " + parseModifiers(cs.getModifiers()) + "\n");
                    p.write("\t\tSignature: " + Arrays.toString(cs.getParameterTypes()) + "\n");
                    p.write("\t\tExceptions: " + Arrays.toString(cs.getExceptionTypes()) + "\n");
                    p.write("\t\tSynthetic: " + cs.isSynthetic() + "\n\n");
                } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
                    reportMetadataFailure(p, c, "CONSTRUCTOR", error);
                }
            }
            p.write("\n\n");
        } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
            reportMetadataFailure(p, c, "CONSTRUCTORS", error);
        }

        try {
            p.write("Fields:\n");
            for (Field fd : c.getDeclaredFields()) {
                try {
                    p.write("\t" + fd.getName() + "\n");
                    p.write("\t\tAnnotations: " + Arrays.toString(fd.getAnnotations()) + "\n");
                    p.write("\t\tModifiers: " + parseModifiers(fd.getModifiers()) + "\n");
                    p.write("\t\tType: " + fd.getType() + "\n");
                    p.write("\t\tSynthetic: " + fd.isSynthetic() + "\n\n");
                } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
                    reportMetadataFailure(p, c, "FIELD", error);
                }
            }
            p.write("\n\n");
        } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
            reportMetadataFailure(p, c, "FIELDS", error);
        }

        try {
            p.write("Methods:\n");
            for (Method m : c.getDeclaredMethods()) {
                try {
                    p.write("\t" + m.getName() + "\n");
                    p.write("\t\tAnnotations: " + Arrays.toString(m.getAnnotations()) + "\n");
                    p.write("\t\tModifiers: " + parseModifiers(m.getModifiers()) + "\n");
                    p.write("\t\tSignature: " + Arrays.toString(m.getParameterTypes()) + "\n");
                    p.write("\t\tExceptions: " + Arrays.toString(m.getExceptionTypes()) + "\n");
                    p.write("\t\tReturn: " + m.getReturnType() + "\n");
                    p.write("\t\tSynthetic: " + m.isSynthetic() + "\n\n");
                } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
                    reportMetadataFailure(p, c, "METHOD", error);
                }
            }
            p.write("\n\n");
        } catch (LinkageError | RuntimeException | java.lang.annotation.AnnotationFormatError error) {
            reportMetadataFailure(p, c, "METHODS", error);
        }
    }

    private static void reportMetadataFailure(BufferedWriter writer, Class<?> type, String section, Throwable error) throws IOException {
        DragonAPI.LOGGER.error("Could not inspect {} metadata for {}", section, type.getName(), error);
        writer.write("ERRORED " + section + "\n");
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        writer.write(trace.toString());
    }

    private static String parseModifiers(int modifiers) {
        ArrayList<String> params = new ArrayList<>();
        for (ClassModifiers mod : ClassModifiers.values()) {
            if (mod.match(modifiers)) {
                params.add(mod.toString());
            }
        }
        return params.toString();
    }

    public static void printClassSource(String path, byte[] data) {
        Objects.requireNonNull(data, "class bytes");
        //read in, build classNode
		/*
		ClassNode classNode = new ClassNode();
		ClassReader cr = new ClassReader(data);
		cr.accept(classNode, 0);

		//peek at classNode and modifier
		List<MethodNode> methods = classNode.methods;
		for (MethodNode method : methods) {
			System.out.println("name = "+method.name+" desc = "+method.desc);
			InsnList insnList = method.instructions;
			Iterator ite = insnList.iterator();

			while(ite.hasNext()) {
				AbstractInsnNode insn = (AbstractInsnNode)ite.next();
				int opcode = insn.getOpcode();
				//add before return: System.out.println("Returning ... ")
				if (opcode == Opcodes.RETURN) {
					InsnList tempList = new InsnList();
					tempList.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
					tempList.add(new LdcInsnNode("Returning ... "));
					tempList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println", "(Ljava/lang/String;)V"));
					insnList.insert(insn.getPrevious(), tempList);
					method.maxStack += 2;
				}
			}
		}

		//write classNode
		ClassWriter out = new ClassWriter(0);
		classNode.accept(out);
		data = out.toByteArray() */

        String filename = path + ".class";
        try (OutputStream fos = Files.newOutputStream(Path.of(filename))) {
            fos.write(data);
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not write {} class bytes to {}", data.length, filename, error);
        }
    }

    public static void initClassWithSubs(Class<?> c) {
        initClass(c);
        for (Class<?> c2 : c.getDeclaredClasses()) {
            initClass(c2);
        }
    }

    public static boolean listContainsArray(List<int[]> li, int[] arr) {
        for (int[] ints : li) {
            if (Arrays.equals(ints, arr)) {
                return true;
            }
        }
        return false;
    }

    public static int getEnumLengthWithoutInitializing(Class<? extends Enum<?>> c) {
        Field[] q = c.getFields();
        int count = 0;
        for (Field f : q) {
            if (f.isEnumConstant())
                count++;
        }
        return count;
    }

    public static ArrayList<String> getEnumEntriesWithoutInitializing(Class<? extends Enum<?>> c) {
        ArrayList<String> li = new ArrayList<>();
        Field[] q = c.getFields();
        for (Field f : q) {
            if (f.isEnumConstant())
                li.add(f.getName());
        }
        return li;
    }

    /**
     * Returns the maximum allowable depth of recursion on the current system.
     * Keep in mind that this number is the <i>total</i> stack depth and as such contains some
     * vanilla MC and Forge calls as well. Subtract 100 or so to be safe.
     */
    public static int getMaximumRecursiveDepth() {
        if (maxRecurse <= 0) {
            recurse(0);
        }
        return maxRecurse;
    }

    private static int recurse(int i) {
        maxRecurse = Math.max(i, maxRecurse);
//        pConsole(i+":"+maxRecurse);
        try {
            recurse(i + 1);
        } catch (StackOverflowError e) {
            return i;
        }
        return 0;
    }

    public static void toggleStackTrace() {
        dumpStack = !dumpStack;
    }

    public static void toggleSilentMode() {
        silent = !silent;
    }

    public static Class<?>[] getObjectClasses(Object... objs) {
        Class<?>[] c = new Class<?>[objs.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = getClassOf(objs[i]);
        }
        return c;
    }

    /** Infers primitive types for actual boxed values; null has no inferable runtime type. */
    public static Class<?> getClassOf(Object o) {
        Objects.requireNonNull(o, "Cannot infer a class from null");
        Class<?> p = getPrimitiveClass(o);
        return p != null ? p : o.getClass();
    }

    private static Class<?> getPrimitiveClass(Object o) {
        if (o instanceof Byte)
            return byte.class;
        if (o instanceof Short)
            return short.class;
        if (o instanceof Integer)
            return int.class;
        if (o instanceof Long)
            return long.class;
        if (o instanceof Character)
            return char.class;
        if (o instanceof Float)
            return float.class;
        if (o instanceof Double)
            return double.class;
        if (o instanceof Boolean)
            return boolean.class;
        if (o instanceof Void)
            return void.class;
        return null;
    }

    public static <K, T> boolean collectionMapContainsValue(Map<K, Collection<T>> map, T value) {
        for (Collection<T> c : map.values()) {
            if (c != null && c.contains(value))
                return true;
        }
        return false;
    }

    /** Reads through EOF without closing the caller-owned stream; failures retain the null result. */
    public static byte @Nullable [] streamToBytes(InputStream input) {
        try {
            return input.readAllBytes();
        } catch (IOException error) {
            DragonAPI.LOGGER.error("Could not read bytes from stream type {}", input.getClass().getName(), error);
            return null;
        }
    }

    /** Constructor lookup failures retain the historical null result and full diagnostic cause. */
    public static <I, O> @Nullable Collection<O> getConstructedCollection(Collection<? extends I> inputs,
            Class<I> inputType, Class<? extends O> outputType) {
        try {
            return getConstructedCollection(inputs, outputType.getConstructor(inputType));
        } catch (ReflectiveOperationException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not construct collection of {} from {}", outputType.getName(), inputType.getName(), error);
            return null;
        }
    }

    /** Preserves input order; a constructor failure never returns a partial collection. */
    public static <I, O> Collection<O> getConstructedCollection(Collection<? extends I> inputs, Constructor<? extends O> constructor) {
        Collection<O> outputs = new ArrayList<>();
        int index = 0;
        for (I input : inputs) {
            try {
                outputs.add(constructor.newInstance(input));
            } catch (ReflectiveOperationException | IllegalArgumentException error) {
                Throwable cause = error instanceof java.lang.reflect.InvocationTargetException invocation
                        ? invocation.getCause() : error;
                throw new IllegalStateException("Could not construct collection entry " + index + " of "
                        + constructor.getDeclaringClass().getName(), cause);
            }
            index++;
        }
        return outputs;
    }

    /** Returns an ordered copy, retaining encounter order for equal values. */
    public static <K, V extends Comparable<? super V>> HashMap<K, V> sortMapByValues(HashMap<K, V> map) {
        return sortMapByValues(map, Comparator.naturalOrder());
    }

    /** Supports non-comparable values and explicit null ordering without changing the input map. */
    public static <K, V> HashMap<K, V> sortMapByValues(HashMap<K, V> map, Comparator<? super V> comparator) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue(comparator));
        HashMap<K, V> sortedHashMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : list) {
            sortedHashMap.put(e.getKey(), e.getValue());
        }
        return sortedHashMap;
    }

    /** Untyped collections produce Object[], without promising an unrepresentable element array type. */
    public static Object[] collectionToArray(Collection<?> values) {
        return values.toArray();
    }

    /** Supplies the runtime component type, including for empty and nullable-element collections. */
    public static <E> E[] collectionToArray(Collection<? extends E> values, IntFunction<E[]> arrayFactory) {
        return values.toArray(arrayFactory);
    }

    public static <E> @Nullable E getRandomListEntry(Random rand, List<E> values) {
        return values.isEmpty() ? null : values.get(rand.nextInt(values.size()));
    }

    public static <E> @Nullable E getRandomListEntry(RandomSource rand, List<E> values) {
        return values.isEmpty() ? null : values.get(rand.nextInt(values.size()));
    }

    public static <E> @Nullable E getRandomCollectionEntry(RandomSource rand, Collection<E> values) {
        if (values.isEmpty()) return null;
        return getRandomListEntry(rand, values instanceof List<E> list ? list : new ArrayList<>(values));
    }

    public static <E> @Nullable E getRandomCollectionEntry(Random rand, Collection<E> values) {
        if (values.isEmpty()) return null;
        return getRandomListEntry(rand, values instanceof List<E> list ? list : new ArrayList<>(values));
    }

    public static <E> @Nullable E getAndRemoveRandomCollectionEntry(Random rand, Collection<E> values) {
        if (values.isEmpty()) return null;
        if (values instanceof List<E> list) return list.remove(rand.nextInt(list.size()));
        E value = getRandomCollectionEntry(rand, values);
        values.remove(value);
        return value;
    }

    public static String getTopLevelPackage(Class<?> c) {
        String name = c.getPackageName();
        int separator = name.indexOf('.');
        return separator < 0 ? name : name.substring(0, separator);
    }

    public static <E> Collection<E> getCompoundCollection(Collection<? extends Collection<? extends E>> colls) {
        Collection<E> c = new ArrayList<>();
        for (Collection<? extends E> c2 : colls) {
            c.addAll(c2);
        }
        return c;
    }

    public static boolean isAnyModLoaded(ModList[] mods) {
        for (ModList mod : mods) {
            if (mod.isLoaded())
                return true;
        }
        return false;
    }

	public static boolean removeValuesFromMap(Map<?, ?> map, Object value) {
		boolean flag = false;
		Iterator<? extends Entry<?, ?>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			if (Objects.equals(iterator.next().getValue(), value)) {
				iterator.remove();
				flag = true;
			}
        }
        return flag;
    }

    public static int getNestedMapSize(Map<?, ?> map) {
        int s = 0;
        for (Object value : map.values()) {
            s += value instanceof Map<?, ?> nested ? nested.size() : 1;
        }
        return s;
    }

    public static <V> Collection<V> getValuesForMapOfMaps(Map<?, ? extends Map<?, ? extends V>> map) {
        Collection<V> li = new ArrayList<>();
        for (Map<?, ? extends V> o : map.values()) {
            li.addAll(o.values());
        }
        return li;
    }

    public static <E> Collection<E> getValuesForMapOfCollections(Map<?, ? extends Collection<? extends E>> map) {
        Collection<E> li = new ArrayList<>();
        for (Collection<? extends E> o : map.values()) {
            li.addAll(o);
        }
        return li;
    }

    /** Positive distances rotate right, negative distances left; work is bounded by list size. */
    public static <E> void cycleList(List<E> li, int n) {
        if (li.isEmpty()) return;
        int distance = Math.floorMod(n, li.size());
        if (distance != 0) Collections.rotate(li, distance);
    }

    public static <E> void cycleLinkedList(LinkedList<E> li, int n) {
        cycleList(li, n);
    }

    @SafeVarargs
    public static <V> Collection<V> combineCollections(Collection<? extends V>... colls) {
        Collection<V> ret = new ArrayList<>();
        for (Collection<? extends V> coll : colls) {
            ret.addAll(coll);
        }
        return ret;
    }

    @SafeVarargs
    public static <E> Set<E> getSet(E... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * Expensive diagnostic lookup of live platform threads; virtual threads are not enumerated.
     * Duplicate names may return any match, and a returned thread may terminate after the snapshot.
     */
    public static @Nullable Thread getThreadByName(@Nullable String name) {
        if (name == null) return null;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (name.equals(thread.getName()) && thread.isAlive()) return thread;
        }
        return null;
    }

    public static ThreadGroup getTopLevelThreadGroup() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }
        return tg;
    }

    public static boolean exceptionMentions(Throwable e, Class<?> c) {
        StackTraceElement[] arr = e.getStackTrace();
        for (StackTraceElement stackTraceElement : arr) {
            if (stackTraceElement.getClassName().equals(c.getName()))
                return true;
        }
        return false;
    }

    public static <K> void subtractFromIntMap(Map<K, Integer> map, K key, int num) {
        Integer get = map.get(key);
        int ret = (get != null ? get : 0) - num;
        map.put(key, ret);
    }

    public static <K> void addToIntMap(Map<K, Integer> map, K key, int num) {
        Integer get = map.get(key);
        int ret = (get != null ? get : 0) + num;
        map.put(key, ret);
    }

    /**
     * Not exactly performant. Do not call this excessively, or ever during the main game loop.
     */
    public static Collection<Class<?>> getAllClassesFromPackage(String pack, @Nullable Class<?> parent, boolean skipAbstract, boolean skipDeprecated) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = ReikaJavaLibrary.class.getClassLoader();
        String prefix = pack.isEmpty() || pack.endsWith(".") ? pack : pack + ".";
        Set<String> names = new LinkedHashSet<>();
        for (ClassPath.ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
            if (info.getName().startsWith(prefix)) names.add(info.getName());
        }
        if (loader instanceof ModuleClassLoader modules) {
            collectModuleClasses(modules.getConfiguration(), prefix, names);
        }
        Collection<Class<?>> li = new ArrayList<>();
        for (String name : names) {
            Class<?> c;
            try {
                c = Class.forName(name, false, loader);
            } catch (ClassNotFoundException error) {
                throw new IOException("Could not load discovered class " + name + " in loader " + loader, error);
            }
            if (parent != null && !parent.isAssignableFrom(c)) continue;
            if (skipAbstract && Modifier.isAbstract(c.getModifiers())) continue;
            if (skipDeprecated && c.isAnnotationPresent(Deprecated.class)) continue;
            li.add(c);
        }
        return li;
    }

    private static void collectModuleClasses(Configuration configuration, String prefix, Set<String> names) throws IOException {
        Set<Configuration> visited = new HashSet<>();
        Deque<Configuration> pending = new ArrayDeque<>();
        pending.add(configuration);
        while (!pending.isEmpty()) {
            Configuration current = pending.removeFirst();
            if (!visited.add(current)) continue;
            pending.addAll(current.parents());
            for (var module : current.modules()) {
                if (module.reference().descriptor().packages().stream().noneMatch(pkg -> (pkg + ".").startsWith(prefix))) continue;
                try (var reader = module.reference().open(); var resources = reader.list()) {
                    resources.sequential().filter(resource -> resource.endsWith(".class") && !resource.contains("$")
                                    && !resource.startsWith("META-INF/") && !resource.equals("module-info.class"))
                            .map(resource -> resource.substring(0, resource.length() - ".class".length()).replace('/', '.'))
                            .filter(name -> name.startsWith(prefix)).forEach(names::add);
                } catch (IOException | UncheckedIOException error) {
                    throw new IOException("Could not scan module " + module.name() + " for package prefix " + prefix, error);
                }
            }
        }
    }

    /**
     * Starts at lowercase alpha, goes through entire alphabet, reuses some as caps, moves onto Cryllic, then Hebrew.
     */
    public static char getIDChar(int id) {
        return IDChars[id % IDChars.length];
    }

    public static int[] splitLong(long val) {
        int l1 = (int) (val >>> 32);
        int l2 = (int) (val & 0xFFFFFFFFL);
        return new int[]{l1, l2};
    }

    public static long buildLong(int l1, int l2) {
        return ((long) l1 << 32) | (l2 & 0xffffffffL);
    }

    public static byte[] splitInt(int val) {
        byte[] ret = new byte[4];
        ret[0] = (byte) ((val) & 255);
        ret[1] = (byte) ((val >>> 8) & 255);
        ret[2] = (byte) ((val >>> 16) & 255);
        ret[3] = (byte) ((val >>> 24) & 255);
        return ret;
    }

    public static int buildInt(byte b1, byte b2, byte b3, byte b4) {
        return (b1 & 255) | ((b2 & 255) << 8) | ((b3 & 255) << 16) | ((b4 & 255) << 24);
    }

    /** Returns a native path for file resources and the intact URL for jar/module/other resources. */
    public static @Nullable String getClassLocation(@Nullable Class<?> c) {
        if (c == null) return null;
        String resource = "/" + c.getName().replace('.', '/') + ".class";
        try {
            URL location = c.getResource(resource);
            if (location == null) return null;
            return location.getProtocol().equals("file") ? Path.of(location.toURI()).toString() : location.toExternalForm();
        } catch (URISyntaxException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not locate class resource {} for {}", resource, c.getName(), error);
            return null;
        }
    }

    public static double buildDoubleFromInts(int i1, int i2) {
		/*
		byte[] arr = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(arr);
		buf.putInt(i1);
		buf.putInt(i2);
		return ByteBuffer.wrap(arr).getDouble();
		 */
        return Double.longBitsToDouble(buildLong(i1, i2));
    }

    public static int[] splitDoubleToInts(double val) {
		/*
		byte[] arr = new byte[8];
		ByteBuffer.wrap(arr).putDouble(val);
		ByteBuffer buf = ByteBuffer.wrap(arr);
		int i1 = buf.getInt();
		int i2 = buf.getInt();
		return new int[]{i1, i2};
		 */
        return splitLong(Double.doubleToRawLongBits(val));
    }

    public static byte[] splitIntToHexChars(int val) {
        byte[] arr = new byte[8];
        for (int i = 0; i < 8; i++) {
            byte hex = (byte) ((val >> (i * 4)) & 0xF);
            arr[i] = hex;
        }
        return arr;
    }

    public static int buildIntFromHexChars(byte[] chars) {
        if (chars.length != 8)
            throw new MisuseException("You cannot build an int from less than or more than 8 nibbles!");
        int val = 0;
        for (int i = 0; i < 8; i++) {
            val |= chars[i] << (i * 4);
        }
        return val;
    }

    /** Copies before sorting; the declared base type selects natural ordering or the historical hash fallback. */
    public static <V> ArrayList<V> makeSortedListFromCollection(Collection<? extends V> c, Class<? super V> baseElementType) {
        ArrayList<V> li = new ArrayList<>(c);
        if (!li.isEmpty()) {
            li.sort(new FallbackComparator<V>(baseElementType));
        }
        return li;
    }

    public static byte flipBits(byte get) {
        return (byte) (Integer.reverse(get) >>> 24);
    }

    public static <K, V> HashMap<K, V> makeMapOf(K key, V val) {
        HashMap<K, V> map = new HashMap<>();
        map.put(key, val);
        return map;
    }

    public static <E> Collection<E> cloneCollectionObjects(Collection<E> c, CloneCallback<E> call) {
        Collection<E> ret = new ArrayList<>();
        for (E o : c) {
            ret.add(call.clone(o));
        }
        return ret;
    }

    /**
     * Order agnostic
     */
    public static <E> boolean collectionsHaveSameValues(Collection<E> c1, Collection<E> c2) {
        return new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    public static Object copyObject(Object o) {
        if (o instanceof Object[])
            return ReikaArrayHelper.deepCopyArray((Object[]) o);
        if (o instanceof ArrayList)
            return new ArrayList<>((ArrayList<?>) o);
        if (o instanceof LinkedList)
            return new LinkedList<>((LinkedList<?>) o);
        if (o instanceof HashMap)
            return new HashMap<>((HashMap<?, ?>) o);
        if (o instanceof TreeMap)
            return new TreeMap<>((TreeMap<?, ?>) o);
        if (o instanceof HashSet)
            return new HashSet<>((HashSet<?>) o);
        if (o instanceof Map)
            return new HashMap<>((Map<?, ?>) o);
        if (o instanceof Collection)
            return new ArrayList<>((Collection<?>) o);
        if (o instanceof ItemStack)
            return ((ItemStack) o).copy();
        return o;
    }

    public static String getPackageName(Class<?> c) {
        return c.getPackageName();
    }

    public static String getEnumNameList(Class<? extends Enum<?>> e) {
        StringBuilder sb = new StringBuilder();
        Enum<?>[] list = e.getEnumConstants();
        for (int i = 0; i < list.length; i++) {
            Enum<?> loc = list[i];
            sb.append(loc.name());
            if (i < list.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    public interface CloneCallback<E> {

        E clone(E o);

    }

    public static class ReverseComparator<T extends Comparable<? super T>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            return o2.compareTo(o1);
        }

    }

    private record FallbackComparator<V>(boolean useDefaultCompare) implements Comparator<V> {

        private FallbackComparator(Class<? super V> test) {
            this(Comparable.class.isAssignableFrom(test));
        }

        @Override
        @SuppressWarnings("unchecked") // The legacy Class selector supplies the runtime Comparable contract.
        public int compare(V o1, V o2) {
            return useDefaultCompare ? ((Comparable<? super V>) o1).compareTo(o2)
                    : Integer.compare(o1.hashCode(), o2.hashCode());
        }

    }
}

