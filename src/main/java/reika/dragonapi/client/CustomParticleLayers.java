package reika.dragonapi.client;

import com.mojang.renderpearl.api.pipeline.*;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;
import reika.dragonapi.instantiable.rendering.ReikaParticleEngine.RenderModeFlags;

/** Immutable native declarations for custom particles. No global GL state is changed. */
public final class CustomParticleLayers {
    private static final Identifier SHADER=Identifier.fromNamespaceAndPath("dragonapi","core/custom_particle");
    private static final Map<Integer,Pipelines> PIPELINES=new HashMap<>();
    private static final java.util.Set<RenderPipeline> SCREEN_PIPELINES=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final Map<SingleQuadParticle.Layer,Map<Key,SingleQuadParticle.Layer>> LAYERS=new WeakHashMap<>();
    private static final ThreadLocal<UnaryOperator<SingleQuadParticle.Layer>> CURRENT=new ThreadLocal<>();
    private record Key(Identifier texture,int flags) {}
    private record Pipelines(RenderPipeline forward,OitPipelineSet oit) {}
    private CustomParticleLayers() {}
    public static Scope begin(ParticleAdmissionPolicy policy,Particle particle) {
        var previous=CURRENT.get();
        CURRENT.set(layer->policy.particleLayer(particle,layer));
        return new Scope(previous);
    }
    public static final class Scope implements AutoCloseable {
        private final UnaryOperator<SingleQuadParticle.Layer> previous;
        private Scope(UnaryOperator<SingleQuadParticle.Layer> previous) { this.previous=previous; }
        @Override public void close() { if(previous==null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static SingleQuadParticle.Layer mapCurrent(SingleQuadParticle.Layer source) {
        var mapping=CURRENT.get(); return mapping==null?source:mapping.apply(source);
    }
    public static synchronized void clearLayers() { LAYERS.clear(); }
    public static synchronized SingleQuadParticle.Layer map(SingleQuadParticle.Layer source,Identifier texture,int flags) {
        if((flags&~63)!=0) throw new IllegalArgumentException("Unknown custom particle render flags: "+flags);
        var variants=LAYERS.computeIfAbsent(source,ignored->new HashMap<>());
        return variants.computeIfAbsent(new Key(texture,flags),key->{
            var pipelines=PIPELINES.computeIfAbsent(flags,CustomParticleLayers::pipelines);
            // Screen blending needs per-channel destination factors. Native OIT transmittance
            // is scalar; send screen layers through native forward drawing with the exact law.
            boolean translucent=enabled(flags,RenderModeFlags.ALPHA) && !enabled(flags,RenderModeFlags.ADDITIVE);
            return new SingleQuadParticle.Layer(translucent,texture,pipelines.forward,translucent?pipelines.oit:null);
        });
    }
    public static synchronized boolean isScreenLayer(SingleQuadParticle.Layer layer) { return SCREEN_PIPELINES.contains(layer.pipeline()); }
    private static boolean enabled(int flags,RenderModeFlags flag) { return (flags&flag.mask())!=0; }
    private static RenderPipeline.Builder base(RenderPipeline.Snippet snippet,int flags) {
        var builder=RenderPipeline.builder(snippet).withVertexShader(SHADER).withFragmentShader(SHADER)
                .withCull(false).withDepthStencilState(new DepthStencilState(
                        enabled(flags,RenderModeFlags.DEPTH)?CompareOp.GREATER_THAN_OR_EQUAL:CompareOp.ALWAYS_PASS,false));
        if(enabled(flags,RenderModeFlags.LIGHT)) builder.withShaderDefine("CUSTOM_LIGHT").withBindGroupLayout(BindGroupLayouts.LIGHTING);
        if(enabled(flags,RenderModeFlags.FOG)) builder.withShaderDefine("CUSTOM_FOG");
        if(enabled(flags,RenderModeFlags.ALPHACLIP)) builder.withShaderDefine("CUSTOM_ALPHA_CLIP");
        return builder;
    }
    private static Pipelines pipelines(int flags) {
        var id=Identifier.fromNamespaceAndPath("dragonapi","custom_particle/"+flags);
        var forward=base(RenderPipelines.PARTICLE_SNIPPET,flags).withLocation(id);
        ColorTargetState target=ColorTargetState.DEFAULT;
        if(enabled(flags,RenderModeFlags.ALPHA)) target=new ColorTargetState(enabled(flags,RenderModeFlags.ADDITIVE)
                ?new BlendFunction(BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_COLOR,BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA)
                :new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE_MINUS_SRC_ALPHA));
        forward.withColorTargetState(target);
        var oit=OitPipelineSet.builder(id,base(RenderPipelines.OIT_PARTICLE_SNIPPET,flags));
        if(!enabled(flags,RenderModeFlags.DEPTH)) oit.withoutDepthTest();
        var pipeline=forward.build();
        if(enabled(flags,RenderModeFlags.ALPHA) && enabled(flags,RenderModeFlags.ADDITIVE)) SCREEN_PIPELINES.add(pipeline);
        return new Pipelines(pipeline,oit.build());
    }
}
