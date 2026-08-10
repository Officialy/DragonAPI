package reika.dragonapi.instantiable.rendering.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * V33a {@code Reika.DragonAPI.Instantiable.Rendering.StructureRenderer}: the rotatable multiblock
 * preview the Chromic Lexicon draws on its structure pages.
 *
 * <p>Upstream held a {@code FilledBlockArray} and a {@code RenderBlocks} bound to a fake world, and
 * drew straight into GUI space with {@code GL11}. Neither survives into 26.2: structures are NBT
 * templates now, and the GUI's matrix stack is two-dimensional. The state upstream kept -- rotation,
 * the current slice, and the per-position display overrides -- lives on here unchanged; the drawing
 * itself is handed to {@link StructurePipRenderer} through a picture-in-picture element.
 */
public final class StructureRenderer {

	/**
	 * One block of the preview.
	 *
	 * @param icon  what the flat slice view draws for this block; the 3D view uses {@code state}
	 * @param alpha whether this block is shared with the tier below, which the 3D view blends
	 *              additively so only the newly required blocks read as solid
	 */
	public record Entry(BlockPos pos, BlockState state, ItemStack icon, boolean alpha) {}

	/** V33a {@code resetRotation}. */
	private static final double DEFAULT_RX = -30;
	private static final double DEFAULT_RY = 45;
	private static final double DEFAULT_RZ = 0;

	/** V33a {@code draw3D}: {@code GL11.glScaled(-d*s, ...)} with {@code s = 12}. */
	private static final double BLOCK_PIXELS = 12;

	private final List<Entry> source;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final int minY;

	private final Map<BlockPos, BlockState> overrides = new HashMap<>();
	private final Map<Block, Supplier<BlockState>> blockHooks = new HashMap<>();
	private final Map<Block, Consumer<BlockEntity>> blockEntityHooks = new HashMap<>();

	/**
	 * One block entity per position that needs one, built once and reused. They are never placed and
	 * never ticked -- upstream does not tick them either, so anything animated off a tick counter
	 * stands still while anything animated off wall-clock time moves.
	 */
	private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
	private final Set<BlockPos> blockEntitiesWithout = new HashSet<>();

	private List<Entry> resolved;

	private double rx;
	private double ry;
	private double rz;
	private int secY;

	public StructureRenderer(List<Entry> blocks, int sizeX, int sizeY, int sizeZ) {
		source = List.copyOf(blocks);
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		minY = source.stream().mapToInt(e -> e.pos().getY()).min().orElse(0);
		this.reset();
	}

	public void resetRotation() {
		rx = DEFAULT_RX;
		ry = DEFAULT_RY;
		rz = DEFAULT_RZ;
	}

	public void rotate(double x, double y, double z) {
		rx += x;
		ry += y;
		rz += z;
	}

	public void reset() {
		this.resetRotation();
		this.resetStepY();
	}

	public void resetStepY() {
		secY = 0;
	}

	public void setSlice(int slice) {
		secY = Math.clamp(slice, 0, sizeY - 1);
	}

	public void incrementStepY() {
		if (secY < sizeY - 1)
			secY++;
	}

	public void decrementStepY() {
		if (secY > 0)
			secY--;
	}

	public int getCurrentSlice() {
		return secY;
	}

	public int getSizeX() {
		return sizeX;
	}

	public int getSizeY() {
		return sizeY;
	}

	public int getSizeZ() {
		return sizeZ;
	}

	/** V33a {@code addOverride(x, y, z, is)}: draw something else at one position. */
	public void addOverride(BlockPos pos, BlockState state) {
		overrides.put(pos, state);
		resolved = null;
	}

	/**
	 * V33a {@code addBlockHook}: draw something else everywhere a given block appears. The
	 * replacement is a supplier because upstream's hooks are not constant -- the casting structure's
	 * runes cycle through the sixteen elements on a timer, which is a hook that returns a different
	 * block every four seconds.
	 */
	public void addBlockHook(Block block, Supplier<BlockState> state) {
		blockHooks.put(block, state);
		resolved = null;
	}

	/**
	 * Runs against the block entity standing in for a given block every frame, before its renderer
	 * extracts. This is where a preview sets up display-only state -- an element to show, an animation
	 * phase -- on an instance that was never placed in a world and so has none of its own.
	 */
	public void addBlockEntityHook(Block block, Consumer<BlockEntity> hook) {
		blockEntityHooks.put(block, hook);
	}

