package reika.dragonapi.instantiable.storage;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** A live, sided view of one transactional fluid handler. All mutations use its journal. */
public final class FilteredFluidResourceHandler implements ResourceHandler<FluidResource> {
    private final ResourceHandler<FluidResource> delegate;
    private final IntPredicate visible;
    private final BiPredicate<Integer, FluidResource> canInsert;
    private final BiPredicate<Integer, FluidResource> canExtract;

    public FilteredFluidResourceHandler(ResourceHandler<FluidResource> delegate,
            IntPredicate visible,
            BiPredicate<Integer, FluidResource> canInsert,
            BiPredicate<Integer, FluidResource> canExtract) {
        this.delegate = Objects.requireNonNull(delegate);
        this.visible = Objects.requireNonNull(visible);
        this.canInsert = Objects.requireNonNull(canInsert);
        this.canExtract = Objects.requireNonNull(canExtract);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public FluidResource getResource(int index) {
        checkIndex(index);
        return visible.test(index) ? delegate.getResource(index) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        checkIndex(index);
        return visible.test(index) ? delegate.getAmountAsLong(index) : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        checkIndex(index);
        return visible.test(index) ? delegate.getCapacityAsLong(index, resource) : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        checkIndex(index);
        return visible.test(index) && delegate.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return visible.test(index) && canInsert.test(index, resource)
                ? delegate.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        checkIndex(index);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return visible.test(index) && canExtract.test(index, resource)
                ? delegate.extract(index, resource, amount, transaction) : 0;
    }

    private void checkIndex(int index) {
        Objects.checkIndex(index, delegate.size());
    }
}
