package reika.dragonapi.instantiable.rendering.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.pipeline.PipelineModifier;
import net.neoforged.neoforge.client.pipeline.RegisterPipelineModifiersEvent;

import reika.dragonapi.DragonAPI;

/**
 * Draws a {@link StructureRenderState} into its own off-screen colour and depth texture, which the
 * GUI layer then blits. This is the 26.2 replacement for V33a's {@code StructureRenderer.draw3D},
 * whose raw {@code GL11} matrix stack has no equivalent in modern GUI space.
 *
 * <p>The base class already translates to the centre of the viewport and applies
 * {@code scale(s, s, -s)}, where {@code s} is the GUI scale multiplied by
 * {@link StructureRenderState#scale()}. That leaves model +X pointing right and model +Y pointing
 * <em>down</em>, because the GUI's orthographic projection has an inverted Y. The extra
 * {@code scale(1, -1, -1)} below finishes the orientation; it is the same step every vanilla
 * picture-in-picture renderer takes (see {@code GuiEntityRenderer} and NeoForge's own stencil
 * sample), and it is paired with the projection's reversed near/far to come out the right way round.
 * Do not reason about it in isolation.
 *
 * <p>V33a used {@code glScaled(-d*s, -d*s, -d*s)} instead, which additionally mirrors the structure
 * in X. That mirror is not reproduced: most ChromatiCraft multiblocks are symmetric about X, and a
 * view you can spin freely makes the difference invisible on the ones that are not.
 */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class StructurePipRenderer extends PictureInPictureRenderer<StructureRenderState> {

	/**
	 * V33a shades the shared blocks of an upgrade structure with {@code BlendMode.ADDITIVE2}, which
	 * is {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} -- exactly {@link BlendFunction#LIGHTNING}. There
	 * is no stock render type that both accepts block-model geometry and blends that way, so the
	 * block sheet's own pipeline is rebuilt with that blend for the duration of the second pass.
	 */
	public static final ResourceKey<PipelineModifier> ADDITIVE = ResourceKey.create(
			PipelineModifier.MODIFIERS_KEY,
			Identifier.fromNamespaceAndPath(DragonAPI.MODID, "structure_additive"));

	@SubscribeEvent
	public static void registerRenderer(RegisterPictureInPictureRenderersEvent event) {
		event.register(StructureRenderState.class, StructurePipRenderer::new);
	}

	@SubscribeEvent
	public static void registerPipelines(RegisterPipelineModifiersEvent event) {
		event.register(ADDITIVE, (pipeline, name) -> {
			// Swap the blend function and nothing else. The single-argument ColorTargetState
			// constructor also fixes the format to RGBA8_UNORM and the write mask to WRITE_ALL, which
			// would silently change whatever the sheet's own pipeline declared -- and the result is
			// cached per pipeline, so it would not correct itself later.
			ColorTargetState existing = pipeline.getColorTargetState();
			ColorTargetState additive = existing == null
					? new ColorTargetState(BlendFunction.LIGHTNING)
					: new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), existing.format(),
							existing.writeMask());
			return pipeline.toBuilder().withLocation(name).withColorTargetState(additive).build();
		});
	}

	private final List<BlockStateModelPart> scratch = new ArrayList<>();
	private final RandomSource random = RandomSource.create();

	@Override
	public Class<StructureRenderState> getRenderStateClass() {
		return StructureRenderState.class;
	}

	@Override
	protected String getTextureLabel() {
		return "structure preview";
	}

	/**
	 * The base class puts the origin at the bottom of the viewport, which suits an entity standing on
	 * a floor. A structure is centred on itself, so it wants the middle.
	 */
	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2F;
	}

	@Override
	protected void renderToTexture(StructureRenderState state, PoseStack pose, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		mc.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

		pose.scale(1, -1, -1);
		pose.mulPose(Axis.XP.rotationDegrees(state.rotX()));
		pose.mulPose(Axis.YP.rotationDegrees(state.rotY()));
		pose.mulPose(Axis.ZP.rotationDegrees(state.rotZ()));
		pose.translate(-state.midX(), -state.midY(), -state.midZ());

		BlockStateModelSet models = mc.getModelManager().getBlockStateModelSet();
		BlockColors colors = mc.getBlockColors();

		submitPass(state, pose, collector, models, colors, false);

		if (state.hasAlpha()) {
			// The solid pass has to reach the framebuffer before the additive one blends against it.
			mc.gameRenderer.featureRenderDispatcher().renderAllFeatures((SubmitNodeStorage)collector);
			RenderSystem.pushPipelineModifier(ADDITIVE);
			try {
				submitPass(state, pose, collector, models, colors, true);
				mc.gameRenderer.featureRenderDispatcher().renderAllFeatures((SubmitNodeStorage)collector);
			}
			finally {
				RenderSystem.popPipelineModifier();
			}
		}

		submitBlockEntities(state, pose, collector, mc);
	}

	/**
	 * V33a's TESR pass. The states were extracted in {@code StructureRenderer.draw3D}, during the
	 * GUI's extract phase; all that is left here is to put each one at its block and submit it.
	 */
	private static void submitBlockEntities(StructureRenderState state, PoseStack pose,
			SubmitNodeCollector collector, Minecraft mc) {
		if (state.blockEntities().isEmpty())
			return;
		BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
		CameraRenderState camera = new CameraRenderState();
		for (BlockEntityRenderState beState : state.blockEntities()) {
			BlockPos pos = beState.blockPos;
			pose.pushPose();
			pose.translate(pos.getX(), pos.getY(), pos.getZ());
			dispatcher.submit(beState, pose, collector, camera);
			pose.popPose();
		}
	}

	private void submitPass(StructureRenderState state, PoseStack pose, SubmitNodeCollector collector,
			BlockStateModelSet models, BlockColors colors, boolean alpha) {
		for (StructureRenderState.Block block : state.blocks()) {
			if (block.alpha() != alpha)
				continue;
			BlockStateModel model = models.get(block.state());
			BlockPos pos = block.pos();
			random.setSeed(Mth.getSeed(pos));
			scratch.clear();
			model.collectParts(random, scratch);
			if (scratch.isEmpty())
				continue;
			RenderType type = model.hasMaterialFlag(1)
					? Sheets.translucentBlockItemSheet()
					: Sheets.cutoutBlockItemSheet();
			pose.pushPose();
			pose.translate(pos.getX(), pos.getY(), pos.getZ());
			// submitBlockModel keeps the list it is handed, so each block needs its own copy.
			collector.submitBlockModel(pose, type, List.copyOf(scratch), tints(colors, block.state()),
					LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}

	/**
	 * Grass, leaves and the like colour their tinted quads from the biome they stand in. There is no
	 * biome here, so this takes the tint source's stateless colour -- the same value the inventory
	 * and the item frame use.
	 */
	private static int[] tints(BlockColors colors, BlockState state) {
		List<BlockTintSource> sources = colors.getTintSources(state);
		if (sources.isEmpty())
			return NO_TINTS;
		int[] tints = new int[sources.size()];
		for (int i = 0; i < tints.length; i++)
			tints[i] = sources.get(i).color(state);
		return tints;
	}

	private static final int[] NO_TINTS = new int[0];
}
