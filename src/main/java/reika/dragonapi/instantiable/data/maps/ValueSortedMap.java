/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.maps;


import java.util.*;
import java.util.Map.Entry;


public class ValueSortedMap<K, V> {

	private final LinkedHashMap<K, V> raw;
	private final LinkedHashMap<K, V> data;

	private Comparator<V> comparator;

	public ValueSortedMap() {
		raw = new LinkedHashMap<>();
		data = new LinkedHashMap<>();
	}

	public ValueSortedMap(Map<K, V> data) {
		this();
		this.putAll(data);
	}

	public ValueSortedMap<K, V> setComparator(Comparator<V> c) {
		comparator = c;
		this.rebuildData();
		return this;
	}

	public int size() {
		return raw.size();
	}

	public boolean isEmpty() {
		return raw.isEmpty();
	}

	public boolean containsKey(K key) {
		return raw.containsKey(key);
	}

	public boolean containsValue(V value) {
		return raw.containsValue(value);
	}

	public V get(K key) {
		return raw.get(key);
	}

	public V put(K key, V value) {
		V ret = raw.put(key, value);
		this.rebuildData();
		return ret;
	}

	public V remove(K key) {
		V ret = raw.remove(key);
		this.rebuildData();
		return ret;
	}

	public void putAll(Map<K, V> m) {
		raw.putAll(m);
		this.rebuildData();
	}

	private void rebuildData() {
		data.clear();
		List<Entry<K, V>> entries = new ArrayList<>(raw.entrySet());
		entries.sort((a, b) -> this.compareValues(a.getValue(), b.getValue()));
		for (Entry<K, V> entry : entries)
			data.put(entry.getKey(), entry.getValue());
	}

	public void clear() {
		raw.clear();
		data.clear();
	}

	public Set<K> keySet() {
		return Collections.unmodifiableSet(data.keySet());
	}

	public Collection<V> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableMap(data).entrySet();
	}

	public K getFirstKey() {
		return this.isEmpty() ? null : data.entrySet().iterator().next().getKey();
	}

	public V getFirst() {
		return this.isEmpty() ? null : data.entrySet().iterator().next().getValue();
	}

	@Override
	public String toString() {
		return raw.toString();
	}

	@SuppressWarnings("unchecked")
	private int compareValues(V a, V b) {
		return comparator != null ? comparator.compare(a, b) : ((Comparable<? super V>) a).compareTo(b);
	}

}
