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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;


public class LuaGetSlot extends LuaMethod {

	public LuaGetSlot() {
		super("getSlot", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		int slot = ((Double)args[0]).intValue();
		Container ii = (Container)te;
		ItemStack is = ii.getItem(slot);
		if (is == null)
			return null;
		Object[] o = new Object[4];
		o[0] = is.getItem().toString();
		o[1] = is.getCount();
		o[2] = is.getDisplayName();
		o[3] = is.getTag() != null ? is.getTag().toString() : null;
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns the inventory slot contents.\nArgs: None\nReturns: \"Empty\" if empty, otherwise [itemID, getCount(), displayName]";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
