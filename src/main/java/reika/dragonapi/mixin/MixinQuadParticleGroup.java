package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import reika.dragonapi.client.CustomParticleLayers;
import reika.dragonapi.client.ParticleAdmissionPolicy;
import reika.dragonapi.client.ParticleGroupAccess;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reika.dragonapi.extras.ThrottleableEffectRenderer;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup {
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"))
    private void dragonapi$customLayer(SingleQuadParticle particle,QuadParticleRenderState state,Camera camera,float partialTick,Operation<Void> original) {
        var engine=((ParticleGroupAccess)this).dragonapi$getEngine();
        if(engine instanceof ParticleAdmissionPolicy policy) {
            try(var scope=CustomParticleLayers.begin(policy,particle)) { original.call(particle,state,camera,partialTick); }
        } else original.call(particle,state,camera,partialTick);
    }
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    private boolean dragonapi$bounds(Frustum frustum,double x,double y,double z,Operation<Boolean> original,@Local SingleQuadParticle particle) {
        return ThrottleableEffectRenderer.isParticleVisible(particle,frustum);
    }
}
