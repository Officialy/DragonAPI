package reika.dragonapi.client;

import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;
import reika.dragonapi.extras.ThrottleableEffectRenderer;

/** Depth overrides are immutable pipeline variants; extraction samples the debug key once per engine tree. */
public final class ParticleDepthLayers {
    private static final ThreadLocal<Boolean> THROUGH_WALLS=new ThreadLocal<>();
    private static final Map<SingleQuadParticle.Layer,SingleQuadParticle.Layer> LAYERS=new WeakHashMap<>();
    private static final Map<RenderPipeline,RenderPipeline> PIPELINES=new WeakHashMap<>();
    private ParticleDepthLayers() {}
    public static Scope begin() {
        var previous=THROUGH_WALLS.get();
        return begin(previous!=null?previous:ThrottleableEffectRenderer.renderThroughWalls());
    }
    public static Scope begin(boolean throughWalls) {
        var previous=THROUGH_WALLS.get();
        THROUGH_WALLS.set(previous!=null?previous:throughWalls);
        return new Scope(previous);
    }
    public static synchronized void clear() { LAYERS.clear(); PIPELINES.clear(); }
    public static synchronized void clearLayers() { LAYERS.clear(); }
    public static final class Scope implements AutoCloseable {
        private final Boolean previous;
        private Scope(Boolean previous) { this.previous=previous; }
        @Override public void close() { if(previous==null) THROUGH_WALLS.remove(); else THROUGH_WALLS.set(previous); }
    }
    public static SingleQuadParticle.Layer map(SingleQuadParticle.Layer layer) {
        return Boolean.TRUE.equals(THROUGH_WALLS.get())?withoutDepth(layer):layer;
    }
    public static synchronized SingleQuadParticle.Layer withoutDepth(SingleQuadParticle.Layer layer) {
        return LAYERS.computeIfAbsent(layer,source->{
            var oit=source.oitPipelineSet();
            var mapped=oit==null?null:new OitPipelineSet(withoutDepth(oit.depthBoundsPipeline()),
                    withoutDepth(oit.transmittancePipeline()),withoutDepth(oit.accumulatePipeline()));
            return new SingleQuadParticle.Layer(source.translucent(),source.textureAtlasLocation(),withoutDepth(source.pipeline()),mapped);
        });
    }
    private static RenderPipeline withoutDepth(RenderPipeline source) {
        return PIPELINES.computeIfAbsent(source,pipeline->pipeline.toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("dragonapi","particle_depth_override/"+pipeline.getLocation().getNamespace()+"/"+pipeline.getLocation().getPath()))
                .withDepthStencilState(pipeline.getDepthStencilState()==null?java.util.Optional.empty():java.util.Optional.of(
                        new DepthStencilState(CompareOp.ALWAYS_PASS,false,pipeline.getDepthStencilState().depthBiasScaleFactor(),
                                pipeline.getDepthStencilState().depthBiasConstant(),pipeline.getDepthStencilState().stencilTest()))).build());
    }
}
