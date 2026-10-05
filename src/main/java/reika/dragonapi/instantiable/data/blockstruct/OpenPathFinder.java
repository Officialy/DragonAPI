package reika.dragonapi.instantiable.data.blockstruct;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.instantiable.data.blockstruct.AbstractSearch.PropagationCondition;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * V33a {@code OpenPathFinder}: propagation through open space between two points, within a radius of the start (at
 * least as far as the end is), treating as open whatever the {@link PassRules} allow -- soft blocks, liquids, or
 * tiny non-solid blocks (a volume of at most 1/16, or 1/128 if it has a collision box).
 */
public class OpenPathFinder implements PropagationCondition {

	public final int searchRadius;
	public final EnumSet<PassRules> rules = EnumSet.noneOf(PassRules.class);
	private final BlockPos startLocation;
	private final BlockPos endLocation;

	public static final Collection<PassRules> defaultRules = Collections.unmodifiableSet(EnumSet.of(PassRules.SOFT));

	public OpenPathFinder(BlockPos c1, BlockPos c2, int r) {
		startLocation = c1;
		endLocation = c2;
		searchRadius = Math.max(r, c2.distManhattan(c1));
	}

	@Override
	public final boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
		if (startLocation.equals(pos) || endLocation.equals(pos))
			return true;
		if (Math.abs(pos.getX()-startLocation.getX()) > searchRadius || Math.abs(pos.getY()-startLocation.getY()) > searchRadius
				|| Math.abs(pos.getZ()-startLocation.getZ()) > searchRadius)
			return false;
		return this.isValidBlock(world, pos);
	}

	protected boolean isValidBlock(Level world, BlockPos pos) {
		return isEmptyBlock(world, pos, rules);
	}

	public static boolean isEmptyBlock(Level world, BlockPos pos, Collection<PassRules> rules) {
		BlockState b = world.getBlockState(pos);
		if (rules.contains(PassRules.SMALLNONSOLID) && isSmallPassable(world, pos, b))
			return true;
		if (!rules.contains(PassRules.LIQUIDS) && ReikaBlockHelper.isLiquid(b))
			return false;
		if (rules.contains(PassRules.SOFT) && ReikaWorldHelper.softBlocks(world, pos))
			return true;
		return b.isAir();
	}

	private static boolean isSmallPassable(Level world, BlockPos pos, BlockState b) {
		double vol = ReikaBlockHelper.getBlockVolume(world, pos);
		double thresh = b.getCollisionShape(world, pos).isEmpty() ? 0.0625 : 0.0078125;
		return vol <= thresh;
	}

	public enum PassRules {
		SOFT,
		LIQUIDS,
		SMALLNONSOLID
    }
}
