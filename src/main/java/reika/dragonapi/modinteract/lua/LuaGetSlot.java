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

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;


public class LuaGetSlot extends LuaMethod {

	public LuaGetSlot() {
		super("getSlot", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		if (args.length != 1) throw new LuaMethodException("Expected one zero-based inventory slot");
		int slot = LuaArguments.integer(args[0], "slot", 0, ((Container)te).getContainerSize() - 1);
		Container ii = (Container)te;
		ItemStack is = ii.getItem(slot);
		if (is.isEmpty())
			return new Object[]{"Empty"};
		Object[] o = new Object[4];
		o[0] = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(is.getItem()).toString();
		o[1] = is.getCount();
		o[2] = is.getDisplayName().getString();
		CustomData cd = is.get(DataComponents.CUSTOM_DATA);
		o[3] = cd != null ? cd.copyTag().toString() : null;
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns the inventory slot contents.\nArgs: zero-based slot\nReturns: \"Empty\" if empty, otherwise [registry name, count, displayName, custom data or nil]";
	}

	@Override
	public String getArgsAsString() {
		return "int slot";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
