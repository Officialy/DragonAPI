package reika.dragonapi.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import reika.dragonapi.client.ParticleDepthLayers;

@Mixin(QuadParticleRenderState.class)
public abstract class MixinQuadParticleRenderState {
    @ModifyVariable(method="add",at=@At("HEAD"),argsOnly=true)
    private SingleQuadParticle.Layer dragonapi$debugDepth(SingleQuadParticle.Layer layer) { return ParticleDepthLayers.map(layer); }
}
