package reika.dragonapi.instantiable.data.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class TierMap<V> {

	private final HashMap<V, Integer> data = new HashMap();
	private final MultiMap<Integer, V> tiers = new MultiMap(MultiMap.CollectionType.HASHSET);
	private int maxTier;

	public TierMap() {

	}

	public void addObject(V val, int tier) {
		this.data.put(val, tier);
		this.tiers.addValue(tier, val);
		this.maxTier = Math.max(this.maxTier, tier);
	}

	public int getTier(V val) {
		Integer ret = data.get(val);
		return ret != null ? ret.intValue() : -1;
	}

	public Set<V> getByTier(int tier) {
		return Collections.unmodifiableSet((Set<V>) tiers.get(tier));
	}

	public int getMaxTier() {
		return maxTier;
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public void clear() {
		data.clear();
		this.tiers.clear();
		this.maxTier = -1;
	}

	public boolean containsKey(V val) {
		return data.containsKey(val);
	}

}
