package reika.dragonapi.instantiable.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 1.21.9: the legacy {@code net.neoforged.neoforge.items.ItemStackHandler} (and the whole
 * {@code IItemHandler} family) was deprecated for removal. The official replacement is
 * {@link ItemStacksResourceHandler}, but its API is meaningfully different:
 *
 * <ul>
 *   <li>Reads return a {@link ItemResource} (resource type) plus a count, not an {@link ItemStack}.</li>
 *   <li>Mutating operations ({@code insert} / {@code extract}) require a {@link Transaction}
 *       rather than a {@code boolean simulate} flag.</li>
 *   <li>Slots are no longer bridged by {@code SlotItemHandler}; the new bridge is
 *       {@link ResourceHandlerSlot}.</li>
 * </ul>
 *
 * <p>This shim subclasses {@link ItemStacksResourceHandler} and adds back the old
 * {@code IItemHandler}-style methods ({@link #getStackInSlot}, {@link #setStackInSlot},
 * {@link #insertItem}, {@link #extractItem}, {@link #getSlots}, {@link #getSlotLimit},
 * {@link #isItemValid}). Internally each one opens a root {@link Transaction} so callers
 * that pass {@code simulate=true} get the same don't-touch semantics they used to have, and
 * callers passing {@code simulate=false} get the committed result. This is a workaround for
 * the impedance mismatch — when the codebase has fully migrated to {@link Transaction}-aware
 * call sites this shim can be deleted.
 *
 * <p>{@link #slot(int, int, int)} is a convenience that hands callers a working
 * {@link ResourceHandlerSlot} bound to this handler's {@link #set} method, so containers can
 * keep saying {@code addSlot(handler.slot(0, x, y))} without thinking about the modifier.
 *
 * <p>Subclasses can override {@link #onContentsChanged(int, ItemStack)} for setChanged() calls.
 */
public class ManagedItemHandler extends ItemStacksResourceHandler {

    /** Single-slot default. Matches the legacy {@code new ItemStackHandler()} zero-arg ctor. */
    public ManagedItemHandler() {
        super(1);
    }

    public ManagedItemHandler(int size) {
        super(size);
    }

    public ManagedItemHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    /* ----------------------------------------------------------------------- */
    /* Legacy IItemHandler-style API: call sites in BEs, containers, GUIs etc. */
    /* ----------------------------------------------------------------------- */

    /** Legacy alias for {@link #size()}. */
    public int getSlots() {
        return size();
    }

    /**
     * Legacy alias for {@code new ItemStack(getResource(i), getAmountAsInt(i))}; returns a
     * <strong>live, mutable copy</strong> snapshot of the slot's contents. Mutating the
     * returned stack does NOT write back — use {@link #setStackInSlot} or {@link #set}.
     */
    public ItemStack getStackInSlot(int slot) {
        return getResource(slot).toStack(getAmountAsInt(slot));
    }

    /**
     * Legacy alias for {@code set(slot, ItemResource.of(stack), stack.getCount())}.
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, ItemResource.of(stack), stack.getCount());
    }

    /**
     * Legacy {@code IItemHandler.extractItem}. Opens a root transaction; commits when
     * {@code simulate == false}, aborts otherwise. The returned stack is the actual amount
     * extracted, exactly the way the deprecated API behaved.
     */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemResource resource = getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(slot, resource, amount, tx);
            if (extracted <= 0) return ItemStack.EMPTY;
            ItemStack out = resource.toStack(extracted);
            if (!simulate) tx.commit();
            return out;
        }
    }

    /**
     * Legacy {@code IItemHandler.insertItem}. Returns the remainder (what couldn't fit).
     */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemResource resource = ItemResource.of(stack);
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = insert(slot, resource, stack.getCount(), tx);
            int remainingCount = stack.getCount() - inserted;
            if (!simulate) tx.commit();
            return remainingCount > 0 ? resource.toStack(remainingCount) : ItemStack.EMPTY;
        }
    }

    /** Legacy alias for {@link #getCapacityAsInt}, with an empty-resource probe. */
    public int getSlotLimit(int slot) {
        return getCapacityAsInt(slot, ItemResource.EMPTY);
    }

    /** Legacy alias for {@link #isValid}. */
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValid(slot, ItemResource.of(stack));
    }

    /**
     * Convenience: build a {@link ResourceHandlerSlot} bound to this handler's {@code set}
     * method, so containers can say {@code addSlot(handler.slot(0, x, y))}.
     */
    public Slot slot(int index, int xPos, int yPos) {
        return new ResourceHandlerSlot(this, this::set, index, xPos, yPos);
    }

    /* ----------------------------------------------------------------------- */
    /* setChanged() hook                                                       */
    /* ----------------------------------------------------------------------- */

    /**
     * Subclasses override this for the legacy {@code onContentsChanged(int slot)} hook (no
     * previous-stack argument). The parent's {@link ItemStacksResourceHandler} method takes
     * the previous {@link ItemStack}; we route both to the slot-only variant most call sites
     * actually want.
     */
    @Override
    protected final void onContentsChanged(int index, ItemStack previousContents) {
        onContentsChanged(index);
    }

    /** Override this in subclasses to react to slot mutations. Default does nothing. */
    protected void onContentsChanged(int slot) {}
}
