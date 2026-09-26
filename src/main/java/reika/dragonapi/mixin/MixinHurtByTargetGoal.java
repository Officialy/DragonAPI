package reika.dragonapi.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.instantiable.event.PigZombieAggroSpreadEvent;

/** Fires {@link PigZombieAggroSpreadEvent} when a hit zombified piglin calls the others onto its attacker. */
@Mixin(HurtByTargetGoal.class)
public abstract class MixinHurtByTargetGoal extends TargetGoal {

	private MixinHurtByTargetGoal(PathfinderMob mob, boolean mustSee) {
		super(mob, mustSee);
	}

	@Inject(method = "alertOthers", at = @At("HEAD"), cancellable = true)
	private void dragonapi$pigZombieAggroSpread(CallbackInfo ci) {
		if (this.mob instanceof ZombifiedPiglin piglin && !PigZombieAggroSpreadEvent.fire(piglin, piglin.getLastHurtByMob()))
			ci.cancel();
	}
}
