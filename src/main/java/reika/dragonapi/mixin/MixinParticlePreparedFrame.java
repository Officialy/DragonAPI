package reika.dragonapi.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import reika.dragonapi.client.*;
/** Execute native prepared quad groups; phase queues have already drained by this point. */
@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class MixinParticlePreparedFrame implements ScreenParticleFrameAccess {
    @Shadow private FeatureFrameContext context;
    @Shadow private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private Map<FeatureRenderPhase<?>,List<?>> groupsByPhase;
    @Invoker("executePhase") public abstract void dragonapi$executePhase(FeatureRenderPhase<?> phase,FeatureFrameContext context,RenderPass pass);
    @Override public boolean dragonapi$hasScreenParticles() {
        if(submitNodeStorage==null) return false;
        for(var collection:submitNodeStorage.getSubmitsPerOrder().values())
            if(!groupsByPhase.getOrDefault(((ScreenParticlePhaseAccess)collection).dragonapi$screenParticles(),List.of()).isEmpty()) return true;
        return false;
    }
    @Override public void dragonapi$executeScreenParticles(RenderPass pass,boolean improved) {
        if(!dragonapi$hasScreenParticles()) return;
        for(var collection:submitNodeStorage.getSubmitsPerOrder().values())
            dragonapi$executePhase(((ScreenParticlePhaseAccess)collection).dragonapi$screenParticles(),Objects.requireNonNull(context),pass);
        ScreenParticleRendering.completed(improved);
    }
    @WrapMethod(method="executeTranslucentAfterTerrain")
    private void dragonapi$classicScreen(RenderPass pass,Operation<Void> original) {
        original.call(pass);
        dragonapi$executeScreenParticles(pass,false);
    }
}
