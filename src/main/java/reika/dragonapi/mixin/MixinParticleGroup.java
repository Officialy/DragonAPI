package reika.dragonapi.mixin;

import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.dragonapi.extras.ThrottleableEffectRenderer;
import reika.dragonapi.client.ParticleGroupAccess;
import reika.dragonapi.client.BoundedParticleQueue;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAccess {
    @Shadow @Final protected ParticleEngine engine;
    @Shadow @Final @Mutable protected Queue<Particle> particles;
    @Inject(method="<init>",at=@At("RETURN"))
    private void dragonapi$ownedQueue(ParticleEngine engine,CallbackInfo callback) {
        var controller=ThrottleableEffectRenderer.forEngine(engine);
        particles=new BoundedParticleQueue<>(controller.limit,controller::bucket,controller::countsToLimit,controller::retire);
    }
    @Override public ParticleEngine dragonapi$getEngine() { return engine; }
    @Override public Queue<Particle> dragonapi$getParticles() { return particles; }
    @Inject(method="add",at=@At("HEAD"),cancellable=true)
    private void dragonapi$configuredCap(Particle particle,CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(ThrottleableEffectRenderer.forEngine(engine).addToGroup(particles,particle));
    }
}
