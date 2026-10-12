package reika.dragonapi.interfaces.blockentity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * The cross-mod contract of ChromatiCraft's World Rift ({@code reika.chromaticraft.api.interfaces.WorldRift} extends
 * this). In 1.7.10 RotaryCraft and ElectriCraft compiled against ChromatiCraft's API package, which every mod jar
 * shipped a copy of; 26.x modules cannot share a package, and ChromatiCraft depends on those mods, so the interface
 * they test for lives here.
 *
 * <p>V33a block/state reads mirror the requested face at the far end, while tile-entity reads use the requested
 * face itself. For a far end at 300, 50, 100, EAST reads the block/state at 299, 50, 100 and the tile entity at
 * 301, 50, 100. Rifts are two-directional.
 */
public interface WorldRift {

	/** The block on the far end's opposite face, or null if the target world cannot be resolved. */
	default @Nullable Block getBlockIDFrom(Direction dir) {
		BlockState state = this.getBlockStateFrom(dir);
		return state != null ? state.getBlock() : null;
	}

	/** Complete state on the far end's opposite face; null while unlinked or the target world is absent. */
	@Nullable BlockState getBlockStateFrom(Direction dir);

	/** The tile entity on the far end's requested face, or null if it is absent or unavailable. */
	@Nullable BlockEntity getTileEntityFrom(Direction dir);

	/** Returns the location of the other rift. */
	@Nullable WorldLocation getLinkTarget();

	/**
	 * Runs {@code action} with the far end's location, as the 1.7.10 callers did inline ({@code loc =
	 * sr.getLinkTarget(); if (loc != null) ...}); whatever the action reads at the far end loads on demand, as it did
	 * then. It does nothing and returns false while the rift is unlinked or its far world is absent, or when this call
	 * chain has already passed through {@link Guard#MAX_DEPTH} rifts: two rifts whose far sides face each other
	 * recursed until the stack overflowed in 1.7.10.
	 */
	static boolean forward(WorldRift rift, java.util.function.Consumer<WorldLocation> action) {
		WorldLocation loc = rift.getLinkTarget();
		if (loc == null)
			return false;
		net.minecraft.world.level.Level world = loc.getWorld();
		if (world == null)
			return false;
		int[] depth = Guard.DEPTH.get();
		if (depth[0] >= Guard.MAX_DEPTH)
			return false;
		depth[0]++;
		try {
			action.accept(loc);
		}
		finally {
			depth[0]--;
		}
		return true;
	}

	/** The per-thread count of rifts the current call chain is inside. */
	final class Guard {
		public static final int MAX_DEPTH = 8;
		private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

		private Guard() {}
	}

}
