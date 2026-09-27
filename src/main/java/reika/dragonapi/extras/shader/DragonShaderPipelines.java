/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.extras.shader;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import reika.dragonapi.DragonAPI;

/**
 * {@link RenderPipeline}s for the DragonAPI shader system.
 */
public final class DragonShaderPipelines {

    private DragonShaderPipelines() {}

    /**
     * The focus-point accumulation pass, drawn by {@link ReikaShaderSystem} into its own render
     * target and then read by the {@code dragonapi:reika_effect} PostChain.
     *
     * <p>This is a fullscreen pass built the same way {@code PostChain} builds its own passes (on
     * {@code POST_PROCESSING_SNIPPET}, with vanilla's {@code core/screenquad} vertex shader, which
     * generates the fullscreen triangle from {@code gl_VertexID} and so needs no vertex buffer).
     * It is <em>not</em> declared as a PostChain pass because a PostChain bakes its uniforms into an
     * immutable buffer when the chain is compiled, and the focus points change every frame. Driving
     * the pass by hand lets it take a {@code FocusPoints} UBO that is rewritten per frame.</p>
     *
     * <p>{@code POST_PROCESSING_SNIPPET} builds on {@code GLOBALS_SNIPPET}, so the shader also gets
     * the {@code Globals} block (it reads {@code ScreenSize} for the aspect correction).</p>
     */
    public static final RenderPipeline REIKA_STENCIL = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DragonAPI.MODID, "pipeline/reika_stencil"))
            // The String overloads take a bare path and assume the minecraft namespace, so ours has
            // to be an explicit Identifier or it ends up as "minecraft:dragonapi:post/...".
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath(DragonAPI.MODID, "post/reika_stencil"))
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform("FocusPoints", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();

    /** Subscribed on the mod event bus by {@code DragonAPI}; client only. */
    public static void register(IEventBus modBus) {
        modBus.addListener(DragonShaderPipelines::onRegisterPipelines);
    }

    private static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(REIKA_STENCIL);
    }
}
