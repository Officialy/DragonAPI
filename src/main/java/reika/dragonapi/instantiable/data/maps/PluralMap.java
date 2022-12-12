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


import com.google.common.collect.Sets;

import java.util.*;

/**
 * A multi-key HashMap.
 */
public class PluralMap<V> extends HashMap {

	public final int keySize;
	private boolean bidi = false;

	public PluralMap(int size) {
		keySize = size;
	}

	public PluralMap<V> setBidirectional() {
		this.bidi = true;
		return this;
	}

	public V put(V value, Object... key) {
		if (key.length != keySize)
			throw new IllegalArgumentException("Invalid key length!");
		return (V) super.put(this.toList(key), value);
	}

	public V get(Object... key) {
		if (key.length != keySize)
			throw new IllegalArgumentException("Invalid key length!");
		return (V) super.get(this.toList(key));
	}

	public boolean containsKeyV(Object... key) {
		if (key.length != keySize)
			throw new IllegalArgumentException("Invalid key length!");
		return super.containsKey(this.toList(key));
	}

	private Collection<Object> toList(Object[] key) {
		/*
		List<Object> li = new ArrayList<>();
		for (int i = 0; i < this.keySize; i++) {
			li.add(key[i]);
		}
		 */
		return bidi ? Sets.newHashSet(key) : Arrays.asList(key);//li;
	}

	public V remove(Object... key) {
		if (key.length != keySize)
			throw new IllegalArgumentException("Invalid key length!");
		return (V) super.remove(this.toList(key));
	}

	public Collection<List<Object>> pluralKeySet() {
		return Collections.unmodifiableCollection(this.keySet());
	}

}
