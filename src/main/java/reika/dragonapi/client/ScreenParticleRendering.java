package reika.dragonapi.client;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
/** Screen blend is a forward operation on the composed scene, with normal scene depth. */
public final class ScreenParticleRendering {
    public static final RenderPhaseKey<SubmitNode> PHASE=new RenderPhaseKey<>(collection->((ScreenParticlePhaseAccess)collection).dragonapi$screenParticles());
    private static long classicPasses,improvedPasses;
    private ScreenParticleRendering() {}
    /** Read-only diagnostics count successfully executed, populated native screen phases. Render thread only. */
    public static long completedPasses(boolean improved) { return improved?improvedPasses:classicPasses; }
    public static void completed(boolean improved) { if(improved) improvedPasses++; else classicPasses++; }
}
