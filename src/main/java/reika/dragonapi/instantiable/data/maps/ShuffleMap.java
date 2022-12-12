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

import reika.dragonapi.exception.MisuseException;

import java.util.ArrayList;
import java.util.Collections;


public class ShuffleMap {

	private final ArrayList<Integer> keys = new ArrayList<>();

	public void addEntry(int val) {
		if (keys.contains(val))
			throw new MisuseException("You cannot shuffle duplicate entries!");
		keys.add(val);
	}

	public void shuffle() {
		Collections.shuffle(keys);
	}

	public int getShuffledIndex(int val) {
		return keys.get(val);
	}

	@Override
	public String toString() {
		return keys.toString();
	}

}
