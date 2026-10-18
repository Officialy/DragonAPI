package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import reika.dragonapi.instantiable.event.client.BossColorEvent;

/** The three multipliers share one invocation-local event result, before native fog brightening. */
@Mixin(FogRenderer.class)
public abstract class MixinBossFogColor {
    @ModifyConstant(method = "computeFogColor", constant = @Constant(floatValue = 0.7F), require = 1, allow = 1)
    private float dragonapi$bossRed(float original, @Share("dragonapi$bossColor") LocalRef<BossColorEvent.Color> color) {
        color.set(BossColorEvent.fire(false));
        return color.get().red();
    }

    @ModifyConstant(method = "computeFogColor", constant = @Constant(floatValue = 0.6F, ordinal = 0), require = 1, allow = 1)
    private float dragonapi$bossGreen(float original, @Share("dragonapi$bossColor") LocalRef<BossColorEvent.Color> color) {
        return color.get().green();
    }

    @ModifyConstant(method = "computeFogColor", constant = @Constant(floatValue = 0.6F, ordinal = 1), require = 1, allow = 1)
    private float dragonapi$bossBlue(float original, @Share("dragonapi$bossColor") LocalRef<BossColorEvent.Color> color) {
        return color.get().blue();
    }
}
