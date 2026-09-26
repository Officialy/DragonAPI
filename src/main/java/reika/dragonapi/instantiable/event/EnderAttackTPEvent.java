package reika.dragonapi.instantiable.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * V33a {@code EnderAttackTPEvent}: an enderman is being hit by a projectile and is about to teleport out of its
 * way. Cancel it and the enderman takes the hit as it would a melee blow.
 *
 * <p>DragonAPI 1.7.10 replaced {@code EntityEnderman.attackEntityFrom}'s {@code instanceof
 * EntityDamageSourceIndirect} test with this event. The 26.2 test is {@code EnderMan.hurtServer}'s
 * {@code source.is(DamageTypeTags.IS_PROJECTILE)}, where {@code MixinEnderMan} fires it. (Cancelling NeoForge's
 * {@code EntityTeleportEvent.EnderEntity} instead would leave the projectile branch returning without ever
 * applying the damage.)
 */
public class EnderAttackTPEvent extends LivingEvent implements ICancellableEvent {

	public final DamageSource source;
	public final float amount;

	public EnderAttackTPEvent(EnderMan entity, DamageSource source, float amount) {
		super(entity);
		this.source = source;
		this.amount = amount;
	}

	public EnderMan getEnderman() {
		return (EnderMan)this.getEntity();
	}

	/** Whether the enderman treats this damage as a projectile it can dodge. */
	public static boolean fire(EnderMan e, DamageSource src, float amt, boolean original) {
		if (!original)
			return false;
		return !NeoForge.EVENT_BUS.post(new EnderAttackTPEvent(e, src, amt)).isCanceled();
	}
}
