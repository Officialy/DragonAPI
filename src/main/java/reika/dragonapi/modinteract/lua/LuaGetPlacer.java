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
		if (args.length != 0) throw new LuaMethodException("getPlacer takes no arguments");
		String name = tile.getPlacerName();
		var uuid = tile.getPlacerID();
		return new Object[]{name == null || name.isEmpty() ? null : name, uuid == null ? null : uuid.toString()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the player who placed the machine.\nArgs: None\nReturns: [Name, UUID string]; absent information is nil, offline placers remain available";
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
