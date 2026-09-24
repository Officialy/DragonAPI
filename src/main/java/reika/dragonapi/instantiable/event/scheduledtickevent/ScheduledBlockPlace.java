/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.scheduledtickevent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.LogicalSide;

import reika.dragonapi.APIPacketHandler.PacketIDs;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

public class ScheduledBlockPlace implements ScheduledEvent {

	private final BlockState block;
	private final WorldLocation location;

	public ScheduledBlockPlace(Level world, BlockPos pos, Block b) {
		this(world, pos, b.defaultBlockState());
	}

	/** 1.7.10's (Block, meta) pair is a BlockState in 26.2. */
	public ScheduledBlockPlace(Level world, BlockPos pos, BlockState state) {
		block = state;
		location = new WorldLocation(world, pos);
	}

	@Override
	public void fire() {
		location.setBlock(block);
		Level world = location.getWorld();
		BlockPos pos = location.pos;
		ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, PacketIDs.BREAKPARTICLES.ordinal(), world, pos, 128, Block.getId(block), 0);
		ReikaSoundHelper.playBreakSound(world, pos, block.getBlock());
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
