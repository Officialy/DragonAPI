package reika.dragonapi.client;

import com.mojang.renderpearl.api.pipeline.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import reika.dragonapi.extras.shader.IrisCompat;

/** Independent directional-light and lightmap choices owned by each submitted draw. */
public enum EntityLighting {
    LIT(true,true), LIGHTMAP_ONLY(false,true), DIRECTIONAL_ONLY(true,false), FULL_BRIGHT(false,false);

    public final boolean directional;
    public final boolean lightmap;
    private final Map<Boolean,Pipelines> pipelines=new HashMap<>();
    private final Map<Key,RenderType> types=new HashMap<>();
    private final Map<Boolean,RenderPipeline> cutoutPipelines=new HashMap<>();
    private final Map<Key,RenderType> cutoutTypes=new HashMap<>();
    private final Map<Boolean,RenderPipeline> screenPipelines=new HashMap<>();
    private final Map<Key,RenderType> screenTypes=new HashMap<>();
    private record Key(Identifier texture,boolean cull) {}
    private record Pipelines(RenderPipeline forward,OitPipelineSet oit) {}
    EntityLighting(boolean directional,boolean lightmap) { this.directional=directional; this.lightmap=lightmap; }

    public synchronized RenderType translucent(Identifier texture,boolean cull) {
        return types.computeIfAbsent(new Key(texture,cull),key->{
            var pair=pipelines(cull);
            var setup=RenderSetup.builder(pair.forward).setOitPipelines(pair.oit)
                    .withTexture("Sampler0",texture).sortOnUpload();
            if(lightmap) setup.useLightmap();
            return RenderType.create("dragonapi_entity_"+name().toLowerCase(java.util.Locale.ROOT),setup.createRenderSetup());
        });
    }
    /** Opaque alpha-cutout draw with independent directional and lightmap intent. */
    public synchronized RenderType cutout(Identifier texture,boolean cull) {
        return cutoutTypes.computeIfAbsent(new Key(texture,cull),key->{
            var setup=RenderSetup.builder(cutoutPipeline(cull)).withTexture("Sampler0",texture);
            if(lightmap) setup.useLightmap();
            return RenderType.create("dragonapi_entity_cutout_"+name().toLowerCase(java.util.Locale.ROOT),setup.createRenderSetup());
        });
    }
    private synchronized RenderPipeline cutoutPipeline(boolean cull) {
        return cutoutPipelines.computeIfAbsent(cull,key->configure(RenderPipeline.builder(
                    lightmap?RenderPipelines.ENTITY_SNIPPET:RenderPipelines.ENTITY_EMISSIVE_SNIPPET))
                .withLocation(Identifier.fromNamespaceAndPath("dragonapi","entity_cutout/"+name().toLowerCase(java.util.Locale.ROOT)+"/"+(cull?"culled":"double_sided")))
                .withCull(cull).withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(DepthStencilState.DEFAULT).withShaderDefine("ALPHA_CUTOUT",0.1F).build());
    }
    /** RGB screen blending; world callers submit through ScreenParticleRendering's native forward phase. */
    public synchronized RenderType screen(Identifier texture,boolean cull) {
        return screenTypes.computeIfAbsent(new Key(texture,cull),key->{
            var setup=RenderSetup.builder(screenPipeline(cull)).withTexture("Sampler0",texture);
            if(lightmap) setup.useLightmap();
            return RenderType.create("dragonapi_entity_screen_"+name().toLowerCase(java.util.Locale.ROOT),setup.createRenderSetup());
        });
    }
    private synchronized RenderPipeline screenPipeline(boolean cull) {
        return screenPipelines.computeIfAbsent(cull,key->configure(RenderPipeline.builder(
                    lightmap?RenderPipelines.ENTITY_SNIPPET:RenderPipelines.ENTITY_EMISSIVE_SNIPPET))
                .withLocation(Identifier.fromNamespaceAndPath("dragonapi","entity_screen/"+name().toLowerCase(java.util.Locale.ROOT)+"/"+(cull?"culled":"double_sided")))
                .withCull(cull).withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_COLOR)))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                // Retain zero-alpha rejection for byte-alpha sprites, without imposing a new 0.1 cutout.
                .withShaderDefine("ALPHA_CUTOUT",1F/255F).build());
    }
    private RenderPipeline.Builder configure(RenderPipeline.Builder builder) {
        builder.withShaderDefine("NO_OVERLAY");
        if(!directional) builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        if(!lightmap) builder.withShaderDefine("EMISSIVE");
        return builder;
    }
    private synchronized Pipelines pipelines(boolean cull) {
        return pipelines.computeIfAbsent(cull,key->{
            var id=Identifier.fromNamespaceAndPath("dragonapi","entity_translucent/"+name().toLowerCase(java.util.Locale.ROOT)+"/"+(cull?"culled":"double_sided"));
            var forward=configure(RenderPipeline.builder(lightmap?RenderPipelines.ENTITY_SNIPPET:RenderPipelines.ENTITY_EMISSIVE_SNIPPET))
                    .withLocation(id).withCull(cull).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                    .withShaderDefine("ALPHA_CUTOUT",0.1F).build();
            var oit=OitPipelineSet.builder(id,configure(RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET)).withCull(cull));
            if(lightmap) oit.withAccumulateModifier(builder->builder.withBindGroupLayout(BindGroupLayouts.SAMPLER2));
            return new Pipelines(forward,oit.build());
        });
    }
    /** Physical-client registration; all profile/culling declarations are known before drawing. */
    public static void register(RegisterRenderPipelinesEvent event) {
        for(var mode:values()) for(boolean cull:new boolean[]{false,true}) {
            var screen=mode.screenPipeline(cull);
            event.registerPipeline(screen);
            IrisCompat.assignWithoutShadow(screen,"ENTITIES");
            var cutout=mode.cutoutPipeline(cull);
            event.registerPipeline(cutout);
            IrisCompat.assign(cutout,"ENTITIES");
            IrisCompat.assignShadow(cutout,"SHADOW_ENTITIES");
            var pair=mode.pipelines(cull);
            for(var pipeline:new RenderPipeline[]{pair.forward,pair.oit.depthBoundsPipeline(),pair.oit.transmittancePipeline(),pair.oit.accumulatePipeline()}) {
                event.registerPipeline(pipeline);
                IrisCompat.assign(pipeline,"ENTITIES");
            }
        }
    }
}
