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


import net.minecraft.world.level.block.entity.BlockEntity;

public class LuaGetCoords extends LuaMethod {

	public LuaGetCoords() {
		super("getCoords", BlockEntity.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the BlockEntity coordinates.\nArgs: None\nReturns: [x,y,z]";
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
