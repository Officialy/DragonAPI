package reika.dragonapi.instantiable.data.maps;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import static org.junit.jupiter.api.Assertions.*;

class TileEntityCacheTest {
    private static WorldLocation location(int x) { return new WorldLocation(Level.OVERWORLD,x,5,0); }
    private record Entry(WorldLocation getLocation) implements TileEntityCache.LocationEntry {}

    @Test void runtimeTokenRejectsWrongTypesBeforeMutation() {
        var cache = new TileEntityCache<>(String.class);
        cache.put(location(0), "valid");
        TileEntityCache raw = cache;
        assertThrows(ClassCastException.class, () -> raw.put(location(1),123));
        assertEquals(1,cache.size()); assertEquals("valid",cache.get(location(0)));
        var other = new TileEntityCache<String>();
        other.put(location(2),"second"); ((TileEntityCache)other).put(location(3),123);
        assertThrows(ClassCastException.class, () -> cache.putAll(other));
        assertEquals(1,cache.size());
    }
    @Test void interfacesAndLocationEntriesRemainUsableWithoutBlockEntityInheritance() {
        var cache = new TileEntityCache<>(TileEntityCache.LocationEntry.class);
        var entry = new Entry(location(0)); cache.put(entry);
        assertSame(entry,cache.get(location(0))); assertSame(entry,cache.remove(entry));
        assertTrue(cache.isEmpty());
    }
    @Test void untypedMapCannotPretendToValidateDeserializedBlockEntities() {
        var cache = new TileEntityCache<String>(); cache.put(location(0),"keep");
        assertThrows(IllegalStateException.class, () -> cache.readFromNBT(new CompoundTag()));
        assertEquals("keep",cache.get(location(0)));
        var typed = new TileEntityCache<>(String.class);
        var tag=new CompoundTag();cache.writeToNBT(tag);typed.readFromNBT(tag);
        assertTrue(typed.isEmpty()); // the dimension is not running in this unit fixture
    }
    @Test void chunkAndNearLookupsDoNotRequireAResolvedLevel() {
        var cache = new TileEntityCache<>(String.class);
        cache.put(location(-1),"negative");cache.put(location(16),"next");
        assertEquals("negative",cache.getChunkData(location(-1)).get(location(-1)));
        assertEquals(1,cache.getAllValuesNear(location(0),1).size());
        assertThrows(UnsupportedOperationException.class, () -> cache.values().clear());
    }
}
