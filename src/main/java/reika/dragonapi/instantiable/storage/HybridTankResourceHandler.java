package reika.dragonapi.instantiable.storage;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntBiFunction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import reika.dragonapi.instantiable.HybridTank;

/**
 * Transactional capability view of machine-owned {@link HybridTank}s. One instance owns the
 * journal for all of a machine's tanks; sided views must delegate to this instance rather than
 * create a second journal over the same tanks.
 *
 * <p>The tanks remain the authoritative storage so their existing named NBT format and direct
 * machine operations are preserved. External insertions and extractions snapshot complete fluid
 * stacks, including data components, and are reversible until the root transaction commits.
 */
public final class HybridTankResourceHandler extends SnapshotJournal<FluidStack[]>
        implements ResourceHandler<FluidResource> {

    private final HybridTank[] tanks;
    private final BiPredicate<Integer, FluidResource> canInsert;
    private final BiPredicate<Integer, FluidResource> canExtract;
    private final Runnable onCommit;
    private final ToIntBiFunction<Integer, FluidResource> capacity;

    public HybridTankResourceHandler(HybridTank[] tanks,
            BiPredicate<Integer, FluidResource> canInsert,
            BiPredicate<Integer, FluidResource> canExtract,
            Runnable onCommit) {
        this(tanks, canInsert, canExtract, onCommit,
                (index, resource) -> tanks[index].getCapacity());
    }

    public HybridTankResourceHandler(HybridTank[] tanks,
            BiPredicate<Integer, FluidResource> canInsert,
            BiPredicate<Integer, FluidResource> canExtract,
            Runnable onCommit,
            IntUnaryOperator capacity) {
        this(tanks, canInsert, canExtract, onCommit,
                (index, resource) -> capacity.applyAsInt(index));
    }

    public HybridTankResourceHandler(HybridTank[] tanks,
            BiPredicate<Integer, FluidResource> canInsert,
            BiPredicate<Integer, FluidResource> canExtract,
            Runnable onCommit,
            ToIntBiFunction<Integer, FluidResource> capacity) {
        this.tanks = Arrays.copyOf(Objects.requireNonNull(tanks), tanks.length);
        for (HybridTank tank : this.tanks) Objects.requireNonNull(tank);
        this.canInsert = Objects.requireNonNull(canInsert);
        this.canExtract = Objects.requireNonNull(canExtract);
        this.onCommit = Objects.requireNonNull(onCommit);
        this.capacity = Objects.requireNonNull(capacity);
    }

    @Override
    public int size() {
        return tanks.length;
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(tank(index).getFluid());
    }

    @Override
    public long getAmountAsLong(int index) {
        return tank(index).getFluidLevel();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        HybridTank tank = tank(index);
        return resource.isEmpty() || isValid(index, resource) ? Math.max(0, Math.min(tank.getCapacity(), capacity.applyAsInt(index, resource))) : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && tank(index).isFluidValid(resource.toStack(1))
                && (canInsert.test(index, resource) || canExtract.test(index, resource));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        HybridTank tank = tank(index);
        if (amount == 0 || !isValid(index, resource) || !canInsert.test(index, resource)) return 0;

        FluidStack current = tank.getFluid();
        if (!current.isEmpty() && !FluidResource.of(current).equals(resource)) return 0;
        int inserted = Math.min(amount, Math.max(0, getCapacityAsInt(index, resource) - current.getAmount()));
        if (inserted <= 0) return 0;

        updateSnapshots(transaction);
        tank.setFluid(resource.toStack(current.getAmount() + inserted));
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        HybridTank tank = tank(index);
        FluidStack current = tank.getFluid();
        if (amount == 0 || current.isEmpty() || !FluidResource.of(current).equals(resource)
                || !canExtract.test(index, resource)) return 0;
        int extracted = Math.min(amount, current.getAmount());
        if (extracted <= 0) return 0;

        updateSnapshots(transaction);
        int remaining = current.getAmount() - extracted;
        tank.setFluid(remaining == 0 ? FluidStack.EMPTY : current.copyWithAmount(remaining));
        return extracted;
    }

    @Override
    protected FluidStack[] createSnapshot() {
        FluidStack[] snapshot = new FluidStack[tanks.length];
        for (int i = 0; i < tanks.length; i++) snapshot[i] = tanks[i].getFluid().copy();
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(FluidStack[] snapshot) {
        for (int i = 0; i < tanks.length; i++) tanks[i].setFluid(snapshot[i]);
    }

    @Override
    protected void onRootCommit(FluidStack[] originalState) {
        onCommit.run();
    }

    private HybridTank tank(int index) {
        return tanks[Objects.checkIndex(index, tanks.length)];
    }
}
