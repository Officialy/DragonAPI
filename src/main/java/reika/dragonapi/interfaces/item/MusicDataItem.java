/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.item;

import reika.dragonapi.instantiable.MusicScore;
import net.minecraft.world.item.ItemStack;

public interface MusicDataItem {

	MusicScore getMusic(ItemStack is);
	//public int[][][] getNoteblockMusic(ItemStack is);

}
