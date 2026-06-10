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
}
