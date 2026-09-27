package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.capabilities.Capabilities;
import reika.dragonapi.interfaces.blockentity.HasItemHandler;

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
        return tile instanceof HasItemHandler;
    }

    public final boolean isTileFluidHandler() {
        return tile.getLevel() != null
                && tile.getLevel().getCapability(Capabilities.Fluid.BLOCK, tile.getBlockPos(), null) != null;
    }

    protected final BlockEntity getTile() {
        return tile;
    }

}
