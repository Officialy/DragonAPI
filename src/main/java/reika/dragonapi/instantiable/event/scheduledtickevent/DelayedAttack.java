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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.LogicalSide;

public class DelayedAttack implements ScheduledEvent {

	private final LivingEntity target;
	private final DamageSource source;
	private final float amount;

	public DelayedAttack(LivingEntity e, DamageSource src, float amt) {
		target = e;
		source = src;
		amount = amt;
	}

	@Override
	public void fire() {
		// 1.7.10 attackEntityFrom on a dead or unloaded entity was a no-op; hurtServer needs the guard.
		if (target.isAlive() && target.level() instanceof ServerLevel level)
			target.hurtServer(level, source, amount);
	}

	@Override
	public boolean runOnSide(LogicalSide s) {
		return s == LogicalSide.SERVER;
	}

}
