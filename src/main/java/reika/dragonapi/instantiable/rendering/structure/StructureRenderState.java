package reika.dragonapi.instantiable.rendering.structure;

import java.util.List;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * An immutable, per-frame snapshot of a structure preview, handed to the GUI's picture-in-picture
 * pass.
 *
 * <p>V33a rendered the structure straight into GUI space with raw {@code GL11} calls, which 26.2 no
 * longer allows: {@code GuiGraphicsExtractor.pose()} is a {@link org.joml.Matrix3x2fStack} and
 * carries no third dimension at all. The only way a rotatable 3D scene reaches the screen now is a
 * picture-in-picture element, which renders to its own colour and depth texture with a real
 * {@link com.mojang.blaze3d.vertex.PoseStack} and then blits that texture into the GUI layer. That
 * is what this record feeds.
 *
 * @param blocks        every block to draw, in no particular order; the depth buffer resolves overlap
 * @param hasAlpha      whether any entry is {@link Block#alpha}, so the renderer can skip a pass
 * @param blockEntities already-extracted renderer states, each carrying the position it belongs at
 * @param entities      already-extracted entity states, for structures partly defined by entities
 * @param midX       the structure centre, subtracted before rotating so it spins about itself
 * @param rotX       V33a's {@code rx}/{@code ry}/{@code rz}, in degrees, applied X then Y then Z
 * @param scale      pixels per block; V33a's discrete size tier multiplied by its {@code s = 12}
 */
public record StructureRenderState(
		List<StructureRenderState.Block> blocks,
		boolean hasAlpha,
		List<BlockEntityRenderState> blockEntities,
		List<StructureRenderState.PlacedEntity> entities,
		float midX,
		float midY,
		float midZ,
		float rotX,
		float rotY,
		float rotZ,
		int x0,
		int y0,
		int x1,
		int y1,
		float scale,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

	/**
	 * One block of the preview.
	 *
	 * @param alpha whether this block belongs to V33a's "alpha" set -- the blocks a structure shares
	 *              with the tier below it, which upstream draws in a second, additively blended pass
	 *              so that only the newly required blocks read as solid
	 */
	public record Block(BlockPos pos, BlockState state, boolean alpha) {}

	/**
	 * An entity standing in the structure, already extracted, with the point it is drawn at. V33a's
	 * dimension portal is the case this exists for: eight ender crystals on a bedrock ring.
	 */
	public record PlacedEntity(EntityRenderState state, double x, double y, double z) {}

	public StructureRenderState(
			List<StructureRenderState.Block> blocks,
			boolean hasAlpha,
			List<BlockEntityRenderState> blockEntities,
			List<StructureRenderState.PlacedEntity> entities,
			float midX,
			float midY,
			float midZ,
			float rotX,
			float rotY,
			float rotZ,
			int x0,
			int y0,
			int x1,
			int y1,
			float scale,
			@Nullable ScreenRectangle scissorArea) {
		this(blocks, hasAlpha, blockEntities, entities, midX, midY, midZ, rotX, rotY, rotZ,
				x0, y0, x1, y1, scale, scissorArea,
				PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
	}
}
