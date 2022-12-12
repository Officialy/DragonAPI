package reika.dragonapi.interfaces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;


public interface CustomSnowAccumulation {

    boolean canSnowAccumulate(Level world, BlockPos pos);

}
