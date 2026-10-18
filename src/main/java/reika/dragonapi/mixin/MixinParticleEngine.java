package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.client.ParticleEngineAccess;
import reika.dragonapi.client.ParticleDepthLayers;
import reika.dragonapi.client.BoundedParticleQueue;
import reika.dragonapi.client.ParticleGroupAccess;
import reika.dragonapi.extras.ThrottleableEffectRenderer;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine implements ParticleEngineAccess {
    @Shadow protected ClientLevel level;
    @Shadow @Final private ParticleResources resourceManager;
    @Shadow @Final private Map<ParticleRenderType,ParticleGroup<?>> particles;
    @Shadow @Final @Mutable private Queue<Particle> particlesToAdd;
    @Shadow protected abstract void updateCount(ParticleLimit limit,int change);
    @Unique private ThrottleableEffectRenderer dragonapi$controller;

    @Inject(method="<init>",at=@At("RETURN"))
    private void dragonapi$attach(ClientLevel level,ParticleResources resources,CallbackInfo callback) {
        new ThrottleableEffectRenderer((ParticleEngine)(Object)this);
        particlesToAdd=new BoundedParticleQueue<>(dragonapi$controller.limit,dragonapi$controller::bucket,dragonapi$controller::retire);
    }
    @Override public ThrottleableEffectRenderer dragonapi$getController() { return dragonapi$controller; }
    @Override public void dragonapi$attachController(ThrottleableEffectRenderer controller) {
        if(dragonapi$controller!=null || controller.getOriginalEngine()!=(Object)this) throw new IllegalStateException("Invalid controller attachment");
        dragonapi$controller=controller;
    }
    @Override public void dragonapi$changeCount(ParticleLimit limit,int change) { updateCount(limit,change); }
    @Override public ClientLevel dragonapi$getLevel() { return level; }
    @Override public ParticleResources dragonapi$getResources() { return resourceManager; }
    @Override public int dragonapi$countOwnedParticles() {
        return particlesToAdd.size()+particles.values().stream().mapToInt(ParticleGroup::size).sum();
    }
    @WrapMethod(method="add")
    private void dragonapi$admit(Particle particle,Operation<Void> original) {
        if(!dragonapi$controller.dispatch(particle)) original.call(particle);
    }
    @WrapMethod(method="tick")
    private void dragonapi$tick(Operation<Void> original) { dragonapi$controller.checkOwner(); original.call(); dragonapi$controller.tickDelegates(); }
    @WrapMethod(method="clearParticles")
    private void dragonapi$clear(Operation<Void> original) {
        if(!dragonapi$controller.beginClear()) return;
        try {
            for(var group:particles.values())
                for(var particle:java.util.List.copyOf(((ParticleGroupAccess)group).dragonapi$getParticles())) particle.remove();
            original.call(); dragonapi$controller.clearDelegates(); ParticleDepthLayers.clearLayers();
        } finally { dragonapi$controller.finishClear(); }
    }
    @WrapMethod(method="countParticles")
    private String dragonapi$statistics(Operation<String> original) { return dragonapi$controller.statistics(original.call()); }
    @WrapMethod(method="setLevel")
    private void dragonapi$level(ClientLevel level,Operation<Void> original) {
        dragonapi$controller.beginLevelChange();
        try { original.call(level); dragonapi$controller.levelChanged(this.level); }
        finally { dragonapi$controller.finishLevelChange(); }
    }
    @WrapMethod(method="extract")
    private void dragonapi$extract(ParticlesRenderState state,Frustum frustum,Camera camera,float partialTick,Operation<Void> original) {
        dragonapi$controller.checkOwner();
        dragonapi$controller.rememberFrustum(frustum);
        if(!ThrottleableEffectRenderer.renderParticles) return;
        try(var scope=ParticleDepthLayers.begin()) {
            original.call(state,frustum,camera,partialTick);
            dragonapi$controller.extractDelegates(state,frustum,camera,partialTick);
        }
    }
}
