/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import java.util.EnumSet;

import net.neoforged.fml.LogicalSide;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.TickRegistry.Phase;
import reika.dragonapi.auxiliary.trackers.TickRegistry.TickHandler;
import reika.dragonapi.auxiliary.trackers.TickRegistry.TickType;
import reika.dragonapi.instantiable.data.maps.TimerMap;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;

/** Runs {@link ScheduledTickEvent}s a number of ticks after they are queued, on the side(s) they belong to. */
public class TickScheduler implements TickHandler {

	public static final TickScheduler instance = new TickScheduler();

	private final TimerMap<ScheduledTickEvent> serverData = new TimerMap<>();
	private final TimerMap<ScheduledTickEvent> clientData = new TimerMap<>();
	private static final Object lock = new Object();

	private TickScheduler() {

	}

	@Override
	public void tick(TickType type, Object... tickData) {
		synchronized(lock) {
			this.getData(type).tick();
		}
	}

	private TimerMap<ScheduledTickEvent> getData(TickType type) {
		return type == TickType.SERVER ? serverData : clientData;
	}

	public void clear() {
		synchronized(lock) {
			serverData.clear();
			clientData.clear();
		}
	}

	@Override
	public EnumSet<TickType> getType() {
		return EnumSet.of(TickType.SERVER, TickType.CLIENT);
	}

	@Override
	public boolean canFire(Phase p) {
		return p == Phase.START;
	}

	@Override
	public String getLabel() {
		return "Scheduled Tick Handler";
	}

	public void scheduleEvent(ScheduledTickEvent evt, int ticks) {
		if (ticks <= 0) {
			DragonAPI.LOGGER.error("Something tried scheduling a delayed event with zero delay!");
			Thread.dumpStack();
			return;
		}
		synchronized(lock) {
			if (evt.runOnSide(LogicalSide.SERVER))
				serverData.put(evt, ticks);
			if (evt.runOnSide(LogicalSide.CLIENT))
				clientData.put(evt, ticks);
		}
	}

}
