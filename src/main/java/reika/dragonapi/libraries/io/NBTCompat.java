package reika.dragonapi.libraries.io;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Temporary compatibility layer for NBT API differences between MC/NeoForge versions
 * that may return Optional values instead of primitives/tags.
 */
public final class NBTCompat {

    private NBTCompat() {}

    private static Object callGetter(CompoundTag tag, String method, String key) throws Exception {
        Method m = tag.getClass().getMethod(method, String.class);
        return m.invoke(tag, key);
    }

    private static int toInt(Object ret, int def) {
        if (ret instanceof Integer i) return i;
        if (ret instanceof Optional<?> opt) {
            Object v = opt.isPresent() ? opt.get() : null;
            return v instanceof Integer i ? i : def;
        }
        return def;
    }

    private static long toLong(Object ret, long def) {
        if (ret instanceof Long l) return l;
        if (ret instanceof Optional<?> opt) {
            Object v = opt.isPresent() ? opt.get() : null;
            return v instanceof Long l ? l : def;
        }
        return def;
    }

    private static double toDouble(Object ret, double def) {
        if (ret instanceof Double d) return d;
        if (ret instanceof Optional<?> opt) {
            Object v = opt.isPresent() ? opt.get() : null;
            return v instanceof Double d ? d : def;
        }
        return def;
    }

    private static boolean toBoolean(Object ret, boolean def) {
        if (ret instanceof Boolean b) return b;
        if (ret instanceof Optional<?> opt) {
            Object v = opt.isPresent() ? opt.get() : null;
            return v instanceof Boolean b ? b : def;
        }
        return def;
    }

    private static CompoundTag toCompound(Object ret) {
        if (ret instanceof CompoundTag ct) return ct;
        if (ret instanceof Optional<?> opt) {
            Object v = opt.isPresent() ? opt.get() : null;
            return v instanceof CompoundTag ct ? ct : new CompoundTag();
        }
        return new CompoundTag();
    }

    public static int getInt(CompoundTag tag, String key, int defaultValue) {
        try {
            Object ret = callGetter(tag, "getInt", key);
            return toInt(ret, defaultValue);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static long getLong(CompoundTag tag, String key, long defaultValue) {
        try {
            Object ret = callGetter(tag, "getLong", key);
            return toLong(ret, defaultValue);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static double getDouble(CompoundTag tag, String key, double defaultValue) {
        try {
            Object ret = callGetter(tag, "getDouble", key);
            return toDouble(ret, defaultValue);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static String getString(CompoundTag tag, String key, String defaultValue) {
        try {
            Object ret = callGetter(tag, "getString", key);
            if (ret instanceof String s) return s;
            if (ret instanceof Optional<?> opt) {
                Object v = opt.isPresent() ? opt.get() : null;
                return v instanceof String s ? s : defaultValue;
            }
        } catch (Throwable ignored) {
        }
        return defaultValue;
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean defaultValue) {
        try {
            Object ret = callGetter(tag, "getBoolean", key);
            return toBoolean(ret, defaultValue);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        try {
            Object ret = callGetter(tag, "getCompound", key);
            return toCompound(ret);
        } catch (Throwable t) {
            return new CompoundTag();
        }
    }

    /** Convenience: safely get a CompoundTag from a ListTag index where API may vary. */
    public static CompoundTag getListCompound(net.minecraft.nbt.ListTag list, int index) {
        try {
            Object ret = list.getCompound(index);
            if (ret instanceof CompoundTag ct) return ct;
        } catch (Throwable ignored) {
        }
        try {
            Object elem = list.get(index);
            return elem instanceof CompoundTag ct ? ct : new CompoundTag();
        } catch (Throwable ignored) {
        }
        return new CompoundTag();
    }
}


