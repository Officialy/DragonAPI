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



import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.modinteract.lua.LibraryLuaMethod;


public class LuaGetWorldTime extends LibraryLuaMethod {

	public LuaGetWorldTime() {
		super("getGameTime");
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[] {te.getLevel().getGameTime()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the world time.\nReturns: Time";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.LONG;
	}

}
