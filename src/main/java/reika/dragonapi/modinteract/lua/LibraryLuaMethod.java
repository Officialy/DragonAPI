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

public abstract class LibraryLuaMethod extends LuaMethod {

	public LibraryLuaMethod(String name) {
		super(name, BlockEntity.class);
	}

	public final boolean isDocumented() {
		return false;
	}

}
