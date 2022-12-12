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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collection;
import java.util.EnumSet;

public interface OreType extends RegistryType {

	OreRarity getRarity();

	String getProductOreDictName();

	Collection<ItemStack> getAllOreBlocks();

	ItemStack getFirstOreBlock();

	EnumSet<OreLocation> getOreLocations();

	boolean canGenerateIn(Block b);

	int getDropCount();

	int ordinal();

	String name();

	int getDisplayColor();

	String getDisplayName();

	enum OreRarity {
		EVERYWHERE("Large and very common veins", "Copper and Fluorite"), //Copper, Fluorite
		COMMON("Larger sized and common veins", "Tin and Redstone"), //Tin, Redstone
		AVERAGE("Average sized veins of average rarity", "Iron"), //Iron
		SCATTERED("Average sized but rarer veins", "Gold and Calcite"), //Gold, Calcite
		SCARCE("Veins are smaller and often hard to find", "Lapis and Diamond"), //Lapis, Diamond
		RARE("Generally a single block or two per chunk", "Emerald and Platinum"); //Emerald, Platinum

		public static final OreRarity[] list = values();
		public final String desc;
		public final String examples;

		OreRarity(String d, String e) {
			desc = d;
			examples = e;
		}
	}

	enum OreLocation {
		OVERWORLD(Blocks.STONE),
		NETHER(Blocks.NETHERRACK),
		END(Blocks.END_STONE),
		OTHER(null);

		public static final OreLocation[] list = values();
		public final Block genBlock;

		OreLocation(Block b) {
			genBlock = b;
		}
	}

}
