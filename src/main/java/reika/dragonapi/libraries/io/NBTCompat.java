package reika.dragonapi.libraries.io;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Typed 26.3 NBT access retaining the shared helper's fallback contract. */
public final class NBTCompat {
    private NBTCompat() {}
    public static int getInt(CompoundTag tag, String key, int fallback) { return tag.getIntOr(key, fallback); }
    public static long getLong(CompoundTag tag, String key, long fallback) { return tag.getLongOr(key, fallback); }
    public static double getDouble(CompoundTag tag, String key, double fallback) { return tag.getDoubleOr(key, fallback); }
    public static String getString(CompoundTag tag, String key, String fallback) { return tag.getStringOr(key, fallback); }
    public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) { return tag.getBooleanOr(key, fallback); }
    public static CompoundTag getCompound(CompoundTag tag, String key) { return tag.getCompoundOrEmpty(key); }
    public static CompoundTag getListCompound(ListTag list, int index) {
        return index >= 0 && index < list.size() ? list.getCompoundOrEmpty(index) : new CompoundTag();
    }
}
