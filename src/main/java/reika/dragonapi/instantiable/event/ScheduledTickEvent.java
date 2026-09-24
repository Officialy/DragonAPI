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

import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

import reika.dragonapi.instantiable.data.maps.TimerMap.FreezableTimer;
import reika.dragonapi.instantiable.event.scheduledtickevent.DelayableSchedulableEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.ScheduledEvent;

/**
 * One delayed action queued on {@link reika.dragonapi.auxiliary.trackers.TickScheduler}; posted to the event
 * bus as it fires, so other mods can observe it.
 *
 * <p>The 1.7.10 class nested its stock actions ({@code DelayedAttack}, {@code DelayedKnockback},
 * {@code ScheduledSoundEvent}, ...); they are top-level classes in {@code scheduledtickevent} now.
 */
public final class ScheduledTickEvent extends Event implements FreezableTimer {

	private final ScheduledEvent action;

	public ScheduledTickEvent(ScheduledEvent evt) {
		action = evt;
	}

	public ScheduledEvent getAction() {
		return action;
	}

	public void fire() {
		NeoForge.EVENT_BUS.post(this);
		action.fire();
	}

	public boolean runOnSide(LogicalSide s) {
		return action.runOnSide(s);
	}

	@Override
	public boolean isFrozen() {
		return action instanceof DelayableSchedulableEvent d && !d.canTick();
	}

	/**
	 * 1.7.10 re-checked the effective side here. The scheduler keeps one timer per logical side and files an
	 * event only under the sides it runs on, and each timer ticks on its own side's thread, so that check
	 * always passed.
	 */
	@Override
	public void call() {
		this.fire();
	}

}
