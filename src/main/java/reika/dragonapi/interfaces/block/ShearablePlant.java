package reika.dragonapi.interfaces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface ShearablePlant {

    void shearAll(Level world, BlockPos pos, Player ep);

    void shearSide(Level world, BlockPos pos, Direction side, Player ep);

}
