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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;


public class PlayerTimer {

	private final PlayerMap<Integer> data = new PlayerMap();

	public final void tick(Level world) {
		if (!data.isEmpty()) {
			Iterator<Entry<UUID, Integer>> it = data.iterator();
			while (it.hasNext()) {
				Entry<UUID, Integer> e = it.next();
				UUID uid = e.getKey();
				Player ep = world.getPlayerByUUID(uid);
				if (ep != null && this.shouldTickPlayer(ep)) {
					int time = e.getValue();
					if (time > 1) {
						e.setValue(time - 1);
					} else {
						it.remove();
					}
				}
			}
		}
	}

	protected boolean shouldTickPlayer(Player ep) {
		return true;
	}

	public final void clear() {
		data.clear();
	}

	public final boolean containsKey(Player ep) {
		return data.containsKey(ep);
	}

	public final int get(Player ep) {
		Integer get = data.get(ep);
		return get != null ? get.intValue() : 0;
	}

	public final void put(Player ep, int time) {
		data.put(ep, time);
	}

}
