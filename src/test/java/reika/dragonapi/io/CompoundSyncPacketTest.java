package reika.dragonapi.io;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.io.SyncPacket;

class CompoundSyncPacketTest {
    private static CompoundTag state(int number, boolean recipe) {
        CompoundTag tag = new CompoundTag(); tag.putInt("number", number);
        if (recipe) tag.putString("recipe", "active");
        CompoundTag nested = new CompoundTag(); nested.putInt("nested", number); tag.put("details", nested);
        return tag;
    }
    @Test void differencesIncludeChangedFieldsAndRemoveAbsentKeys() {
        var patch = CompoundSyncPacket.difference(state(1,true),state(2,false),false);
        assertEquals(2,patch.getIntOr("number",0));
        assertEquals("recipe",patch.getListOrEmpty(SyncPacket.REMOVED_KEYS).getFirst().asString().orElseThrow());
        assertFalse(patch.contains("recipe"));
    }
    @Test void unchangedStateIsEmptyAndForcedStateIncludesAllCurrentFields() {
        var tag=state(1,true);
        assertTrue(CompoundSyncPacket.difference(tag,tag.copy(),false).isEmpty());
        assertEquals(tag,CompoundSyncPacket.difference(tag,tag.copy(),true));
    }
    @Test void mergingPreservesUnsentFieldsAndAppliesRemovals() {
        var tag=state(1,true); tag.putString("unsent","kept");
        CompoundSyncPacket.merge(tag,CompoundSyncPacket.difference(state(1,true),state(2,false),false));
        assertFalse(tag.contains("recipe")); assertEquals("kept",tag.getStringOr("unsent",""));
        assertEquals(2,tag.getIntOr("number",0)); assertFalse(tag.contains(SyncPacket.REMOVED_KEYS));
    }
    @Test void changedNestedTagsAreDetachedInBothDirections() {
        var before=state(1,true); var after=state(2,true);
        var patch=CompoundSyncPacket.difference(before,after,false);
        after.getCompoundOrEmpty("details").putInt("nested",99);
        assertEquals(2,patch.getCompoundOrEmpty("details").getIntOr("nested",0));
        var client=before.copy(); CompoundSyncPacket.merge(client,patch);
        patch.getCompoundOrEmpty("details").putInt("nested",77);
        assertEquals(2,client.getCompoundOrEmpty("details").getIntOr("nested",0));
    }
    @Test void coalescedChangesAreRelativeToTheConsumedBaseline() {
        var before=state(1,true); var first=state(2,true); var latest=state(2,false);
        var patch=CompoundSyncPacket.difference(before,latest,false);
        assertEquals(2,patch.getIntOr("number",0));
        assertTrue(patch.contains(SyncPacket.REMOVED_KEYS));
        assertFalse(CompoundSyncPacket.difference(first,latest,false).contains("number"));
        assertTrue(CompoundSyncPacket.difference(before,before.copy(),false).isEmpty());
    }
    @Test void deletionAndReplacementApplyInDeclaredOrder() {
        CompoundTag patch=new CompoundTag(); ListTag removed=new ListTag(); removed.add(StringTag.valueOf("value"));
        patch.put(SyncPacket.REMOVED_KEYS,removed); patch.putInt("value",9);
        CompoundTag client=new CompoundTag(); client.putInt("value",1); CompoundSyncPacket.merge(client,patch);
        assertEquals(9,client.getIntOr("value",0));
    }
}
