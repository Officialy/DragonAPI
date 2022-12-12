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


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.libraries.ReikaFluidHelper;
import reika.dragonapi.modinteract.lua.LibraryLuaMethod;


public class LuaFluidColor extends LibraryLuaMethod {

	public LuaFluidColor() {
		super("fluidColor");
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		String name = (String)args[0];
		Fluid f = ForgeRegistries.FLUIDS.getValue(ResourceLocation.tryParse(name));
		if (f == null)
			throw new IllegalArgumentException("No such fluid with name '"+name+"'.");
		return new Object[]{ReikaFluidHelper.getFluidColor(f)};
	}

	@Override
	public String getDocumentation() {
		return "Returns fluid color.\nArgs: fluidName\nReturns: Fluid color";
	}

	@Override
	public String getArgsAsString() {
		return "String fluidName";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INTEGER;
	}

}
