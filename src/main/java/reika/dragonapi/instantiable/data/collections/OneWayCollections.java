/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.collections;


import java.util.*;

public class OneWayCollections {

	public static final class OneWayList<E> extends ArrayList<E> {

		@Override
		public E remove(int o) {
			throw new UnsupportedOperationException("You cannot remove entries from this list!");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("You cannot remove entries from this list!");
		}

		@Override
		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException("You cannot remove entries from this list!");
		}

		@Override
		public E set(int index, E element) {
			throw new UnsupportedOperationException("You cannot overwrite entries in this list!");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("You cannot clear this list!");
		}

		@Override
		public Iterator<E> iterator() {
			return new WrapperIterator(super.iterator());
		}
	}

	public static final class OneWaySet<E> extends HashSet<E> {

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("You cannot remove entries from this set!");
		}

		@Override
		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException("You cannot remove entries from this set!");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("You cannot clear this set!");
		}

		@Override
		public Iterator<E> iterator() {
			return new WrapperIterator(super.iterator());
		}
	}

	public static final class OneWayMap<K, V> extends HashMap<K, V> {

		@Override
		public V remove(Object obj) {
			throw new UnsupportedOperationException("You cannot remove entries from this map!");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("You cannot clear this map!");
		}

		@Override
		public V put(K key, V value) {
			if (this.containsKey(key)) {
				throw new UnsupportedOperationException("You cannot overwrite entries in this map!");
			} else {
				return super.put(key, value);
			}
		}

		@Override
		public Set<K> keySet() {
			return Collections.unmodifiableSet(super.keySet());
		}

		@Override
		public Collection<V> values() {
			return Collections.unmodifiableCollection(super.values());
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet() {
			return new WrapperEntrySet(super.entrySet());
		}

	}

	private static final class WrapperEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {

		private final Set<Map.Entry<K, V>> wrapped;

		private WrapperEntrySet(Set<Map.Entry<K, V>> set) {
			wrapped = set;
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return wrapped.iterator();
		}

		@Override
		public boolean contains(Object o) {
			return wrapped.contains(o);
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("You cannot remove entries from this map with its entry set!");
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("You cannot clear this map entry set!");
		}

	}

	private static final class WrapperIterator<E> implements Iterator<E> {

		private final Iterator<E> wrapped;

		private WrapperIterator(Iterator<E> wrap) {
			wrapped = wrap;
		}

		public void remove() {
			throw new UnsupportedOperationException("You cannot remove entries from this collection with an iterator!");
		}

		@Override
		public boolean hasNext() {
			return wrapped.hasNext();
		}

		@Override
		public E next() {
			return wrapped.next();
		}

	}

}
