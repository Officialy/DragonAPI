package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reika.dragonapi.extras.ThrottleableEffectRenderer;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup {
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    private boolean dragonapi$bounds(Frustum frustum,double x,double y,double z,Operation<Boolean> original,@Local SingleQuadParticle particle) {
        return ThrottleableEffectRenderer.isParticleVisible(particle,frustum);
    }
}
