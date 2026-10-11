package reika.dragonapi.auxiliary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import static org.junit.jupiter.api.Assertions.*;

class BlockArrayComputerTest {
    @Test void workerIterationRetainsCapturedMembershipAndImmutableCoordinates() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(-8, 4, 12);
        List<BlockPos> original = List.of(mutable.immutable(), new BlockPos(5, -2, 3));
        BlockArray source = new BlockArray(List.of(mutable, original.get(1)));
        List<BlockPos> visited = new ArrayList<>();
        Thread owner = Thread.currentThread();
        BlockArrayComputer task = source.getComputer().setIteration(position -> {
            assertNotSame(owner, Thread.currentThread());
            assertFalse(position instanceof BlockPos.MutableBlockPos);
            visited.add(position);
        });
        mutable.set(100, 100, 100);
        source.clear(); source.addBlockCoordinate(BlockPos.ZERO);
        CompletableFuture.runAsync(task).join();
        assertEquals(original, visited);
        assertEquals(original, task.getResult().positions());
        assertEquals(List.of(BlockPos.ZERO), source.list());
        assertThrows(UnsupportedOperationException.class, () -> task.getResult().positions().clear());
    }

    @Test void workerLoadCopiesNbtFiltersBoundsAndNeverMutatesItsSource() {
        BlockArray source = new BlockArray(List.of(new BlockPos(2, 3, 4)));
        source.bounds = new BlockBox(-10, 10);
        ListTag coordinates = new ListTag();
        coordinates.add(coordinate(-7, 2, 9, false));
        coordinates.add(coordinate(4, -3, 6, true));
        coordinates.add(coordinate(-7, 2, 9, false));
        coordinates.add(coordinate(50, 0, 0, false));
        CompoundTag data = new CompoundTag(); data.put("cells", coordinates);
        BlockArrayComputer task = source.getComputer().setLoad("cells", data);
        source.bounds = BlockBox.infinity();
        coordinates.clear(); data.remove("cells");
        CompletableFuture.runAsync(task).join();
        assertEquals(List.of(new BlockPos(-7, 2, 9), new BlockPos(4, -3, 6)), task.getResult().positions());
        assertEquals(List.of(new BlockPos(2, 3, 4)), source.list());
        assertEquals(BlockArrayComputer.Operation.LOAD, task.getResult().operation());
    }

    @Test void coordinatePersistenceReadsOriginalAndPortKeysAndRebuildsLimits() {
        List<BlockPos> coordinates = List.of(new BlockPos(-17, 8, 23), new BlockPos(6, -9, -4));
        BlockArray original = new BlockArray(coordinates);
        CompoundTag data = new CompoundTag(); original.saveAdditional("cells", data);
        CompoundTag first = data.getListOrEmpty("cells").getCompoundOrEmpty(0);
        assertEquals(-17, first.getIntOr("x", 0)); assertFalse(first.contains("pos.getX()"));
        CompoundTag incorrectLimits = data.getCompoundOrEmpty("cells_lim");
        incorrectLimits.putInt("minx", 9999); incorrectLimits.putInt("maxy", -9999);
        BlockArray restored = new BlockArray(); restored.load("cells", data);
        assertEquals(coordinates, restored.list());
        assertEquals(-17, restored.getMinX()); assertEquals(8, restored.getMaxY());
        ListTag legacyPort = new ListTag();
        CompoundTag mixed = coordinate(6, -9, -4, true); mixed.putInt("x", -2);
        legacyPort.add(coordinate(-17, 8, 23, true)); legacyPort.add(mixed);
        data.put("cells", legacyPort);
        restored.bounds = new BlockBox(-10, 10); restored.load("cells", data);
        assertEquals(List.of(new BlockPos(-2, -9, -4)), restored.list());
        assertEquals(-2, restored.getMinX()); assertEquals(-2, restored.getMaxX());
    }

    @Test void unconfiguredAndFailedOperationsCannotAdvertiseSuccessfulResults() {
        BlockArray source = new BlockArray(List.of(BlockPos.ZERO));
        BlockArrayComputer task = source.getComputer();
        assertThrows(IllegalStateException.class, task::run);
        for (BlockArrayComputer.Operation operation : BlockArrayComputer.Operation.values()) {
            task.setOperation(operation);
            assertThrows(IllegalStateException.class, task::run);
            assertThrows(IllegalStateException.class, task::getResult);
        }
        task.setIteration(position -> {}).run();
        assertEquals(1, task.getResult().positions().size());
        IllegalArgumentException failure = new IllegalArgumentException("callback cause");
        task.setIteration(position -> { throw failure; });
        assertSame(failure, assertThrows(IllegalArgumentException.class, task::run));
        assertThrows(IllegalStateException.class, task::getResult);
        assertEquals(List.of(BlockPos.ZERO), source.list());
    }

    @Test void activeTaskRejectsReconfigurationAndConcurrentExecution() throws InterruptedException {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        BlockArrayComputer task = new BlockArray(List.of(BlockPos.ZERO)).getComputer().setIteration(position -> {
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("test release timed out");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new AssertionError(exception);
            }
        });
        CompletableFuture<Void> worker = CompletableFuture.runAsync(task);
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, task::run);
            assertThrows(IllegalStateException.class, () -> task.setIteration(position -> {}));
            assertThrows(IllegalStateException.class, () -> task.setOperation(BlockArrayComputer.Operation.LOAD));
            assertThrows(IllegalStateException.class, () -> task.setLoad("cells", new CompoundTag()));
            assertThrows(IllegalStateException.class, task::getResult);
        } finally {
            release.countDown(); worker.join();
        }
        assertEquals(List.of(BlockPos.ZERO), task.getResult().positions());
        task.setLoad("absent", new CompoundTag()).run();
        assertTrue(task.getResult().positions().isEmpty());
    }

    @Test void freshComputerIncludesCoordinatesAddedAfterArrayConstruction() {
        BlockArray source = new BlockArray();
        BlockArrayComputer empty = source.getComputer().setIteration(position -> fail("empty snapshot"));
        source.addBlockCoordinate(1, 2, 3);
        empty.run(); assertTrue(empty.getResult().positions().isEmpty());
        BlockArrayComputer current = source.getComputer().setIteration(position -> {});
        current.run(); assertEquals(List.of(new BlockPos(1, 2, 3)), current.getResult().positions());
    }

    private static CompoundTag coordinate(int x, int y, int z, boolean portKeys) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(portKeys ? "pos.getX()" : "x", x);
        tag.putInt(portKeys ? "pos.getY()" : "y", y);
        tag.putInt(portKeys ? "pos.getZ()" : "z", z);
        return tag;
    }
}
