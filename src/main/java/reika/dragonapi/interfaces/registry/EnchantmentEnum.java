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

import net.minecraft.world.item.enchantment.Enchantment;

public interface EnchantmentEnum extends RegistryEntry {

    Enchantment getEnchantment();

}
