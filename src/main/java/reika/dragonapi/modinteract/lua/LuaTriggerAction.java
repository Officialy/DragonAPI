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
import reika.dragonapi.interfaces.blockentity.TriggerableAction;

public class LuaTriggerAction extends LuaMethod {

	public LuaTriggerAction() {
		super("trigger", TriggerableAction.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{((TriggerableAction)te).trigger()};
	}

	@Override
	public String getDocumentation() {
		return "Triggers the block to attempt to perform its action.\nArgs: None\nReturns: Success true/false";
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
