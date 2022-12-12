package reika.dragonapi.interfaces.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface SimpleConnection {

    boolean tryConnect(Level world, BlockPos pos);

}
