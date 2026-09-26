package reika.dragonapi.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.instantiable.event.AttackAggroEvent;

/** Fires {@link AttackAggroEvent} where a hurt monster learns who hurt it. */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityAggro {
	@Inject(method = "resolveMobResponsibleForDamage", at = @At("HEAD"), cancellable = true)
	private void dragonapi$attackAggroEvent(DamageSource source, CallbackInfo ci) {
		if ((Object)this instanceof Monster mob && source.getEntity() != null && !AttackAggroEvent.fire(mob, source))
			ci.cancel();
	}
}
