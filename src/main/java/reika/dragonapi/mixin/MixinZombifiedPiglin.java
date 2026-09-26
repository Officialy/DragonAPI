package reika.dragonapi.mixin;

import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.instantiable.event.PigZombieAggroSpreadEvent;

/** Fires {@link PigZombieAggroSpreadEvent} at an angry zombified piglin's periodic alert of the others. */
@Mixin(ZombifiedPiglin.class)
public abstract class MixinZombifiedPiglin {
	@Inject(method = "alertOthers", at = @At("HEAD"), cancellable = true)
	private void dragonapi$pigZombieAggroSpread(CallbackInfo ci) {
		ZombifiedPiglin self = (ZombifiedPiglin)(Object)this;
		if (!PigZombieAggroSpreadEvent.fire(self, self.getTarget()))
			ci.cancel();
	}
}