	private List<Entry> blocks() {
		if (resolved != null)
			return resolved;
		if (overrides.isEmpty() && blockHooks.isEmpty()) {
			resolved = source;
			return resolved;
		}
		List<Entry> out = new ArrayList<>(source.size());
		for (Entry e : source) {
			BlockState state = overrides.get(e.pos());
			if (state == null) {
				Supplier<BlockState> hook = blockHooks.get(e.state().getBlock());
				if (hook != null)
					state = hook.get();
			}
			out.add(state == null ? e
					: new Entry(e.pos(), state, new ItemStack(state.getBlock()), e.alpha()));
		}
		out = List.copyOf(out);
		// A supplier hook can return something different next frame, so the resolved list is only
		// cacheable when every hook is a fixed override.
		if (blockHooks.isEmpty())
			resolved = out;
		return out;
	}

	/**
	 * V33a's discrete size tiers. The preview never fits itself to the pane: a structure of a given
	 * size is always drawn at the same scale, so two tiers of the same multiblock stay comparable.
	 *
	 * <p>Upstream keys this off {@code max(sizeY, hypot(sizeX, array.getMaxZ()))}, and its arrays are
	 * centred on the origin, so {@code getMaxZ()} is a half-extent rather than a full size. That half
	 * is reproduced here; reading it as the full depth would push most structures a tier smaller.
	 */
	private double sizeTier() {
		double zHalf = (sizeZ - 1) / 2D;
		double max = Math.max(sizeY, Math.hypot(sizeX, zHalf));
		if (max >= 24)
			return 0.5;
		if (max >= 21)
			return 0.625;
		if (max >= 18)
			return 0.675;
		if (max >= 14)
			return 0.8;
		if (max >= 12)
			return 0.95;
		if (max >= 10)
			return 1.2;
		if (max >= 8)
			return 1.5;
		if (max >= 4)
			return 1.75;
		return 2;
	}

	/**
	 * Submits the 3D view. The rectangle is the viewport the preview is drawn into and it is clipped
	 * to it; V33a sets no scissor and centres on the screen, so callers generally want the screen.
	 *
	 * <p>Block entity render states are extracted here rather than in the picture-in-picture renderer
	 * because this is the GUI's extract phase, which is where vanilla extracts them too.
	 */
	public void draw3D(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float partialTick) {
		List<Entry> blocks = this.blocks();
		if (blocks.isEmpty())
			return;
		boolean hasAlpha = false;
		List<StructureRenderState.Block> out = new ArrayList<>(blocks.size());
		for (Entry e : blocks) {
			if (e.state().isAir())
				continue;
			hasAlpha |= e.alpha();
			out.add(new StructureRenderState.Block(e.pos(), e.state(), e.alpha()));
		}
		if (out.isEmpty())
			return;
		// A block at position p occupies [p, p+1], so the structure's centre sits half a block past
		// the middle index. Rotating about that keeps the preview from drifting as it spins.
		graphics.submitPictureInPictureRenderState(new StructureRenderState(
				List.copyOf(out), hasAlpha, this.extractBlockEntities(blocks, partialTick),
				sizeX / 2F, minY + sizeY / 2F, sizeZ / 2F,
				(float)rx, (float)ry, (float)rz,
				x0, y0, x1, y1,
				(float)(this.sizeTier() * BLOCK_PIXELS),
				graphics.peekScissorStack()));
	}

	/**
	 * V33a's TESR pass: every position whose block has a block entity with a renderer gets that
	 * renderer run over a stand-in instance, so a pylon in the guide glows and spins as it does in
	 * the world instead of standing there as a bare model.
	 *
	 * <p>The stand-ins are given the client level, as upstream gives them {@code theWorld}. That is a
	 * real level at a position the structure does not actually occupy, so anything a renderer reads
	 * from it is meaningless -- {@link #isRenderingTiles()} is set across the extract for renderers
	 * that need to know to skip those reads, which is exactly what upstream's flag of the same name is
	 * for. The light level is one such read, and is overwritten with full brightness afterwards
	 * because otherwise the preview samples whatever solid block happens to be at those coordinates.
	 */
	private List<BlockEntityRenderState> extractBlockEntities(List<Entry> blocks, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return List.of();
		BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
		List<BlockEntityRenderState> states = new ArrayList<>();
		tileRendering = true;
		renderRotationX = rx;
		renderRotationY = ry;
		renderRotationZ = rz;
		try {
			for (Entry e : blocks) {
				BlockEntity be = this.blockEntity(e, mc);
				if (be == null)
					continue;
				BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = dispatcher.getRenderer(be);
				if (renderer == null)
					continue;
				Consumer<BlockEntity> hook = blockEntityHooks.get(e.state().getBlock());
				if (hook != null)
					hook.accept(be);
				BlockEntityRenderState state = renderer.createRenderState();
				renderer.extractRenderState(be, state, partialTick, Vec3.ZERO, null);
				state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
				states.add(state);
			}
		}
		finally {
			tileRendering = false;
		}
		return List.copyOf(states);
	}

