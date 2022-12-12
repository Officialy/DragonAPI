/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;


public interface CustomBiomeDistributionWorld {

    Biome getBiomeID(Level world, int x, int z);

}
