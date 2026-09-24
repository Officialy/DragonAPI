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

/** A scheduled event whose countdown pauses while {@link #canTick()} is false. */
public interface DelayableSchedulableEvent extends ScheduledEvent {

	boolean canTick();

}
