/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * This is an interface for ENUMS!
 */
public interface BlockEnum extends RegistryEntry {

    Block getBlock();

    Item getItem();

}
