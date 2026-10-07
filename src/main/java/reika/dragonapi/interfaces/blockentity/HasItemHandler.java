package reika.dragonapi.interfaces.blockentity;

import reika.dragonapi.instantiable.storage.ManagedItemHandler;

/**
 * Marker for {@link reika.dragonapi.base.BlockEntityBase} subclasses that expose a single primary
 * {@link ManagedItemHandler}. Used by {@code CoreContainer} to bind its slot helpers to the
 * tile's inventory without relying on the deprecated {@code te instanceof IItemHandler} check
 * (the {@code net.neoforged.neoforge.items.IItemHandler} family is being removed in 1.21.9).
 */
public interface HasItemHandler {
    ManagedItemHandler getItemHandler();

    default net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getAutomationItemHandler(net.minecraft.core.Direction side) {
        var handler = getAutomationItemHandler();
        return this instanceof net.minecraft.world.WorldlyContainer container
                ? new reika.dragonapi.instantiable.storage.SidedItemAutomation(handler, container, side) : handler;
    }

    /**
     * What automation (hoppers, pipes, the item capability) sees; the full handler unless the tile restricts which
     * slots can be filled or emptied from outside (1.7.10's {@code canInsertItem}/{@code canExtractItem}).
     */
    default net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getAutomationItemHandler() {
        return this.getItemHandler();
    }
}
