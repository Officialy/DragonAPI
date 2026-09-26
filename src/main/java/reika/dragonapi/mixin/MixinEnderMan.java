package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import reika.dragonapi.instantiable.event.EnderAttackTPEvent;

/** Fires {@link EnderAttackTPEvent} at the projectile test in {@code EnderMan.hurtServer}. */
@Mixin(EnderMan.class)
public abstract class MixinEnderMan {
	@WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean dragonapi$enderAttackTPEvent(DamageSource source, TagKey<DamageType> tag, Operation<Boolean> original,
			@Local(argsOnly = true) float damage) {
		return EnderAttackTPEvent.fire((EnderMan)(Object)this, source, damage, original.call(source, tag));
	}
}
