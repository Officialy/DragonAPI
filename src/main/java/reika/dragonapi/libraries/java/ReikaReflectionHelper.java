package reika.dragonapi.libraries.java;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import reika.dragonapi.libraries.io.ReikaChatHelper;

public class ReikaReflectionHelper {

    private record MethodSignature(String name, List<Class<?>> parameterTypes) {}
    private static final ClassValue<ConcurrentMap<MethodSignature, Method>> methodCache = new ClassValue<>() {
        @Override protected ConcurrentMap<MethodSignature, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /** Missing/inaccessible or non-integer fields retain the historical sentinel. */
    public static int getPrivateInteger(@Nullable Object obj, @Nullable String field) {
        return readPrivateField(obj, field, Integer.MIN_VALUE, Field::getInt);
    }

    /** Missing/inaccessible or non-boolean fields retain the historical false result. */
    public static boolean getPrivateBoolean(@Nullable Object obj, @Nullable String field) {
        return readPrivateField(obj, field, false, Field::getBoolean);
    }

    @FunctionalInterface
    private interface FieldReader<T> { T read(Field field, @Nullable Object receiver) throws IllegalAccessException; }

    private static <T> T readPrivateField(@Nullable Object obj, @Nullable String name, T fallback, FieldReader<T> reader) {
        if (obj == null || name == null || name.isEmpty()) return fallback;
        try {
            Field field = getProtectedInheritedField(obj.getClass(), name);
            if (field == null) throw new NoSuchFieldException(obj.getClass().getName() + "." + name);
            Object receiver = Modifier.isStatic(field.getModifiers()) ? null : obj;
            boolean accessible = field.canAccess(receiver);
            try {
                if (!accessible) field.setAccessible(true);
                return reader.read(field, receiver);
            } finally {
                if (!accessible) field.setAccessible(false);
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not read field {}.{}", obj.getClass().getName(), name, error);
            if (DragonAPI.config != null && Boolean.TRUE.equals(DragonAPI.config.getControl(DragonOptions.DEBUGLOG.ordinal())))
                ReikaChatHelper.write("Could not read field " + obj.getClass().getName() + "." + name + ": " + error);
            return fallback;
        }
    }

    public static @Nullable Field getProtectedInheritedField(Object o, String field) {
        return getProtectedInheritedField(o.getClass(), field);
    }

    /**
     * Gets a nonvisible field that may be inherited by any of the superclasses. Returns null if none exists.
     */
    public static @Nullable Field getProtectedInheritedField(Class<?> c, String field) {
        Field f = null;
        while (f == null && c != null) {
            try {
                f = c.getDeclaredField(field);
            } catch (NoSuchFieldException e2) {
                c = c.getSuperclass();
            }
        }
        return f;
    }

    public static @Nullable Executable getProtectedInheritedMethod(Object o, String method, Class<?>... types) {
        return getProtectedInheritedMethod(o.getClass(), method, types);
    }

    /**
     * Gets a nonvisible Method that may be inherited by any of the superclasses. Returns null if none exists.
     */
    public static @Nullable Executable getProtectedInheritedMethod(Class<?> c, String method, Class<?>... types) {
        Executable f = null;
        if (method.equals("<init>")) {
            // Constructors belong only to the declaring class; they are never inherited.
            try {
                return c == null ? null : c.getDeclaredConstructor(types);
            } catch (NoSuchMethodException absent) {
                return null;
            }
        } else {
            while (f == null && c != null) {
                try {
                    f = c.getDeclaredMethod(method, types);
                } catch (NoSuchMethodException e2) {
                    c = c.getSuperclass();
                }
            }
        }
        return f;
    }

    public static void setFinalField(Class<?> owner, String name, @Nullable Object instance, @Nullable Object value) throws Exception {
        Field field = getProtectedInheritedField(owner, name);
        if (field == null) throw new NoSuchFieldException(owner.getName() + "." + name);
        setFinalField(field, instance, value);
    }

    /**
     * Uses Java 25's supported reflective write for ordinary instance fields. Final
     * fields are only suitable for reconstruction before publication. Static final,
     * record and hidden-class final fields require a different design/access transformer.
     * No JVM internals or modifier-bit mutation is involved.
     */
    @SuppressWarnings("deprecation") // isAccessible is the API for preserving the override flag itself.
    public static void setFinalField(Field field, @Nullable Object instance, @Nullable Object value) throws Exception {
        Objects.requireNonNull(field, "field");
        if (Modifier.isFinal(field.getModifiers()) && (Modifier.isStatic(field.getModifiers())
                || field.getDeclaringClass().isRecord() || field.getDeclaringClass().isHidden()))
            throw new IllegalAccessException("Java 25 cannot reflectively write " + field + "; use a supported API or an access transformer");
        boolean override = field.isAccessible();
        try {
            field.setAccessible(true);
            field.set(Modifier.isStatic(field.getModifiers()) ? null : instance, value);
        } finally {
            field.setAccessible(override);
        }
    }

    public static Collection<Field> getFields(Class<?> c, @Nullable FieldSelector sel) {
        Collection<Field> li = new ArrayList<>();
        while (c != null) {
            Field[] fd = c.getDeclaredFields();
            for (Field f : fd) {
                if (sel == null || sel.isValid(f))
                    li.add(f);
            }
            c = c.getSuperclass();
        }
        return li;
    }

    public static Collection<Method> getMethods(Class<?> c, @Nullable MethodSelector sel) {
        Collection<Method> li = new ArrayList<>();
        while (c != null) {
            Method[] fd = c.getDeclaredMethods();
            for (Method f : fd) {
                if (sel == null || sel.isValid(f))
                    li.add(f);
            }
            c = c.getSuperclass();
        }
        return li;
    }

    public interface FieldSelector {
        boolean isValid(Field f);
    }

    public interface MethodSelector {
        boolean isValid(Method m);
    }

    public record TypeSelector(Class<?> type) implements FieldSelector {

        @Override
            public boolean isValid(Field f) {
                return f.getType() == type;
            }
        }

    public static boolean checkForField(Class<?> c, String name, int... modifiers) {
        try {
            Field f = c.getDeclaredField(name);
            for (int mod : modifiers) {
                if ((f.getModifiers() & mod) == 0) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkForMethod(Class<?> c, String name, Class<?>[] args, int... modifiers) {
        try {
            Method f = c.getDeclaredMethod(name, args);
            for (int mod : modifiers) {
                if ((f.getModifiers() & mod) == 0) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convenience lookup using exact runtime argument types, as in the original API.
     * Use the explicit-signature overload for primitive, interface or nullable arguments.
     * Failures retain the original null result and are logged with their cause.
     */
    public static @Nullable Object cacheAndInvokeMethod(String className, String name, @Nullable Object receiver, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < types.length; i++)
                types[i] = Objects.requireNonNull(args[i], "Nullable arguments need an explicit method signature").getClass();
            return cacheAndInvokeMethod(Class.forName(className), name, types, receiver, args);
        } catch (ReflectiveOperationException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not resolve method {}.{}", className, name, error);
            return null;
        }
    }

    /** Exact parameter types distinguish overloads, including primitive and nullable arguments. */
    public static @Nullable Object cacheAndInvokeMethod(Class<?> owner, String name, Class<?>[] parameterTypes,
            @Nullable Object receiver, @Nullable Object... args) {
        try {
            var signature = new MethodSignature(name, List.of(parameterTypes));
            var methods = methodCache.get(owner);
            Method method = methods.get(signature);
            if (method == null) {
                Method candidate = owner.getDeclaredMethod(name, parameterTypes);
                candidate.setAccessible(true);
                Method previous = methods.putIfAbsent(signature, candidate);
                method = previous != null ? previous : candidate;
            }
            return method.invoke(receiver, args);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Throwable cause = error instanceof java.lang.reflect.InvocationTargetException invocation
                    ? invocation.getCause() : error;
            DragonAPI.LOGGER.error("Could not invoke method {}.{} with signature {}", owner.getName(), name,
                    java.util.Arrays.toString(parameterTypes), cause);
            return null;
        }
    }
}
