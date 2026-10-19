package reika.dragonapi.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
/** Screen blend is a forward operation on the composed scene, with normal scene depth. */
public final class ScreenParticleRendering {
    public static final RenderPhaseKey<SubmitNode> PHASE=new RenderPhaseKey<>(collection->((ScreenParticlePhaseAccess)collection).dragonapi$screenParticles());
    private static long classicPasses,improvedPasses;
    private ScreenParticleRendering() {}
    /** Explicit forward draw on the composed world color; the native phase owns grouping and lifetime. */
    public static void submitGeometry(SubmitNodeCollector collector,PoseStack pose,RenderType type,
            SubmitNodeCollector.CustomGeometryRenderer renderer) {
        collector.submitSpecial(PHASE,new CustomFeatureRenderer.Submit(pose.last().copy(),type,renderer));
    }
    /** Read-only diagnostics count successfully executed, populated native screen phases. Render thread only. */
    public static long completedPasses(boolean improved) { return improved?improvedPasses:classicPasses; }
    public static void completed(boolean improved) { if(improved) improvedPasses++; else classicPasses++; }
}
