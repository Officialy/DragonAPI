package reika.dragonapi.libraries.io;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NBTCompatTest {
    @Test
    void typedReadsRetainFallbacksAndNestedData() {
        var tag = new CompoundTag();
        tag.putInt("number", 7);
        tag.putString("text", "saved");
        var child = new CompoundTag();
        child.putBoolean("flag", true);
        tag.put("child", child);
        assertEquals(7, NBTCompat.getInt(tag, "number", -1));
        assertEquals(-1, NBTCompat.getInt(tag, "text", -1));
        assertEquals("default", NBTCompat.getString(tag, "missing", "default"));
        assertTrue(NBTCompat.getBoolean(NBTCompat.getCompound(tag, "child"), "flag", false));
        assertTrue(NBTCompat.getCompound(tag, "text").isEmpty());
    }

    @Test
    void compoundListReadsHandleMixedTagsAndOutOfBounds() {
        var list = new ListTag();
        list.add(StringTag.valueOf("wrong-type"));
        var compound = new CompoundTag();
        compound.putLong("value", 123L);
        list.add(compound);
        assertTrue(NBTCompat.getListCompound(list, 0).isEmpty());
        assertEquals(123L, NBTCompat.getLong(NBTCompat.getListCompound(list, 1), "value", -1));
        assertTrue(NBTCompat.getListCompound(list, -1).isEmpty());
        assertTrue(NBTCompat.getListCompound(list, 9).isEmpty());
    }
}
