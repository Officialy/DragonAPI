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

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import reika.dragonapi.DragonAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reika's screen-warp shader, rebuilt on Minecraft's own shader system.
 *
 * <p>Effects register "focus points" — world positions that drag and tint the screen around them —
 * and this drives two passes each frame:</p>
 * <ol>
 *   <li>an accumulation pass ({@code dragonapi:post/reika_stencil}) that resolves every point into
 *       an offscreen buffer, storing the falloff factor in red and the winning point's screen UV in
 *       green/blue;</li>
 *   <li>the {@code dragonapi:reika_effect} PostChain, which reads that buffer alongside the scene
 *       and warps the scene toward each point.</li>
 * </ol>
 *
 * <h2>Why it is split this way</h2>
 * <p>1.7.10 ran the accumulation once per point, ping-ponging framebuffers, feeding each point's
 * world coords in as uniforms. Modern Minecraft compiles a PostChain's uniforms into an immutable
 * buffer, so a declarative chain cannot carry data that changes per frame — the focus points would
 * be frozen at whatever they were when the chain loaded. The dynamic data therefore reaches the GPU
 * as a <em>texture</em> (the accumulation buffer) rather than as uniforms, which is the same trick
 * the original used to get many points into one effect pass, and the chain itself stays static.</p>
 *
 * <p>The accumulation pass is hand-driven ({@link #drawStencil}) rather than declared in the chain
 * JSON precisely so it <em>can</em> take a per-frame UBO. It is handed to the chain as an external
 * target through {@link PostChain.TargetBundle}, which is why the chain JSON can name
 * {@code dragonapi:stencil} as an input.</p>
 */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class ReikaShaderSystem {

    /** The PostChain: assets/dragonapi/post_effect/reika_effect.json. */
    public static final Identifier EFFECT_ID = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "reika_effect");

    /** The accumulation buffer, as named by the chain JSON's input and supplied via a TargetBundle. */
    public static final Identifier STENCIL_TARGET_ID = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "stencil");

    /** Targets the chain is permitted to reference; enforced by {@code PostChain.load}. */
    private static final Set<Identifier> ALLOWED_TARGETS = Set.of(PostChain.MAIN_TARGET_ID, STENCIL_TARGET_ID);

    /** Must match {@code MAX_FOCUS_POINTS} in reika_stencil.fsh. */
    public static final int MAX_FOCUS_POINTS = 16;

    /** std140 layout of the FocusPoints block: ivec4 FocusCount, then vec4 Focus[MAX_FOCUS_POINTS]. */
    private static final int UBO_SIZE = 16 + MAX_FOCUS_POINTS * 16;

    /** MAP_WRITE | UNIFORM. */
    private static final int UBO_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;

    /**
     * How long a point survives without being refreshed. Callers re-post their points every frame
     * while their effect is live, so this only has to outlast a frame; it exists so a point vanishes
     * on its own when its machine stops rendering, breaks, or unloads, without needing a removal call.
     */
    private static final long FOCUS_LIFETIME_MS = 250;

    private static final List<Focus> POINTS = new ArrayList<>();

    private static @Nullable MappableRingBuffer focusUbo;

    private ReikaShaderSystem() {}

    private static final class Focus {

        final Vec3 position;
        float size;
        float strength;
        long lastSeen;

        Focus(Vec3 position, float size, float strength, long lastSeen) {
            this.position = position;
            this.size = size;
            this.strength = strength;
            this.lastSeen = lastSeen;
        }
    }

    /**
     * Post a focus point. Call every tick or frame the point should be visible; a point that stops
     * being posted fades out of the set on its own.
     *
     * <p>The two knobs are separate because the original kept them separate: {@code size} was a
     * per-focus value that only set how far the point's falloff reached, while the warp strength was
     * a single value applied to the whole effect.</p>
     *
     * @param position the world position to warp around
     * @param size     how far the point's influence reaches; scaled down with range like real size
     * @param strength how hard it warps, 0-1
     */
    public static void addFocus(Vec3 position, float size, float strength) {
        if (size <= 0 || strength <= 0)
            return;
        long now = Util.getMillis();
        for (Focus f : POINTS) {
            if (f.position.distanceToSqr(position) < 1.0E-6) {
                f.size = size;
                f.strength = strength;
                f.lastSeen = now;
                return;
            }
        }
        // The shader reads a fixed-size array; past that, extra points are simply dropped rather
        // than fighting over slots, since the nearest ones dominate the screen anyway.
        if (POINTS.size() >= MAX_FOCUS_POINTS)
            return;
        POINTS.add(new Focus(position, size, strength, now));
    }

    /** Runs once the level is fully drawn, so the chain warps the finished scene. */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
        long now = Util.getMillis();
        POINTS.removeIf(f -> now - f.lastSeen > FOCUS_LIFETIME_MS);
        if (POINTS.isEmpty())
            return;

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        render(event.getModelViewMatrix(), camera.projectionMatrix, camera.pos);
    }

    private static void render(Matrix4fc modelView, Matrix4fc projection, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        PostChain chain = mc.getShaderManager().getPostChain(EFFECT_ID, ALLOWED_TARGETS);
        if (chain == null)
            return;

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        int width = main.width;
        int height = main.height;

        List<float[]> projected = project(modelView, projection, cameraPos, width, height);
        if (projected.isEmpty())
            return;

        uploadFocusPoints(projected);

        // Mirrors PostChain#process, with our accumulation pass spliced in ahead of the chain and
        // its output handed to the chain as the dragonapi:stencil external target.
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> mainHandle = frame.importExternal("main", main);
        ResourceHandle<RenderTarget> stencilHandle = frame.createInternal(
                "dragonapi_stencil",
                new RenderTargetDescriptor(width, height, false, new Vector4f(0, 0, 0, 0), GpuFormat.RGBA8_UNORM));

        FramePass stencilPass = frame.addPass("dragonapi_reika_stencil");
        ResourceHandle<RenderTarget> stencilOut = stencilPass.readsAndWrites(stencilHandle);
        stencilPass.executes(() -> drawStencil(stencilOut.get()));

        chain.addToFrame(frame, width, height, new StencilTargetBundle(mainHandle, stencilOut));
        frame.execute(GraphicsResourceAllocator.UNPOOLED);

        if (focusUbo != null)
            focusUbo.rotate();
    }

    /**
     * Project each point to screen space and size its falloff.
     *
     * <p>The 1.7.10 shader did the projection itself (lib_geometry's {@code getScreenPos}) because
     * it had the point's world coords as uniforms; here the CPU owns the points, so it projects them
     * and sends screen-space values straight through.</p>
     *
     * @return one {@code {u, v, radius, strength}} per visible point, matching the shader's vec4
     */
    private static List<float[]> project(Matrix4fc modelView, Matrix4fc projection, Vec3 cameraPos, int width, int height) {
        List<float[]> out = new ArrayList<>(POINTS.size());
        for (Focus f : POINTS) {
            Vector4f v = new Vector4f(
                    (float) (f.position.x - cameraPos.x),
                    (float) (f.position.y - cameraPos.y),
                    (float) (f.position.z - cameraPos.z),
                    1);
            v.mul(modelView);
            v.mul(projection);
            if (v.w <= 1.0E-4F) // behind the near plane; no meaningful screen position
                continue;

            // NDC -> the UV convention of core/screenquad's texCoord (origin bottom-left), so no flip.
            float u = (v.x / v.w) * 0.5F + 0.5F;
            float vv = (v.y / v.w) * 0.5F + 0.5F;

            // Squared, as the original's "distance" uniform was. It makes the radius fall off as
            // 1/distance, i.e. the point shrinks with range the way a real object would.
            double distSq = Math.max(0.01, f.position.distanceToSqr(cameraPos));

            // The 1.7.10 falloff hit zero at 1.5 - 5*r^2*distSq/size = 0; solving for r gives the
            // radius that makes reika_stencil.fsh's normalised form identical to the original.
            float radius = (float) Math.sqrt(0.3 * f.size / distSq);
            if (radius < 1.0E-4F)
                continue;

            // Skip points whose falloff cannot touch the screen. Radius is x-normalised, matching
            // the shader's aspect correction, so the vertical bound has to be scaled back up.
            float vRadius = radius * width / height;
            if (u < -radius || u > 1 + radius || vv < -vRadius || vv > 1 + vRadius)
                continue;

            out.add(new float[]{u, vv, radius, Math.min(1, f.strength)});
        }
        return out;
    }

    private static void uploadFocusPoints(List<float[]> projected) {
        if (focusUbo == null)
            focusUbo = new MappableRingBuffer(() -> "DragonAPI FocusPoints", UBO_USAGE, UBO_SIZE);

        try (GpuBufferSlice.MappedView view = focusUbo.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putIVec4(projected.size(), 0, 0, 0);
            for (float[] p : projected)
                builder.putVec4(p[0], p[1], p[2], p[3]);
            // The tail is left as-is; the shader only reads up to FocusCount.
        }
    }

    private static void drawStencil(RenderTarget target) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass pass = encoder.createRenderPass(
                () -> "DragonAPI focus accumulation",
                target.getColorTextureView(),
                Optional.of(new Vector4f(0, 0, 0, 0)))) {
            pass.setPipeline(DragonShaderPipelines.REIKA_STENCIL);
            RenderSystem.bindDefaultUniforms(pass); // Globals, for ScreenSize
            pass.setUniform("FocusPoints", focusUbo.currentBuffer());
            pass.draw(3, 1, 0, 0); // core/screenquad builds the fullscreen triangle from gl_VertexID
        }
    }

    /** Supplies the chain with the main target plus our accumulation buffer. */
    private record StencilTargetBundle(ResourceHandle<RenderTarget> main,
                                       ResourceHandle<RenderTarget> stencil) implements PostChain.TargetBundle {

        @Override
        public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
            // The chain writes back the handles it produced. Ours are read-only inputs to it, and the
            // final blit lands in the real main target, so there is nothing to carry forward.
        }

        @Override
        public @Nullable ResourceHandle<RenderTarget> get(Identifier id) {
            if (id.equals(PostChain.MAIN_TARGET_ID))
                return main;
            return id.equals(STENCIL_TARGET_ID) ? stencil : null;
        }
    }
}
