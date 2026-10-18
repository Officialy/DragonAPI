package reika.dragonapi.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import reika.dragonapi.client.ParticleLayerAccess;

@Mixin(SingleQuadParticle.class)
public interface MixinSingleQuadParticle extends ParticleLayerAccess {
    @Override @Invoker("getLayer") SingleQuadParticle.Layer dragonapi$getLayer();
}
