package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class BlockReplaceOnBreak extends Block {

    public BlockReplaceOnBreak(Properties p) {
        super(p);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        return world.setBlock(pos, state, 3);
    }

}
