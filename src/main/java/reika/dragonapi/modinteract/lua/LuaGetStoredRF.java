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




<<<<<<< Updated upstream:ModInteract/Lua/LuaGetStoredRF.java
@ModTileDependent(value = {"cofh.api.energy.IEnergyProvider", "cofh.api.energy.IEnergyReceiver"})
=======
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.energy.IEnergyStorage;

>>>>>>> Stashed changes:src/main/java/reika/dragonapi/modinteract/lua/LuaGetStoredRF.java
public class LuaGetStoredRF extends LuaMethod {

	public LuaGetStoredRF() {
		super("getStoredRF", IEnergyStorage.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{((IEnergyStorage)te).getEnergyStored()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the stored RF value.\nArgs: Side (compass)\nReturns: Energy";
	}

	@Override
	public String getArgsAsString() {
		return "String dir";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INTEGER;
	}

}