	/** The stand-in for one position, or null if that block has no block entity. */
	private BlockEntity blockEntity(Entry e, Minecraft mc) {
		BlockPos pos = e.pos();
		if (blockEntitiesWithout.contains(pos))
			return null;
		BlockEntity be = blockEntities.get(pos);
		// A supplier hook can swap the block under a position between frames, which invalidates any
		// instance built for what used to be there.
		if (be != null && be.getBlockState().getBlock() == e.state().getBlock())
			return be;
		if (!(e.state().getBlock() instanceof EntityBlock entityBlock)) {
			blockEntitiesWithout.add(pos);
			return null;
		}
		be = entityBlock.newBlockEntity(pos, e.state());
		if (be == null) {
			blockEntitiesWithout.add(pos);
			return null;
		}
		be.setLevel(mc.level);
		blockEntities.put(pos, be);
		return be;
	}

	/**
	 * V33a's static flag, for renderers that have to behave differently inside a structure preview
	 * than in the world -- typically to skip reading the level, since the stand-in block entity's
	 * position is not where the structure actually is.
	 */
	public static boolean isRenderingTiles() {
		return tileRendering;
	}

	public static double getRenderRX() {
		return renderRotationX;
	}

	public static double getRenderRY() {
		return renderRotationY;
	}

	public static double getRenderRZ() {
		return renderRotationZ;
	}

	private static boolean tileRendering;
	private static double renderRotationX;
	private static double renderRotationY;
	private static double renderRotationZ;

	/**
	 * V33a {@code drawSlice}: the flat, one-layer-at-a-time view, drawn as item icons on a grid whose
	 * cell size shrinks as the footprint grows.
	 *
	 * @param j the page's left edge, {@code k} its top -- upstream adds its own (120, 105) inset
	 */
	public void drawSlice(GuiGraphicsExtractor graphics, Font font, int j, int k, int mouseX, int mouseY) {
		int max = Math.max(sizeX, sizeZ);
		double s = 1;
		double dd = max > 16 ? Math.max(12, 28 - max) : 14;
		if (max >= 20) {
			s -= 0.05 * (max - 20);
			dd -= 0.625 * (max - 20);
		}
		int y = minY + secY;
		double midX = (sizeX - 1) / 2D;
		double midZ = (sizeZ - 1) / 2D;
		ItemStack hovered = ItemStack.EMPTY;
		graphics.pose().pushMatrix();
		graphics.pose().scale((float)s, (float)s);
		for (Entry e : this.blocks()) {
			if (e.pos().getY() != y || e.icon().isEmpty())
				continue;
			double dx = (e.pos().getX() - midX) * dd;
			double dz = (e.pos().getZ() - midZ) * dd;
			int px = (int)Math.round((j + dx + SLICE_OFFSET_X) / s);
			int py = (int)Math.round((k + dz + SLICE_OFFSET_Y) / s);
			graphics.item(e.icon(), px, py);
			int sx = (int)Math.round(px * s);
			int sy = (int)Math.round(py * s);
			if (mouseX >= sx && mouseX < sx + 16 * s && mouseY >= sy && mouseY < sy + 16 * s)
				hovered = e.icon();
		}
		graphics.pose().popMatrix();
		if (!hovered.isEmpty())
			graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	/** V33a {@code drawSlice}: {@code int ox = 120; int oy = 105;}. */
	private static final int SLICE_OFFSET_X = 120;
	private static final int SLICE_OFFSET_Y = 105;

	/**
	 * V33a {@code drawTally}: how many of each block the whole structure needs, in the stable item
	 * order upstream sorts by.
	 */
	public List<TallyEntry> tally() {
		Map<Item, TallyEntry> counts = new LinkedHashMap<>();
		for (Entry e : this.blocks()) {
			if (e.icon().isEmpty())
				continue;
			counts.merge(e.icon().getItem(), new TallyEntry(e.icon(), 1),
					(a, b) -> new TallyEntry(a.icon(), a.count() + b.count()));
		}
		List<TallyEntry> out = new ArrayList<>(counts.values());
		out.sort(Comparator.comparing(t -> t.icon().getHoverName().getString()));
		return out;
	}

	public record TallyEntry(ItemStack icon, int count) {}
}
