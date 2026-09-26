package reika.dragonapi.instantiable.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * V33a {@code AttackAggroEvent}: a hostile mob has just been hurt and is about to take its attacker as its
 * target. Cancel it and the mob does not turn on the attacker (the damage itself still lands).
 *
 * <p>DragonAPI 1.7.10 replaced the {@code entity != this} test that set {@code EntityMob.entityToAttack} (and
 * {@code EntityPigZombie}'s {@code instanceof EntityPlayer} anger test) with this event. In 26.2 a hurt mob
 * learns its attacker through {@code LivingEntity.resolveMobResponsibleForDamage}, which is what
 * {@code HurtByTargetGoal} (and so a zombified piglin's anger) reads; {@code MixinLivingEntityAggro} fires the
 * event there, for {@link Monster}s as 1.7.10 did for {@code EntityMob}. Kill credit
 * ({@code resolvePlayerResponsibleForDamage}) is untouched.
 */
public class AttackAggroEvent extends LivingEvent implements ICancellableEvent {

	public final DamageSource source;

	public AttackAggroEvent(Monster entity, DamageSource source) {
		super(entity);
		this.source = source;
	}

	public Monster getMob() {
		return (Monster)this.getEntity();
	}

	/** Whether the mob may take its attacker as its target. */
	public static boolean fire(Monster e, DamageSource src) {
		return !NeoForge.EVENT_BUS.post(new AttackAggroEvent(e, src)).isCanceled();
	}
}
