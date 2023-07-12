/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

import java.util.ArrayList;
import java.util.Collection;

public final class RAMPair {

	private final Collection<Object> objects = new ArrayList<>();
	private int hash = 0;

	public RAMPair(Object... o) {
		for (Object value : o) {
			this.addObject(value);
		}
	}

	public RAMPair(Collection<Object> c) {
		for (Object o : c) {
			this.addObject(o);
		}
	}

	private void addObject(Object o) {
		if (!objects.contains(o)) {
			objects.add(o);
			hash += o.hashCode();
		}
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RAMPair r) {
			if (objects.size() != r.objects.size())
				return false;
			for (Object obj : objects) {
				if (!r.objects.contains(obj))
					return false;
			}
			return true;
		}
		return false;
	}

}
