package reika.dragonapi.libraries.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.IDConflictException;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.data.maps.PluralMap;
import reika.dragonapi.interfaces.registry.RegistrationList;
import reika.dragonapi.libraries.io.ReikaChatHelper;

public class ReikaReflectionHelper {

    private static final PluralMap<Method> methodCache = new PluralMap<>(2);

    /**
     * Gets the value of a private int in an instance of obj.
     */
    public static int getPrivateInteger(Object obj, String field) {
        try {
            Class<? extends Object> c = obj.getClass();
            Field f = null;
            while (f == null && c != null) {
                try {
                    f = c.getDeclaredField(field);
                } catch (NoSuchFieldException e2) {
                    c = c.getSuperclass();
                }
            }
            if (f == null) {
                if (DragonOptions.DEBUGLOG.getState()) {
                    DragonAPI.LOGGER.error("Could not find field " + field + " in " + obj);
                    ReikaChatHelper.write("Could not find field " + field + " in " + obj);
                }
                throw new NoSuchFieldException();
            }
            int val = Integer.MIN_VALUE;
            if (!f.canAccess(f)) {
                f.setAccessible(true);
                val = f.getInt(obj);
                f.setAccessible(false);
            } else
                val = f.getInt(obj);
            return val;
        } catch (NoSuchFieldException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Could not find field " + field + " in " + obj);
                ReikaChatHelper.write("Could not find field " + field + " in " + obj);
            }
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Could not access field " + field + " in " + obj);
                ReikaChatHelper.write("Could not access field " + field + " in " + obj);
            }
            e.printStackTrace();
        } catch (SecurityException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Security Manager locked field " + field + " in " + obj);
                ReikaChatHelper.write("Security Manager locked field " + field + " in " + obj);
            }
            e.printStackTrace();
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Gets the value of a private boolean in an instance of obj.
     */
    public static boolean getPrivateBoolean(Object obj, String field) {
        try {
            Class<?> c = obj.getClass();
            Field f = null;
            while (f == null && c != null) {
                try {
                    f = c.getDeclaredField(field);
                } catch (NoSuchFieldException e2) {
                    c = c.getSuperclass();
                }
            }
            if (f == null) {
                if (DragonOptions.DEBUGLOG.getState()) {
                    DragonAPI.LOGGER.error("Could not find field " + field + " in " + obj);
                    ReikaChatHelper.write("Could not find field " + field + " in " + obj);
                }
                throw new NoSuchFieldException();
            }
            boolean val = false;
            if (!f.canAccess(f)) { //todo test this, was f.isAccessible() before
                f.setAccessible(true);
                val = f.getBoolean(obj);
                f.setAccessible(false);
            } else
                val = f.getBoolean(obj);
            return val;
        } catch (NoSuchFieldException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Could not find field " + field + " in " + obj);
                ReikaChatHelper.write("Could not find field " + field + " in " + obj);
            }
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Could not access field " + field + " in " + obj);
                ReikaChatHelper.write("Could not access field " + field + " in " + obj);
            }
            e.printStackTrace();
        } catch (SecurityException e) {
            if (DragonOptions.DEBUGLOG.getState()) {
                DragonAPI.LOGGER.error("Security Manager locked field " + field + " in " + obj);
                ReikaChatHelper.write("Security Manager locked field " + field + " in " + obj);
            }
            e.printStackTrace();
        }
        return false;
    }

    public static Field getProtectedInheritedField(Object o, String field) {
        return getProtectedInheritedField(o.getClass(), field);
    }

    /**
     * Gets a nonvisible field that may be inherited by any of the superclasses. Returns null if none exists.
     */
    public static Field getProtectedInheritedField(Class<?> c, String field) {
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

    public static Executable getProtectedInheritedMethod(Object o, String method, Class<?>... types) {
        return getProtectedInheritedMethod(o.getClass(), method, types);
    }

    /**
     * Gets a nonvisible Method that may be inherited by any of the superclasses. Returns null if none exists.
     */
    public static Executable getProtectedInheritedMethod(Class<?> c, String method, Class<?>... types) {
        Executable f = null;
        if (method.equals("<init>")) {
            while (f == null && c != null) {
                try {
                    f = c.getDeclaredConstructor(types);
                } catch (NoSuchMethodException e2) {
                    c = c.getSuperclass();
                }
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

    public static void setFinalField(Class<?> c, String s, Object instance, Object o) throws Exception {
        setFinalField(getProtectedInheritedField(c, s), instance, o);
    }

    public static void setFinalField(Field f, Object instance, Object o) throws Exception {
        try {
            f.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.set(instance, o);
        } catch (IllegalAccessException e) {
            if (e.toString().contains("Can not set final")) {
                throw new RuntimeException("You need to un-final a field BEFORE any accesses (ie f.get() calls)!", e);
            } else {
                throw e;
            }
        }
    }

    public static Collection<Field> getFields(Class<?> c, FieldSelector sel) {
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

    public static Collection<Method> getMethods(Class<?> c, MethodSelector sel) {
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

    public static final class TypeSelector implements FieldSelector {

        public final Class<?> type;

        public TypeSelector(Class<?> c) {
            type = c;
        }

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
     * Note that this does not support overloading, as to do so would compromise performance.
     */
    public static Object cacheAndInvokeMethod(String cl, String name, Object ref, Object... args) {
        try {
            Method m = methodCache.get(cl, name);
            if (m == null) {
                try {
                    Class<?> c = Class.forName(cl);
                    m = c.getDeclaredMethod(cl, getArgTypesFromArgs(args));
                    m.setAccessible(true);
                    methodCache.put(m, cl, name);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return m.invoke(ref, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Class<?>[] getArgTypesFromArgs(Object[] args) {
        Class<?>[] arr = new Class[args.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = args[i].getClass();
        }
        return arr;
    }

}
