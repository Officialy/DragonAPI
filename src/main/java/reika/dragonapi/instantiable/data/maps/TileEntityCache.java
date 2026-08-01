/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.WorldChunk;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.MultiMap.CollectionType;

/** Location-keyed cache of live block entities, preserving the V33a location and near-chunk API. */
public final class TileEntityCache<V> {

	private final Map<WorldLocation, V> data = new LinkedHashMap<>();

	public V put(Level level, int x, int y, int z, V value) {
		return put(new WorldLocation(level, x, y, z), value);
	}

	public V put(BlockEntity tile, V value) {
		return put(new WorldLocation(tile), value);
	}

	public V put(WorldLocation location, V value) {
		return data.put(location, value);
	}

	public V put(V value) {
		if (value instanceof LocationEntry entry) {
			return put(entry.getLocation(), value);
		}
		if (!(value instanceof BlockEntity tile)) {
			throw new IllegalArgumentException("TileEntityCache self-put entries must be BlockEntities or LocationEntries");
		}
		return put(new WorldLocation(tile), value);
	}

	public void putAll(TileEntityCache<V> other) {
		data.putAll(other.data);
	}

	public V get(Level level, int x, int y, int z) {
		return get(new WorldLocation(level, x, y, z));
	}

	public V get(BlockEntity tile) {
		return tile != null ? get(new WorldLocation(tile)) : null;
	}

	public V get(WorldLocation location) {
		return data.get(location);
	}

	public boolean containsKey(Level level, int x, int y, int z) {
		return containsKey(new WorldLocation(level, x, y, z));
	}

	public boolean containsKey(BlockEntity tile) {
		return tile != null && containsKey(new WorldLocation(tile));
	}

	public boolean containsKey(WorldLocation location) {
		return data.containsKey(location);
	}

	public boolean containsValue(V value) {
		return data.containsValue(value);
	}

	public V remove(Level level, int x, int y, int z) {
		return remove(new WorldLocation(level, x, y, z));
	}

	public V remove(BlockEntity tile) {
		return tile != null ? remove(new WorldLocation(tile)) : null;
	}

	public V remove(WorldLocation location) {
		return data.remove(location);
	}

	public V remove(V value) {
		if (value instanceof LocationEntry entry) {
			return remove(entry.getLocation());
		}
		if (!(value instanceof BlockEntity tile)) {
			throw new IllegalArgumentException("TileEntityCache self-remove entries must be BlockEntities or LocationEntries");
		}
		return remove(new WorldLocation(tile));
	}

	public Collection<WorldLocation> keySet() {
		return Collections.unmodifiableSet(data.keySet());
	}

	public Collection<V> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	public void clear() {
		data.clear();
	}

	public void removeWorld(Level level) {
		data.keySet().removeIf(location -> location.getDimension().equals(level.dimension()));
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public int size() {
		return data.size();
	}

	@Override
	public String toString() {
		return data.toString();
	}

	public void writeToNBT(CompoundTag tag) {
		ListTag locations = new ListTag();
		for (WorldLocation location : data.keySet()) {
			locations.add(location.writeToTag());
		}
		tag.put("locs", locations);
	}

	public void readFromNBT(CompoundTag tag) {
		readFromNBT(tag, null);
	}

	/** Resolve entries against the owning level when reading client synchronization data. */
	public void readFromNBT(CompoundTag tag, Level level) {
		data.clear();
		ListTag locations = tag.getList("locs").orElse(new ListTag());
		for (Tag value : locations) {
			if (!(value instanceof CompoundTag entry)) {
				continue;
			}
			WorldLocation location = WorldLocation.readTag(entry);
			BlockEntity tile = location.getBlockEntity(level != null ? level : location.getWorld());
			try {
				@SuppressWarnings("unchecked")
				V cast = (V) tile;
				if (cast != null) {
					data.put(location, cast);
				}
			} catch (ClassCastException ex) {
				DragonAPI.LOGGER.error("Tried to load a TileEntityCache from invalid NBT at {}", location, ex);
			}
		}
	}

	public MultiMap<V, WorldLocation> invert(CollectionType collectionType) {
		MultiMap<V, WorldLocation> inverted = new MultiMap<>(collectionType);
		data.forEach((location, value) -> inverted.addValue(value, location));
		return inverted;
	}

	public MultiMap<V, WorldLocation> invert() {
		return invert(CollectionType.LIST);
	}

	/**
	 * Returns entries in every chunk intersected by the radius. As in V33a, callers must perform an
	 * exact distance check when they require a spherical result.
	 */
	public Collection<WorldLocation> getAllLocationsNear(WorldLocation center, double radius) {
		Collection<WorldLocation> result = new HashSet<>();
		for (WorldLocation location : data.keySet()) {
			if (isInIntersectingChunk(center, location, radius)) {
				result.add(location);
			}
		}
		return result;
	}

	/** Same intersecting-chunk semantics as {@link #getAllLocationsNear}. */
	public Collection<V> getAllValuesNear(WorldLocation center, double radius) {
		Collection<V> result = new HashSet<>();
		for (Map.Entry<WorldLocation, V> entry : data.entrySet()) {
			if (isInIntersectingChunk(center, entry.getKey(), radius)) {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	private static boolean isInIntersectingChunk(WorldLocation center, WorldLocation candidate, double radius) {
		if (!center.getDimension().equals(candidate.getDimension())) {
			return false;
		}
		int minChunkX = (int)Math.floor((center.pos.getX() - radius) / 16D);
		int maxChunkX = (int)Math.floor((center.pos.getX() + radius) / 16D);
		int minChunkZ = (int)Math.floor((center.pos.getZ() - radius) / 16D);
		int maxChunkZ = (int)Math.floor((center.pos.getZ() + radius) / 16D);
		int chunkX = candidate.pos.getX() >> 4;
		int chunkZ = candidate.pos.getZ() >> 4;
		return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
	}

	public Map<WorldLocation, V> getChunkData(WorldChunk chunk) {
		Map<WorldLocation, V> result = new LinkedHashMap<>();
		for (Map.Entry<WorldLocation, V> entry : data.entrySet()) {
			if (entry.getKey().getChunk().equals(chunk)) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	public Map<WorldLocation, V> getChunkData(WorldLocation location) {
		return getChunkData(location.getChunk());
	}

	public WorldLocation getRandomEntry(Random random) {
		if (data.isEmpty()) {
			return null;
		}
		return new ArrayList<>(data.keySet()).get(random.nextInt(data.size()));
	}

	public interface LocationEntry {
		WorldLocation getLocation();
	}
}
