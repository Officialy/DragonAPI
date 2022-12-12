/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.lua.library;



import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.modinteract.lua.LibraryLuaMethod;


public class LuaGetBlock extends LibraryLuaMethod {

	public LuaGetBlock() {
		super("getBlock");
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		int x = ((Double)args[0]).intValue();
		int y = ((Double)args[1]).intValue();
		int z = ((Double)args[2]).intValue();
		return new Object[]{te.getLevel().getBlockState(new BlockPos(x, y, z)).getBlock().toString()};
	}

	@Override
	public String getDocumentation() {
		return "Returns block at position.\nArgs: x, y, z\nReturns: {Block Name}";
	}

	@Override
	public String getArgsAsString() {
		return "int x, int y, int z";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
