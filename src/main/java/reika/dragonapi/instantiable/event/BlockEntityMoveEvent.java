package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class BlockEntityMoveEvent extends Event {

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
        return MinecraftForge.EVENT_BUS.post(new BlockEntityMoveEvent(world, pos, world.getBlockState(pos), world.getBlockEntity(pos)));
    }
}
