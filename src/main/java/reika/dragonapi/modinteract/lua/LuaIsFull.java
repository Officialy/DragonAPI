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
import reika.dragonapi.libraries.ReikaInventoryHelper;

public class LuaIsFull extends LuaMethod {

	public LuaIsFull() {
		super("isFull", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		Container ii = (Container)te;
		return new Object[]{ReikaInventoryHelper.isFull(ii)};
	}

	@Override
	public String getDocumentation() {
		return "Checks if an inventory is completely full.\nArgs: None\nReturns: true/false";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
