package reika.dragonapi.instantiable.io;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LuaBlockTest {
    private static LuaBlock.NBTLuaBlock block(CompoundTag tag, boolean parse) {
        return new LuaBlock.NBTLuaBlock("nbt", null, new LuaBlock.LuaBlockDatabase(), tag, parse);
    }

    @Test void legacyNumericIdsAndUnknownModsKeepOriginalDataAndReadableLevels() {
        var tag = new CompoundTag(); var entries = new ListTag();
        var known = new CompoundTag(); known.putShort("id", (short)16); known.putShort("lvl", (short)3); entries.add(known);
        var unknown = new CompoundTag(); unknown.putInt("id", 199); unknown.putInt("lvl", 7); entries.add(unknown);
        tag.put("ench", entries); tag.putString("owner", "retained"); var original = tag.copy();
        var result = block(tag, true).asHashMap();
        assertEquals(List.of("minecraft:sharpness (level 3)", "Unknown legacy enchantment 199 (level 7)"), result.get("Enchantments"));
        assertEquals(2, ((List<?>)result.get("ench")).size()); assertEquals("retained", result.get("owner"));
        assertEquals(original, tag, "Export must never modify the caller's NBT");
    }

    @Test void storedAppliedMalformedAndNamespacedEntriesAreNotDiscarded() {
        var tag = new CompoundTag(); var applied = new ListTag(); var stored = new ListTag();
        var named = new CompoundTag(); named.putString("id", "example:enchanted"); named.putInt("lvl", 4); applied.add(named);
        var malformed = new CompoundTag(); malformed.putString("id", "old:missing_level"); applied.add(malformed);
        var numeric = new CompoundTag(); numeric.putShort("id", (short)34); numeric.putShort("lvl", (short)2); stored.add(numeric);
        tag.put("ench", applied); tag.put("StoredEnchantments", stored);
        var result = block(tag, true).asHashMap();
        assertEquals(List.of("minecraft:unbreaking (level 2)", "example:enchanted (level 4)"), result.get("Enchantments"));
        assertEquals(2, ((List<?>)result.get("ench")).size()); assertEquals(1, ((List<?>)result.get("StoredEnchantments")).size());
        assertFalse(block(tag, false).asHashMap().containsKey("Enchantments"));
    }

    @Test void modernComponentMapsRetainIdsLevelsAndHistoricalLevelsWrapper() {
        var tag = new CompoundTag(); var components = new CompoundTag();
        var modern = new CompoundTag(); modern.putInt("minecraft:sharpness", 5);
        var historical = new CompoundTag(); var levels = new CompoundTag(); levels.putInt("example:custom", 8);
        historical.put("levels", levels); historical.putBoolean("show_in_tooltip", false);
        components.put("minecraft:enchantments", modern); components.put("minecraft:stored_enchantments", historical);
        tag.put("components", components);
        var result = block(tag, true).asHashMap();
        assertEquals(List.of("minecraft:sharpness (level 5)", "example:custom (level 8)"), result.get("Enchantments"));
        assertEquals(Map.of("minecraft:enchantments", Map.of("minecraft:sharpness", 5),
                "minecraft:stored_enchantments", Map.of("levels", Map.of("example:custom", 8), "show_in_tooltip", 0)), result.get("components"));
    }

    @Test void writtenContainersPreserveEmptySingletonAndMixedListTypesAndOrdering() {
        var b = block(new CompoundTag(), false);
        var list = List.of("first", Map.of("id", "example:custom", "lvl", 3), "last");
        b.writeData(Map.of("empty_map", Map.of(), "empty_list", List.of(), "singleton", List.of("one"), "mixed", list));
        assertEquals(Map.of("empty_map", Map.of(), "empty_list", List.of(), "singleton", List.of("one"), "mixed", list), b.asHashMap());
        var text = String.join("\n", b.getChild("mixed").writeToStrings());
        assertTrue(text.indexOf("\"first\"") < text.indexOf("example:custom")
                && text.indexOf("example:custom") < text.indexOf("\"last\""));
        assertFalse(text.contains("1 = {"), "List records must be anonymous entries, not numeric map fields");
    }

    @Test void existingEnchantmentsFieldIsRetainedBesideTheDerivedDisplay() {
        var tag = new CompoundTag(); var list = new ListTag(); var entry = new CompoundTag();
        entry.putString("id", "minecraft:power"); entry.putInt("lvl", 2); list.add(entry); tag.put("Enchantments", list);
        var result = block(tag, true).asHashMap();
        assertEquals(List.of(Map.of("id", "minecraft:power", "lvl", 2)), result.get("Enchantments"));
        assertEquals(List.of("minecraft:power (level 2)"), result.get("EnchantmentDisplay"));
    }
}
