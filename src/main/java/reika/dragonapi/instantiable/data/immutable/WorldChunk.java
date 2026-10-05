package reika.dragonapi.instantiable.data.immutable;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import reika.dragonapi.libraries.io.NBTCompat;

/**
 * Immutable chunk coordinate paired with its 26.2 dimension key.
 */
public record WorldChunk(ResourceKey<Level> dimension, ChunkPos chunk) {

    public WorldChunk(Level world, LevelChunk chunk) {
        this(world, chunk.getPos());
    }

    public WorldChunk(Level world, ChunkPos chunk) {
        this(Objects.requireNonNull(world, "world").dimension(), chunk);
    }

    public WorldChunk(Level world, int x, int z) {
        this(world, new ChunkPos(x, z));
    }

    public WorldChunk(ResourceKey<Level> dimension, int x, int z) {
        this(dimension, new ChunkPos(x, z));
    }

    public WorldChunk(ResourceKey<Level> dimension, ChunkPos chunk) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.chunk = Objects.requireNonNull(chunk, "chunk");
    }

    /**
     * Compatibility reader for V33a's three vanilla numeric dimension ids.
     */
    @Deprecated
    public WorldChunk(int dimensionId, int x, int z) {
        this(dimensionKeyFromLegacyId(dimensionId), x, z);
    }

    /**
     * Compatibility reader for V33a's three vanilla numeric dimension ids.
     */
    @Deprecated
    public WorldChunk(int dimensionId, ChunkPos chunk) {
        this(dimensionKeyFromLegacyId(dimensionId), chunk);
    }

    public static ResourceKey<Level> dimensionKeyFromLegacyId(int dimensionId) {
        return switch (dimensionId) {
            case -1 -> Level.NETHER;
            case 0 -> Level.OVERWORLD;
            case 1 -> Level.END;
            default -> ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("legacy", "dimension_" + dimensionId));
        };
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof WorldChunk other
                && dimension.equals(other.dimension)
                && chunk.equals(other.chunk);
    }

    @Override
    public String toString() {
        return "Chunk " + chunk + " in " + dimension.identifier();
    }

    public static WorldChunk fromSerialString(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid world-chunk string: " + value);
        }
        ResourceKey<Level> dimension;
        Identifier id = Identifier.tryParse(parts[0]);
        if (id != null && parts[0].contains(":")) {
            dimension = ResourceKey.create(Registries.DIMENSION, id);
        } else {
            dimension = dimensionKeyFromLegacyId(Integer.parseInt(parts[0]));
        }
        return new WorldChunk(dimension, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public String toSerialString() {
        return dimension.identifier() + "," + chunk.x() + "," + chunk.z();
    }

    public CompoundTag writeToTag() {
        CompoundTag result = new CompoundTag();
        result.putString("dimension", dimension.identifier().toString());
        result.putInt("x", chunk.x());
        result.putInt("z", chunk.z());
        return result;
    }

    public static WorldChunk readFromTag(CompoundTag tag) {
        String dimensionName = NBTCompat.getString(tag, "dimension", "");
        ResourceKey<Level> dimension;
        if (!dimensionName.isEmpty()) {
            dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionName));
        } else {
            dimension = dimensionKeyFromLegacyId(NBTCompat.getInt(tag, "dimension", 0));
        }
        int x = tag.contains("x") ? NBTCompat.getInt(tag, "x", 0) : NBTCompat.getInt(tag, "xCoord", 0);
        int z = tag.contains("z") ? NBTCompat.getInt(tag, "z", 0) : NBTCompat.getInt(tag, "zCoord", 0);
        return new WorldChunk(dimension, x, z);
    }
}
