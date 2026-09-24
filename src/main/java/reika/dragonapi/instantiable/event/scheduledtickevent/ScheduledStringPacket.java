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

import net.neoforged.fml.LogicalSide;

import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

public final class ScheduledStringPacket implements ScheduledEvent {

	public final String channel;
	public final int packetID;
	private final PacketTarget target;
	private final String data;

	public ScheduledStringPacket(String ch, int id, PacketTarget pt, String data) {
		channel = ch;
		packetID = id;
		target = pt;
		this.data = data;
	}

	@Override
	public void fire() {
		ReikaPacketHelper.sendStringPacket(channel, packetID, data, target);
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
