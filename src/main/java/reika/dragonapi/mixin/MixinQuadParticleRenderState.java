package reika.dragonapi.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import reika.dragonapi.client.*;
/** Keep per-channel screen blending out of solid and scalar-transmittance OIT groups. */
@Mixin(QuadParticleRenderState.class)
public abstract class MixinQuadParticleRenderState implements ScreenParticleStateAccess {
    @Unique private boolean dragonapi$screenStorage;
    @Unique private QuadParticleRenderState dragonapi$screen;
    @Override public void dragonapi$markScreenStorage() { dragonapi$screenStorage=true; }
    @Override public QuadParticleRenderState dragonapi$getScreenStorage() { return dragonapi$screen; }
    @WrapMethod(method="add")
    private void dragonapi$customLayer(SingleQuadParticle.Layer layer,float x,float y,float z,float qx,float qy,float qz,float qw,
            float size,float u0,float u1,float v0,float v1,int color,int light,Operation<Void> original) {
        if(dragonapi$screenStorage) { original.call(layer,x,y,z,qx,qy,qz,qw,size,u0,u1,v0,v1,color,light); return; }
        var mapped=CustomParticleLayers.mapCurrent(layer);
        boolean screen=CustomParticleLayers.isScreenLayer(mapped);
        mapped=ParticleDepthLayers.map(mapped);
        if(screen) {
            if(dragonapi$screen==null) {
                dragonapi$screen=new QuadParticleRenderState();
                ((ScreenParticleStateAccess)dragonapi$screen).dragonapi$markScreenStorage();
            }
            dragonapi$screen.add(mapped,x,y,z,qx,qy,qz,qw,size,u0,u1,v0,v1,color,light);
        } else original.call(mapped,x,y,z,qx,qy,qz,qw,size,u0,u1,v0,v1,color,light);
    }
    @WrapMethod(method="isEmpty")
    private boolean dragonapi$hasAnyParticles(Operation<Boolean> original) {
        return original.call() && (dragonapi$screen==null || dragonapi$screen.isEmpty());
    }
    @WrapMethod(method="clear")
    private void dragonapi$clearScreen(Operation<Void> original) {
        original.call(); if(dragonapi$screen!=null) dragonapi$screen.clear();
    }
    @WrapMethod(method="submit")
    private void dragonapi$submitScreen(SubmitNodeCollector collector,CameraRenderState camera,Operation<Void> original) {
        original.call(collector,camera);
        if(dragonapi$screen!=null && !dragonapi$screen.isEmpty())
            collector.order(0).submitSpecial(ScreenParticleRendering.PHASE,new QuadParticleFeatureRenderer.Submit(dragonapi$screen,false));
    }
}
