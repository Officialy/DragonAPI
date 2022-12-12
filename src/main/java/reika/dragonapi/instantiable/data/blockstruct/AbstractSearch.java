package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.level.ReikaBlockHelper;

import java.util.*;

public abstract class AbstractSearch {

    public final BlockPos root;
    protected final HashSet<BlockPos> searchedCoords = new HashSet<>();
    private final LinkedList<BlockPos> result = new LinkedList<>();
    public BlockBox limit = BlockBox.infinity();
    public int depthLimit = Integer.MAX_VALUE;

    public AbstractSearch(BlockPos pos) {
        root = new BlockPos(pos);
        searchedCoords.add(root);
    }

    /**
     * Note that the propagation condition must include the termination condition, or it will never be moved into!
     */
    public abstract boolean tick(Level world, PropagationCondition propagation, TerminationCondition terminate);

    /**
     * Whether a path is found or no valid paths exist.
     */
    public abstract boolean isDone();

    public abstract void clear();

    public final LinkedList<BlockPos> getResult() {
        return result;
    }

    public final Set<BlockPos> getTotalSearchedCoords() {
        return Collections.unmodifiableSet(searchedCoords);
    }

    public void complete(Level world, PropagationCondition propagation, TerminationCondition terminate) {
        while (!this.tick(world, propagation, terminate)) {

        }
    }

    protected final boolean isValidLocation(Level world, BlockPos pos, BlockPos from, PropagationCondition p, TerminationCondition c) {
        return p.isValidLocation(world, pos, from) || c.isValidTerminus(world, pos);
    }

    protected ArrayList<BlockPos> getNextSearchCoordsFor(Level world, BlockPos c) {
        return (ArrayList) CoordHelper.getAdjacentCoordinates(c);
    }

    public interface TerminationCondition {

        boolean isValidTerminus(Level world, BlockPos pos);

    }

    public interface FixedPositionTarget {

        BlockPos getTarget();

    }

    public interface PropagationCondition {

        boolean isValidLocation(Level world, BlockPos pos, BlockPos from);

    }

    public static final class LocationTerminus implements TerminationCondition, FixedPositionTarget {

        public final BlockPos target;

        public LocationTerminus(BlockPos c) {
            target = c;
        }

        @Override
        public boolean isValidTerminus(Level world, BlockPos pos) {
            return target.equals(pos);
        }

        @Override
        public BlockPos getTarget() {
            return target;
        }

    }

    public static final class CompoundPropagationCondition implements PropagationCondition {

        private final ArrayList<PropagationCondition> conditions = new ArrayList<>();

        public CompoundPropagationCondition() {

        }

        public CompoundPropagationCondition addCondition(PropagationCondition pc) {
            conditions.add(pc);
            return this;
        }

        @Override
        public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
            for (PropagationCondition pc : conditions) {
                if (!pc.isValidLocation(world, pos, from))
                    return false;
            }
            return true;
        }

    }

    public static final class AirPropagation implements PropagationCondition {

        public static final AirPropagation instance = new AirPropagation();

        private AirPropagation() {

        }

        @Override
        public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
            return world.getBlockState(pos).isAir();
        }

    }

    public static final class DirectionalPropagation implements PropagationCondition {

        public final BlockPos location;
        public final boolean requireCloser;

        public DirectionalPropagation(BlockPos c, boolean cl) {
            location = c;
            requireCloser = cl;
        }

        @Override
        public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
            int d0 = new WorldLocation(world, from).getTaxicabDistanceTo(location);
            int d1 = new WorldLocation(world, pos).getTaxicabDistanceTo(location);
            return requireCloser ? d1 < d0 : d0 < d1;
        }

    }

    public static final class WalkablePropagation implements PropagationCondition {

        public static final WalkablePropagation instance = new WalkablePropagation();

        private WalkablePropagation() {

        }

        @Override
        public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
            return PassablePropagation.instance.isValidLocation(world, pos, from) && (!PassablePropagation.instance.isValidLocation(world, pos.below(), from) || !PassablePropagation.instance.isValidLocation(world, pos.below(2), from));
        }

    }

    public static final class PassablePropagation implements PropagationCondition {

        public static final PassablePropagation instance = new PassablePropagation();

        private PassablePropagation() {

        }

        @Override
        public boolean isValidLocation(Level world, BlockPos pos, BlockPos from) {
            return !ReikaBlockHelper.isCollideable(world, pos);
        }

    }

}
