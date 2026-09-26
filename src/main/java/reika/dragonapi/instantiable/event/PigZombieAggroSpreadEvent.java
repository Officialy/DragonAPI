package reika.dragonapi.instantiable.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * V33a {@code PigZombieAggroSpreadEvent}: an angry zombified piglin is about to call the others around it onto
 * its target. Cancel it and they are not alerted; the piglin itself stays angry.
 *
 * <p>DragonAPI 1.7.10 put this on {@code EntityPigZombie.attackEntityFrom}'s loop that angered every pig zombie
 * within 32 blocks. 26.2 spreads the anger in two places, both routed through this event:
 * {@code HurtByTargetGoal.alertOthers} when the piglin is hit ({@code MixinHurtByTargetGoal}), and
 * {@code ZombifiedPiglin.alertOthers} every few seconds while it keeps its target in sight
 * ({@code MixinZombifiedPiglin}). The second has no damage source, so the event carries the piglin and its
 * target.
 */
public class PigZombieAggroSpreadEvent extends LivingEvent implements ICancellableEvent {

	public final LivingEntity target;

	public PigZombieAggroSpreadEvent(ZombifiedPiglin entity, LivingEntity target) {
		super(entity);
		this.target = target;
	}

	public ZombifiedPiglin getPiglin() {
		return (ZombifiedPiglin)this.getEntity();
	}

	/** Whether the piglin may alert the others around it. */
	public static boolean fire(ZombifiedPiglin e, LivingEntity target) {
		if (target == null)
			return true;
		return !NeoForge.EVENT_BUS.post(new PigZombieAggroSpreadEvent(e, target)).isCanceled();
	}
}
