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

/** An action {@link reika.dragonapi.auxiliary.trackers.TickScheduler} runs after a delay. */
public interface ScheduledEvent {

	void fire();

	boolean runOnSide(LogicalSide s);

}
