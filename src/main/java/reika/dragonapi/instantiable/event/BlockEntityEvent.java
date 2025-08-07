package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class BlockEntityEvent extends Event {

    private final BlockEntity tile;

    public BlockEntityEvent(BlockEntity te) {
        tile = te;
    }

    public final Level getWorld() {
        return tile.getLevel();
    }

    public final BlockPos getTilePos() {
        return tile.getBlockPos();
    }

    public final boolean isTileInventory() {
        return tile instanceof IItemHandler;
    }

    public final boolean isTileFluidHandler() {
        return tile instanceof IFluidHandler;
    }

    protected final BlockEntity getTile() {
        return tile;
    }

}

