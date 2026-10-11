package reika.dragonapi.auxiliary;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockBox;

/**
 * A manually executed task over a detached coordinate snapshot. Capture an array while
 * exclusively owning it (on its server thread when attached). LOAD and pure ITERATE callbacks
 * may run on a worker; FILL capture and execution require the world's owning server thread.
 * No operation mutates the source array, starts a thread or implicitly queues world changes.
 */
public class BlockArrayComputer implements Runnable {
    private final List<BlockPos> positions;
    private final BlockBox bounds;
    private Operation op;
    private LoadInput load;
    private FillInput fill;
    private Consumer<BlockPos> iterator;
    private boolean running;
    private volatile Result result;

    public BlockArrayComputer(BlockArray array) {
        Objects.requireNonNull(array, "Source array");
        if (array.getWorld() instanceof ServerLevel level)
            requireServerThread(level);
        positions = immutablePositions(array.list());
        bounds = new BlockBox(array.bounds.minX, array.bounds.minY, array.bounds.minZ,
                array.bounds.maxX, array.bounds.maxY, array.bounds.maxZ);
    }

    /** Copies the coordinate NBT now; LOAD decodes it into a detached result, retaining array bounds. */
    public synchronized BlockArrayComputer setLoad(String label, CompoundTag tag) {
        requireIdle();
        load = new LoadInput(Objects.requireNonNull(label, "Coordinate label"),
                Objects.requireNonNull(tag, "Coordinate NBT").copy());
        return select(Operation.LOAD);
    }

    /** The callback must be world-free when this task is executed off-thread. Order is retained. */
    public synchronized BlockArrayComputer setIteration(Consumer<BlockPos> visitor) {
        requireIdle();
        iterator = Objects.requireNonNull(visitor, "Coordinate visitor");
        return select(Operation.ITERATE);
    }

    /**
     * Captures expected states from already loaded chunks. Permission is checked for every
     * coordinate at execution time before any write. The policy must not mutate the world and
     * must enforce the caller's actor/claim rules; this utility cannot infer those rules.
     * Queued work rejects stale states instead of overwriting changes made after capture.
     */
    public synchronized BlockArrayComputer setFill(ServerLevel level, BlockState state, FillAuthorization authorization) {
        requireIdle();
        Objects.requireNonNull(level, "Fill world");
        requireServerThread(level);
        Objects.requireNonNull(state, "Fill state");
        Objects.requireNonNull(authorization, "Fill authorization");
        List<BlockState> expected = positions.stream().map(position -> {
            requireLoadedPosition(level, position);
            return level.getBlockState(position);
        }).toList();
        fill = new FillInput(level, state, expected, authorization);
        return select(Operation.FILL);
    }

    /** Retained entry point: selecting an operation requires its corresponding configured input. */
    public synchronized BlockArrayComputer setOperation(Operation operation) {
        requireIdle();
        return select(Objects.requireNonNull(operation, "Operation"));
    }

    private BlockArrayComputer select(Operation operation) {
        op = operation;
        result = null;
        return this;
    }

    private void requireIdle() {
        if (running)
            throw new IllegalStateException("Cannot configure or concurrently execute a running block array task");
    }

    /** Execute manually, through a caller-owned executor, or explicitly on the owning server. */
    @Override
    public void run() {
        Operation operation;
        synchronized (this) {
            requireIdle();
            operation = op;
            if (operation == null || switch (operation) {
                case LOAD -> load == null;
                case FILL -> fill == null;
                case ITERATE -> iterator == null;
            })
                throw new IllegalStateException("Configure inputs for the block array operation before running it: " + operation);
            running = true;
            result = null;
        }
        Result completed = null;
        try {
            completed = switch (operation) {
                case LOAD -> loadCoordinates();
                case FILL -> fillWorld();
                case ITERATE -> iterateCoordinates();
            };
        } finally {
            synchronized (this) {
                result = completed;
                running = false;
            }
        }
    }

    /** A successfully completed, immutable result. Failed or unexecuted tasks have no result. */
    public Result getResult() {
        Result completed = result;
        if (completed == null)
            throw new IllegalStateException("Block array task has not completed successfully");
        return completed;
    }

    private Result loadCoordinates() {
        BlockArray decoded = new BlockArray();
        decoded.bounds = bounds;
        decoded.load(load.label(), load.tag());
        return new Result(Operation.LOAD, decoded.list(), 0);
    }

    private Result iterateCoordinates() {
        positions.forEach(iterator);
        return new Result(Operation.ITERATE, positions, 0);
    }

    private Result fillWorld() {
        ServerLevel level = fill.level();
        requireServerThread(level);
        for (int i = 0; i < positions.size(); i++) {
            BlockPos position = positions.get(i);
            requireExpectedState(fill, position, i, 0);
            if (!fill.authorization().mayReplace(level, position, fill.expected().get(i), fill.state()))
                throw new SecurityException("Block array fill denied at " + position + "; no blocks changed");
        }
        // Recheck after all authorization callbacks, before the first native write.
        for (int i = 0; i < positions.size(); i++)
            requireExpectedState(fill, positions.get(i), i, 0);

        int changed = 0;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos position = positions.get(i);
            // Native neighbor updates may affect later targets; never overwrite those new states.
            requireExpectedState(fill, position, i, changed);
            if (!fill.expected().get(i).equals(fill.state())) {
                if (!level.setBlock(position, fill.state(), Block.UPDATE_ALL))
                    throw new IllegalStateException("Native block array fill failed at " + position
                            + " after " + changed + " changes");
                changed++;
            }
        }
        return new Result(Operation.FILL, positions, changed);
    }

    private static void requireExpectedState(FillInput input, BlockPos position, int index, int changed) {
        requireLoadedPosition(input.level(), position);
        if (!input.level().getBlockState(position).equals(input.expected().get(index)))
            throw new IllegalStateException("Stale block array fill at " + position + " after " + changed + " changes");
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread())
            throw new IllegalStateException("Block array world access requires the owning server thread");
    }

    private static void requireLoadedPosition(ServerLevel level, BlockPos position) {
        if (!level.isInValidBounds(position) || !level.getWorldBorder().isWithinBounds(position))
            throw new IllegalArgumentException("Block array target is outside world bounds: " + position);
        if (level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) == null)
            throw new IllegalStateException("Block array target chunk is not loaded: " + position);
    }

    private static List<BlockPos> immutablePositions(List<BlockPos> positions) {
        return positions.stream().map(BlockPos::immutable).toList();
    }

    private record LoadInput(String label, CompoundTag tag) {}
    private record FillInput(ServerLevel level, BlockState state, List<BlockState> expected,
                             FillAuthorization authorization) {}

    public record Result(Operation operation, List<BlockPos> positions, int changedBlocks) {
        public Result {
            Objects.requireNonNull(operation, "Result operation");
            positions = immutablePositions(positions);
        }
    }

    @FunctionalInterface
    public interface FillAuthorization {
        boolean mayReplace(ServerLevel level, BlockPos position, BlockState previous, BlockState replacement);
    }

    public enum Operation { LOAD, FILL, ITERATE }
}
