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

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.LogicalSide;

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.ReikaEntityHelper;

public class DelayedKnockback implements ScheduledEvent {

	private final LivingEntity target;
	private final DecimalPosition position;
	private final double amount;
	private final double exponent;

	public DelayedKnockback(LivingEntity e, DecimalPosition from, double amt) {
		this(e, from, amt, 0);
	}

	public DelayedKnockback(LivingEntity e, DecimalPosition from, double amt, double exp) {
		target = e;
		amount = amt;
		position = from;
		exponent = exp;
	}

	@Override
	public void fire() {
		if (target.isAlive())
			ReikaEntityHelper.knockbackEntityFromPos(position.xCoord, position.yCoord, position.zCoord, target, amount, exponent);
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
