/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event;

import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class TileEntityEvent extends Event {

	private final BlockEntity tile;

	public TileEntityEvent(BlockEntity te) {
		tile = te;
	}

	public final Level getWorld() {
		return tile.getLevel();
	}

	public final int getTileX() {
		return tile.getBlockPos().getX();
	}

	public final int getTileY() {
		return tile.getBlockPos().getY();
	}

	public final int getTileZ() {
		return tile.getBlockPos().getZ();
	}

	public final boolean isTileInventory() {
		return tile instanceof Container;
	}

	public final boolean isTileFluidHandler() {
		return tile instanceof IFluidHandler;
	}

	protected final BlockEntity getTile() {
		return tile;
	}

}
