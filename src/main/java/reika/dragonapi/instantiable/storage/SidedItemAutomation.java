package reika.dragonapi.instantiable.storage;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Applies sided container rules without discarding the delegate's resource-specific restrictions. */
public final class SidedItemAutomation extends DelegatingResourceHandler<ItemResource> {
    private final WorldlyContainer container;
    private final Direction side;

    public SidedItemAutomation(ResourceHandler<ItemResource> delegate, WorldlyContainer container, Direction side) {
        super(delegate);
        this.container = container;
        this.side = side;
    }

    private boolean exposed(int slot) {
        return side == null || Arrays.stream(container.getSlotsForFace(side)).anyMatch(i -> i == slot);
    }

    private boolean canExtract(int slot, ItemResource resource) {
        var stack = resource.toStack(1);
        if (side != null) return exposed(slot) && container.canTakeItemThroughFace(slot, stack, side);
        for (Direction face : Direction.values()) {
            if (Arrays.stream(container.getSlotsForFace(face)).anyMatch(i -> i == slot)
                    && container.canTakeItemThroughFace(slot, stack, face)) return true;
        }
        return false;
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return resource.isEmpty() || exposed(slot) && container.canPlaceItem(slot, resource.toStack(1))
                && (side == null || container.canPlaceItemThroughFace(slot, resource.toStack(1), side))
                && super.isValid(slot, resource);
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return isValid(slot, resource) ? super.insert(slot, resource, amount, tx) : 0;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return canExtract(slot, resource) ? super.extract(slot, resource, amount, tx) : 0;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int moved = 0;
        for (int slot = 0; slot < size() && moved < amount; slot++) moved += insert(slot, resource, amount - moved, tx);
        return moved;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext tx) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int moved = 0;
        for (int slot = 0; slot < size() && moved < amount; slot++) moved += extract(slot, resource, amount - moved, tx);
        return moved;
    }
}
