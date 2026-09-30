package reika.dragonapi.interfaces.blockentity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * The cross-mod contract of ChromatiCraft's World Rift ({@code reika.chromaticraft.api.interfaces.WorldRift} extends
 * this). In 1.7.10 RotaryCraft and ElectriCraft compiled against ChromatiCraft's API package, which every mod jar
 * shipped a copy of; 26.x modules cannot share a package, and ChromatiCraft depends on those mods, so the interface
 * they test for lives here.
 *
 * <p>For every directional method the result is the object on that side of the rift's other end: calling
 * getBlockIDFrom(Direction.EAST) on a rift linked to another at 300, 50, 100 in the Nether returns the block at
 * 301, 50, 100 in the Nether. Rifts are two-directional.
 */
public interface WorldRift {

	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	@Nullable Block getBlockIDFrom(Direction dir);

	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	int getBlockMetadataFrom(Direction dir);

	/** Direction is the side it is relative to you, NOT the side of it you are asking! */
	@Nullable BlockEntity getTileEntityFrom(Direction dir);

	/** Returns the location of the other rift. */
	@Nullable WorldLocation getLinkTarget();

	/**
	 * Runs {@code action} with the far end's location, as the 1.7.10 callers did inline ({@code loc =
	 * sr.getLinkTarget(); if (loc != null) ...}). It does nothing and returns false while the rift is unlinked, while
	 * the far end is not loaded (the rift itself forwards nothing then either), or when this call chain has already
	 * passed through {@link Guard#MAX_DEPTH} rifts: two rifts whose far sides face each other recursed until the stack
	 * overflowed in 1.7.10.
	 */
	static boolean forward(WorldRift rift, java.util.function.Consumer<WorldLocation> action) {
		WorldLocation loc = rift.getLinkTarget();
		if (loc == null)
			return false;
		net.minecraft.world.level.Level world = loc.getWorld();
		if (world == null || !world.isLoaded(loc.pos))
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
