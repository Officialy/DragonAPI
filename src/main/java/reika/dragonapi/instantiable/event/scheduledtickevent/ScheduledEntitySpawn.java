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

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.LogicalSide;

public class ScheduledEntitySpawn<E extends Entity> implements ScheduledEvent {

	protected final E entity;

	public ScheduledEntitySpawn(E e) {
		entity = e;
	}

	@Override
	public void fire() {
		entity.level().addFreshEntity(entity);
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
