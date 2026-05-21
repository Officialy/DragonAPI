package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

public class BlockEntityMoveEvent extends Event implements ICancellableEvent {

    public final Level world;
    public final BlockPos pos;

    public final BlockState block;
    public final BlockEntity tile;

    public BlockEntityMoveEvent(Level w, BlockPos position, BlockState b, BlockEntity te) {
        world = w;
        pos = position;
        block = b;
        tile = te;
    }

    public static boolean fireTileMoveEvent(Level world, BlockPos pos) {
        return NeoForge.EVENT_BUS.post(new BlockEntityMoveEvent(world, pos, world.getBlockState(pos), world.getBlockEntity(pos)));
    }
}

