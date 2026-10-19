package reika.dragonapi.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.*;
import reika.dragonapi.client.ScreenParticleFrameAccess;
/** Screen blending consumes the composed OIT color while retaining opaque scene depth. */
@Mixin(LevelRenderer.class)
public abstract class MixinScreenParticleLevelRenderer {
    @Shadow @Final private LevelTargetBundle targets;
    @WrapMethod(method="executeOit")
    private void dragonapi$screenAfterComposite(ChunkSectionsToRender sections,FeatureRenderDispatcher.PreparedFrame frame,Operation<Void> original) {
        original.call(sections,frame);
        var screen=(ScreenParticleFrameAccess)frame;
        if(!screen.dragonapi$hasScreenParticles()) return;
        var target=targets.main.get();
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                ()->"DragonAPI screen-color effects after OIT",target.getColorTextureView(),Optional.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            screen.dragonapi$executeScreenParticles(pass,true);
        }
    }
}
