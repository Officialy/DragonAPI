/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Instantiable.Data.Collections;

import java.util.ArrayList;
import java.util.Collection;

public class ImmutableList<E> extends ArrayList<E> {

	@Override
	public final E remove(int o) {
		throw new UnsupportedOperationException("You cannot remove entries from this list!");
	}

	@Override
	public final boolean remove(Object o) {
		throw new UnsupportedOperationException("You cannot remove entries from this list!");
	}

	@Override
	public final boolean removeAll(Collection c)  {
		throw new UnsupportedOperationException("You cannot remove entries from this list!");
	}

	@Override
	public final E set(int index, E element) {
		throw new UnsupportedOperationException("You cannot overwrite entries in this list!");
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException("You cannot overwrite entries in this list!");
	}
}