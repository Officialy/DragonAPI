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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.base.BlockEntityBase;


public class LuaGetPlacer extends LuaMethod {

	public LuaGetPlacer() {
		super("getPlacer", BlockEntityBase.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		BlockEntityBase tile = (BlockEntityBase)te;
		Player ep = tile.getPlacer();
		return new Object[]{ep.getName(), ep.getUUID()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the player who placed the machine.\nArgs: None\nReturns: [Name, UUID]";
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
