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

import java.util.ArrayList;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;


public class LuaPrintInv extends LuaMethod {

	public LuaPrintInv() {
		super("printInv", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		ArrayList<String> li = new ArrayList<>();
		Container ii = (Container) te;
		for (int i = 0; i < ii.getContainerSize(); i++) {
			ItemStack is = ii.getItem(i);
			String name = is != null ? is.toString() : "Empty";
			li.add(name);
		}
		return li.toArray();
	}

	@Override
	public String getDocumentation() {
		return "Prints an entire inventory.\nArgs: None\nReturns: List of ItemStack.toString()";
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
