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

import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

public class ScheduledBlockBreak implements ScheduledEvent {

	private final WorldLocation location;

	public ScheduledBlockBreak(WorldLocation loc) {
		location = loc;
	}

	@Override
	public void fire() {
		ReikaWorldHelper.dropAndDestroyBlockAt(location.getWorld(), location.pos, null, true, true);
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
