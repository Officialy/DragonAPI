/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

public class AlphabeticItemComparator implements Comparator<ItemStack> {

	@Override
	public int compare(ItemStack o1, ItemStack o2) {
		return String.CASE_INSENSITIVE_ORDER.compare(o1.getDisplayName().toString(), o2.getDisplayName().toString());
	}

}
