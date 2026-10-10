package reika.dragonapi.instantiable.data.immutable;

import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import static org.junit.jupiter.api.Assertions.*;

class WorldLocationTest {
    private static WorldLocation location() { return new WorldLocation(ResourceKey.create(Registries.DIMENSION,Identifier.parse("dragonapi:not_loaded")),-17,9,31); }
    @Test void unloadedLocationCoordinatesRemainPureValues() {
        var loc=location();assertNull(loc.getWorld());
        assertEquals(-2,loc.getChunk().chunk().x());assertEquals(1,loc.getChunk().chunk().z());
        assertEquals(loc.getDimension(),loc.getChunk().dimension());
        assertEquals(new BlockPos(-16,11,28),loc.move(1,2,-3).pos);
        assertEquals(loc.getDimension(),loc.move(1,2,-3).getDimension());
        assertEquals(new BlockPos(-17,0,31),loc.to2D().pos);assertEquals(loc,loc.copy());
    }
    @Test void unresolvedReadHelpersUseTheirDocumentedSentinels() {
        var loc=location();assertNull(loc.getBlock());assertNull(loc.getBlockKey());assertNull(loc.getBlockEntity());
        assertFalse(loc.canSeeTheSky());assertFalse(loc.isChunkLoaded());assertFalse(loc.isEmpty());
        assertFalse(loc.isWithinSquare(null,-17,9,31,1));
    }
    @Test void nativeAndOriginalNumericDimensionDataRoundTrip() {
        var loc=location();assertEquals(loc,WorldLocation.readTag(loc.writeToTag()));
        assertEquals(loc,WorldLocation.fromSerialString(loc.toSerialString()));
        for(int id : new int[]{-1,0,1,7}) {
            var tag=new CompoundTag();tag.putInt("dim",id);tag.putInt("x",-17);tag.putInt("y",9);tag.putInt("z",31);
            assertEquals(WorldChunk.dimensionKeyFromLegacyId(id),WorldLocation.readTag(tag).getDimension());
        }
        var tag=loc.writeToTag();tag.putString("dim","INVALID DIM");
        assertThrows(IllegalArgumentException.class, () -> WorldLocation.readTag(tag));
    }
    @Test void orderingKeepsHashCollidingDimensionsAndCoordinatesDistinct() {
        var a=new WorldLocation(Level.OVERWORLD,0,1,0);var b=new WorldLocation(Level.OVERWORLD,31,0,0);
        assertEquals(a.hashCode(),b.hashCode());assertNotEquals(0,a.compareTo(b));
        assertEquals(2,new java.util.TreeSet<>(java.util.List.of(a,b)).size());
        assertEquals(0,a.compareTo(a.copy()));
    }
    @Test void locationDoesNotRetainAnyLevelInstance() {
        assertTrue(java.util.Arrays.stream(WorldLocation.class.getDeclaredFields()).noneMatch(field -> Level.class.isAssignableFrom(field.getType())));
    }
}
