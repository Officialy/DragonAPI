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

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.LogicalSide;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

public final class ScheduledPacket implements ScheduledEvent {

	public final String channel;
	public final int packetID;
	private final PacketTarget target;
	private final int[] data;

	public ScheduledPacket(String ch, int id, Level world, BlockPos pos, int r, int... data) {
		this(ch, id, new PacketTarget.RadiusTarget(new WorldLocation(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), r), data);
	}

	public ScheduledPacket(String ch, int id, PacketTarget pt, int... data) {
		channel = ch;
		packetID = id;
		target = pt;
		this.data = data;
	}

	@Override
	public void fire() {
		ReikaPacketHelper.sendDataPacket(channel, packetID, target, Arrays.stream(data).boxed().toList());
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
