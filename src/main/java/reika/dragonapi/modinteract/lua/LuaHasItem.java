/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.lua;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.libraries.ReikaInventoryHelper;

public class LuaHasItem extends LuaMethod {

	public LuaHasItem() {
		super("hasItem", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		Container ii = (Container) te;
		boolean flag = false;
		switch (args.length) {
			/* todo case 1 -> {
				Item itemID = Item.byId(((Double) args[0]).intValue());
				flag = ReikaInventoryHelper.checkForItem(itemID, ii);
			}
			case 2 -> {
				Item itemID = Item.byId(((Double) args[0]).intValue());
				flag = ReikaInventoryHelper.checkForItemStack(itemID, ii);
			}*/
			case 3 -> {
				Item itemID = Item.byId(((Double) args[0]).intValue());
				int size = ((Double) args[2]).intValue();
				ItemStack is = new ItemStack(itemID, size);
				flag = ReikaInventoryHelper.checkForItemStack(is, ii, true);
			}
			default -> throw new IllegalArgumentException("Invalid ItemStack!");
		}
		return new Object[]{flag};
	}

	@Override
	public String getDocumentation() {
		return "Checks for the item in an inventory.\nArgs: ID, metadata (optional), getCount() (optional)\nReturns: true/false";
	}

	@Override
	public String getArgsAsString() {
		return "int id, int meta*, int size*";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
