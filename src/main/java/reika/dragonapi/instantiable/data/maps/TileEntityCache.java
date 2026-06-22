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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/** Location-keyed cache of live tile entities (ReactorCraft control-rod layout, etc.). */
public final class TileEntityCache<V> {

	private final Map<WorldLocation, V> data = new LinkedHashMap<>();

	public V put(V tile) {
		if (!(tile instanceof BlockEntity te))
			throw new IllegalArgumentException("TileEntityCache entries must be BlockEntities");
		WorldLocation loc = new WorldLocation(te);
		data.put(loc, tile);
		return tile;
	}

	public V get(WorldLocation c) {
		return data.get(c);
	}

	public V remove(V tile) {
		if (!(tile instanceof BlockEntity te))
			return null;
		return data.remove(new WorldLocation(te));
	}

	public void clear() {
		data.clear();
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public int size() {
		return data.size();
	}

	public Collection<V> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	public Set<WorldLocation> keySet() {
		return Collections.unmodifiableSet(data.keySet());
	}

	public void writeToNBT(CompoundTag tag) {
		ListTag li = new ListTag();
		for (WorldLocation loc : data.keySet()) {
			CompoundTag entry = new CompoundTag();
			loc.writeToTag(entry);
			li.add(entry);
		}
		tag.put("locs", li);
	}

	public void readFromNBT(CompoundTag tag) {
		data.clear();
		ListTag li = tag.getList("locs").orElse(new ListTag());
		for (Tag o : li) {
			if (!(o instanceof CompoundTag entry))
				continue;
			WorldLocation loc = WorldLocation.readTag(entry);
			BlockEntity te = loc.getBlockEntity(loc.getWorld());
			try {
				@SuppressWarnings("unchecked")
				V v = (V) te;
				if (v != null)
					data.put(loc, v);
			} catch (ClassCastException e) {
				DragonAPI.LOGGER.error("Tried to load a TileEntityCache from invalid NBT!");
			}
		}
	}
}
