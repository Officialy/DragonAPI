package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class WorldChunk {

    public int dimensionID;
    public ChunkPos chunk;

    public WorldChunk(Level world, LevelChunk ch) {
        this(world, ch.getPos());
    }

    public WorldChunk(Level world, ChunkPos ch) {

    }

    public WorldChunk(Level world, int x, int z) {
        this(world, new ChunkPos(x, z));
    }

    public WorldChunk(int dim, int x, int z) {
        this(dim, new ChunkPos(x, z));
    }

    public WorldChunk(int dim, ChunkPos ch) {
        dimensionID = dim;
        chunk = ch;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WorldChunk c) {
            return c.dimensionID == dimensionID && c.chunk.equals(chunk);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return chunk.hashCode()^dimensionID;
    }

    @Override
    public String toString() {
        return "Chunk "+chunk.toString()+" in DIM"+dimensionID;
    }

    public static WorldChunk fromSerialString(String sg) {
        String[] parts = sg.split(",");
        return new WorldChunk(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public String toSerialString() {
        return dimensionID+","+chunk.x+","+chunk.z;
    }

    public CompoundTag writeToTag() {
        CompoundTag ret = new CompoundTag();
        ret.putInt("dimension", dimensionID);
        ret.putInt("x()", chunk.x);
        ret.putInt("zCoord", chunk.z);
        return ret;
    }

    public static WorldChunk readFromTag(CompoundTag tag) {
        return new WorldChunk(tag.getInt("dimension"), tag.getInt("xCoord"), tag.getInt("zCoord"));
    }

}
