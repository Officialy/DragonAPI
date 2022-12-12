package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

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
