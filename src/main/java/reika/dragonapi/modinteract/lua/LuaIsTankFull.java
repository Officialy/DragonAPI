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

<<<<<<< Updated upstream:ModInteract/Lua/LuaIsTankFull.java
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
=======
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fluids.IFluidTank;
import net.neoforged.fluids.capability.IFluidHandler;
>>>>>>> Stashed changes:src/main/java/reika/dragonapi/modinteract/lua/LuaIsTankFull.java

public class LuaIsTankFull extends LuaMethod {

	public LuaIsTankFull() {
		super("isTankFull", IFluidHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		IFluidTank ifl = (IFluidTank)te;
		if (ifl.getFluid() == null)
			return new Object[]{false};
		return new Object[]{ifl.getFluidAmount() >= ifl.getCapacity()};
	}

	@Override
	public String getDocumentation() {
		return "Checks if a tank is full.\nArgs: Tank Index\nReturns: true/false";
	}

	@Override
	public String getArgsAsString() {
		return "int tankIndex";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
