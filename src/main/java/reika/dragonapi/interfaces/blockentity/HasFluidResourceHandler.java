package reika.dragonapi.interfaces.blockentity;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/** Exposes the transaction-aware fluid view of a block entity to NeoForge automation. */
public interface HasFluidResourceHandler {
    /**
     * @param side face through which the caller accesses the block, or {@code null} for an
     *             unsided query
     * @return the live handler for that face, or {@code null} if fluid access is unavailable
     */
    @Nullable ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side);
}
