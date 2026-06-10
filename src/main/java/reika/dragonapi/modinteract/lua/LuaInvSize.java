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
import net.minecraft.world.level.block.entity.BlockEntity;


public class LuaInvSize extends LuaMethod {

	public LuaInvSize() {
		super("getSizeInv", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{((Container)te).getContainerSize()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the inventory size.\nArgs: None\nReturns: Inventory Size";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INTEGER;
	}

}
