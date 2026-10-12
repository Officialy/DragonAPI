package reika.dragonapi.instantiable.worldgen;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

/** Finite job origins, separate from the support chunks a population job needs to load. */
public final class PopulationScope {
    private final boolean rectangle;
    private final int minX, minZ, maxX, maxZ;
    private final Map<Long, BitSet> regions = new HashMap<>();
    private long observed;

    private PopulationScope(boolean rectangle, int minX, int minZ, int maxX, int maxZ) {
        this.rectangle = rectangle;
        this.minX = minX; this.minZ = minZ; this.maxX = maxX; this.maxZ = maxZ;
    }

    public static PopulationScope rectangle(int minX, int minZ, int maxX, int maxZ) {
        if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("Inverted chunk bounds");
        return new PopulationScope(true, minX, minZ, maxX, maxZ);
    }

    public static PopulationScope observedChunks() { return new PopulationScope(false, 0, 0, 0, 0); }

    public void observe(ChunkPos chunk) {
        if (rectangle) throw new IllegalStateException("A rectangular scope has fixed bounds");
        BitSet cells = regions.computeIfAbsent(ChunkPos.pack(chunk.x() >> 5, chunk.z() >> 5), k -> new BitSet(1024));
        int index = (chunk.z() & 31) * 32 + (chunk.x() & 31);
        if (!cells.get(index)) { cells.set(index); observed++; }
    }

    public boolean contains(ChunkPos chunk) {
        if (rectangle) return chunk.x() >= minX && chunk.x() <= maxX && chunk.z() >= minZ && chunk.z() <= maxZ;
        BitSet cells = regions.get(ChunkPos.pack(chunk.x() >> 5, chunk.z() >> 5));
        return cells != null && cells.get((chunk.z() & 31) * 32 + (chunk.x() & 31));
    }

    public long size() { return rectangle ? ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1) : observed; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Rectangle", rectangle);
        tag.putInt("MinX", minX); tag.putInt("MinZ", minZ); tag.putInt("MaxX", maxX); tag.putInt("MaxZ", maxZ);
        ListTag list = new ListTag();
        regions.forEach((position, cells) -> {
            CompoundTag record = new CompoundTag();
            record.putLong("Region", position); record.putLongArray("Cells", cells.toLongArray()); list.add(record);
        });
        tag.put("Regions", list);
        return tag;
    }

    public static PopulationScope load(CompoundTag tag) {
        PopulationScope scope = new PopulationScope(tag.getBooleanOr("Rectangle", false),
                tag.getIntOr("MinX", 0), tag.getIntOr("MinZ", 0), tag.getIntOr("MaxX", 0), tag.getIntOr("MaxZ", 0));
        for (var entry : tag.getListOrEmpty("Regions")) if (entry instanceof CompoundTag record) {
            BitSet cells = BitSet.valueOf(record.getLongArray("Cells").orElse(new long[0]));
            if (cells.length() > 1024) throw new IllegalArgumentException("Population region exceeds 32x32 chunks");
            scope.regions.put(record.getLongOr("Region", 0), cells);
        }
        scope.observed = scope.regions.values().stream().mapToLong(BitSet::cardinality).sum();
        if (scope.rectangle && (scope.minX > scope.maxX || scope.minZ > scope.maxZ))
            throw new IllegalArgumentException("Inverted saved population bounds");
        return scope;
    }
}
