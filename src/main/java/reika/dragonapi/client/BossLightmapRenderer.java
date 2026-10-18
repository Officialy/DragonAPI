package reika.dragonapi.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Vector3f;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.event.client.BossColorEvent;

/** Native lightmap math with a per-operation RGB uniform at the original boss-darkening stage. */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class BossLightmapRenderer implements AutoCloseable {
    private static final int UBO_SIZE = new Std140SizeCalculator().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putVec3().putVec3().putVec3().putVec3().putVec3().get();
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.parse("dragonapi:pipeline/boss_lightmap"))
            .withVertexShader("core/screenquad").withFragmentShader(Identifier.parse("dragonapi:core/boss_lightmap"))
            .withBindGroupLayout(BindGroupLayouts.LIGHTMAP_INFO).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState.DEFAULT).build();
    private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "DragonAPI boss lightmap UBO", 130, UBO_SIZE);

    @SubscribeEvent public static void register(RegisterRenderPipelinesEvent event) { event.registerPipeline(PIPELINE); }

    public void render(LightmapRenderState state, GpuTextureView target, BossColorEvent.Color color) {
        if (!state.needsUpdate) return;
        var profiler = Profiler.get();
        profiler.push("lightmap");
        try {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBufferSlice.MappedView view = ubo.currentBuffer().map(false, true)) {
                Std140Builder.intoBuffer(view.data()).putFloat(state.skyFactor).putFloat(state.blockFactor)
                        .putFloat(state.nightVisionEffectIntensity).putFloat(state.darknessEffectScale)
                        .putFloat(state.bossOverlayWorldDarkening).putFloat(state.brightness)
                        .putVec3(state.blockLightTint).putVec3(state.skyLightColor).putVec3(state.ambientColor)
                        .putVec3(state.nightVisionColor).putVec3(new Vector3f(color.red(), color.green(), color.blue()));
            }
            try (var pass = encoder.createRenderPass(() -> "DragonAPI boss lightmap", target, Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("LightmapInfo", ubo.currentBuffer());
                pass.draw(3, 1, 0, 0);
            }
            ubo.rotate();
        } finally { profiler.pop(); }
    }

    @Override public void close() { ubo.close(); }
}
