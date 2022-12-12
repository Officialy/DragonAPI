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


import java.util.*;
import java.util.Map.Entry;


public class TimerMap<V> {

	private final Map<V, Integer> timer;

	public TimerMap() {
		this(new HashMap());
	}

	public TimerMap(Map<V, Integer> map) {
		timer = map;
	}

	public void put(V val, int time) {
		this.timer.put(val, time);
	}

	public void putAll(Map<V, Integer> map) {
		this.timer.putAll(map);
	}

	public void clear() {
		timer.clear();
	}

	public void tick() {
		this.tick(1);
	}

	public void tick(int amt) {
		if (!timer.isEmpty()) {
			Iterator<Entry<V, Integer>> it = timer.entrySet().iterator();
			while (it.hasNext()) {
				Entry<V, Integer> e = it.next();
				V key = e.getKey();
				if (key instanceof FreezableTimer) {
					if (((FreezableTimer) key).isFrozen()) {
						continue;
					}
				}

				if (e.getValue() >= amt) {
					e.setValue(e.getValue() - amt);
				} else {
					if (key instanceof TimerCallback) {
						((TimerCallback) key).call();
					}
					it.remove();
				}
			}
		}
	}

	public boolean containsKey(V val) {
		return timer.containsKey(val);
	}

	public boolean isEmpty() {
		return timer.isEmpty();
	}

	public Collection<V> keySet() {
		return Collections.unmodifiableCollection(timer.keySet());
	}

	public int get(V val) {
		return timer.get(val);
	}

	@Override
	public final String toString() {
		return timer.toString();
	}

	public final int size() {
		return timer.size();
	}

	public Map<V, Integer> toMap() {
		return Collections.unmodifiableMap(timer);
	}

	public interface TimerCallback {

		void call();

	}

	public interface FreezableTimer extends TimerCallback {

		boolean isFrozen();

	}

}
