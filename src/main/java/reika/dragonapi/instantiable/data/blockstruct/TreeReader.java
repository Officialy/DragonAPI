/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.blockstruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.instantiable.data.immutable.BlockKey;

/**
 * Flood-fill scanner that collects one whole tree (logs + natural leaves) into a BlockArray.
 *
 * <p>26.2 port notes: the legacy version identified trees through TreeType/ReikaTreeHelper (a
 * hardcoded per-species block+meta list) and ModWoodList (mod trees). Here a tree is the connected
 * set of the SAME log block as the trunk plus any non-persistent {@code minecraft:leaves}-tagged
 * leaves -- tag-driven, so correctly-tagged modded trees work without a registry. The legacy
 * per-species scan depths and the log->sapling mapping are kept for the vanilla species (new-in-
 * modern species get the legacy default depth of 12 unless obviously large). The ChromatiCraft
 * dye/rainbow-tree handling is gated out (mod not ported).</p>
 */
public final class TreeReader extends BlockArray {

	// CHROMA-PORT: dye/rainbow tree detection (dyeLeafID/rainbowLeafID/... + isDyeTree/isRainbowTree).

	/** Legacy ReikaTreeHelper.TREE_MIN_LOG / TREE_MIN_LEAF. */
	public static final int TREE_MIN_LOG = 2;
	public static final int TREE_MIN_LEAF = 5;

	private static final Map<Block, Block> SAPLINGS = new HashMap<>();
	private static final Map<Block, Integer> SCAN_DEPTH = new HashMap<>();

	static {
		SAPLINGS.put(Blocks.OAK_LOG, Blocks.OAK_SAPLING);
		SAPLINGS.put(Blocks.BIRCH_LOG, Blocks.BIRCH_SAPLING);
		SAPLINGS.put(Blocks.SPRUCE_LOG, Blocks.SPRUCE_SAPLING);
		SAPLINGS.put(Blocks.JUNGLE_LOG, Blocks.JUNGLE_SAPLING);
		SAPLINGS.put(Blocks.ACACIA_LOG, Blocks.ACACIA_SAPLING);
		SAPLINGS.put(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_SAPLING);
		SAPLINGS.put(Blocks.CHERRY_LOG, Blocks.CHERRY_SAPLING);
		SAPLINGS.put(Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_SAPLING);
		SAPLINGS.put(Blocks.MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE);

		// Legacy getMaxDepthFromTreeType (vanilla species); post-1.7 species get sensible analogues.
		SCAN_DEPTH.put(Blocks.OAK_LOG, 18);
		SCAN_DEPTH.put(Blocks.BIRCH_LOG, 6);
		SCAN_DEPTH.put(Blocks.SPRUCE_LOG, 48);
		SCAN_DEPTH.put(Blocks.JUNGLE_LOG, 36);
		SCAN_DEPTH.put(Blocks.ACACIA_LOG, 12);
		SCAN_DEPTH.put(Blocks.DARK_OAK_LOG, 12);
		SCAN_DEPTH.put(Blocks.CHERRY_LOG, 18);
		SCAN_DEPTH.put(Blocks.PALE_OAK_LOG, 12);
		SCAN_DEPTH.put(Blocks.MANGROVE_LOG, 30);
	}

	private int leafCount;
	private int logCount;

	/** The trunk log block this reader is matching; null = no tree set. */
	private Block logBlock;
	/** First leaf block encountered (for drops/inspection). */
	private Block leafBlock;

	private boolean stopIfValid = false;

	public TreeReader() {
		super();
	}

	public TreeReader setStopIfValid() {
		stopIfValid = true;
		return this;
	}

	/** Modern replacement for {@code setTree(TreeType)}: match this log block, with its species scan depth. */
	public void setTree(Block log) {
		logBlock = log;
		if (log != null)
			maxDepth = SCAN_DEPTH.getOrDefault(log, 12);
	}

	/** The log block of a scannable tree at pos, or null (modern {@code ReikaTreeHelper.getTree}). */
	public static Block getTreeLog(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.is(BlockTags.LOGS) ? state.getBlock() : null;
	}

	public void addTree(Level world, BlockPos pos) {
		HashSet<BlockPos> search = new HashSet<>();
		HashSet<BlockPos> failed = new HashSet<>();
		HashSet<BlockPos> next = new HashSet<>();

		int iterations = 0;

		this.validateAndAdd(world, pos, search, failed);

		while (!search.isEmpty() && iterations < maxDepth) {
			iterations++;

			Iterator<BlockPos> it = search.iterator();
			while (it.hasNext()) {
				BlockPos c = it.next();
				if (bounds.isBlockInside(c)) {
					this.addBlockCoordinate(c);

					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx == 0 && dy == 0 && dz == 0)
									continue;

								BlockPos c2 = c.offset(dx, dy, dz);
								if (!search.contains(c2) && !next.contains(c2) && !this.containsKey(c2) && !failed.contains(c2)) {
									this.validateAndAdd(world, c2, next, failed);
								}
							}
						}
					}
				}
				it.remove();
			}

			if (stopIfValid && this.isValidTree())
				return;

			search.addAll(next);
			next.clear();
		}
	}

	private boolean isTree(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (logBlock != null && state.is(logBlock)) {
			logCount++;
			return true;
		}
		// Natural (non-player-placed) leaves only -- the modern equivalent of the legacy
		// getLeafMetadatas() natural-meta filter.
		if (state.is(BlockTags.LEAVES) && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT))) {
			if (leafBlock == null)
				leafBlock = state.getBlock();
			leafCount++;
			return true;
		}
		return false;
	}

	private void validateAndAdd(Level world, BlockPos pos, HashSet<BlockPos> search, HashSet<BlockPos> failed) {
		BlockPos c = pos.immutable();
		if (this.isTree(world, c)) {
			search.add(c);
		}
		else {
			failed.add(c);
		}
	}

	public int getNumberLeaves() {
		return leafCount;
	}

	public int getNumberLogs() {
		return logCount;
	}

	public void reset() {
		logCount = 0;
		leafCount = 0;
		logBlock = null;
		leafBlock = null;
	}

	/** The sapling that regrows this tree, or null for unmapped (e.g. untagged modded) logs. */
	public BlockKey getSapling() {
		Block s = logBlock != null ? SAPLINGS.get(logBlock) : null;
		return s != null ? new BlockKey(s) : null;
	}

	public boolean isValidTree() {
		return this.getNumberLeaves() >= TREE_MIN_LEAF && this.getNumberLogs() >= TREE_MIN_LOG;
	}

	/** The trunk log block (modern stand-in for the legacy {@code getTreeType()}). */
	public Block getTreeLog() {
		return logBlock;
	}

	public Block getLeafBlock() {
		return leafBlock;
	}

	@Override
	protected BlockArray instantiate() {
		return new TreeReader();
	}

	@Override
	public void copyTo(BlockArray cp) {
		super.copyTo(cp);
		TreeReader copy = (TreeReader) cp;

		copy.leafCount = leafCount;
		copy.logCount = logCount;

		copy.logBlock = logBlock;
		copy.leafBlock = leafBlock;
		copy.stopIfValid = stopIfValid;
	}

}
