package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;


public abstract class BlockReplaceOnBreak extends Block {

    protected BlockReplaceOnBreak() {
        super(Properties.of());
    }

	/*
	@Override
	public void harvestBlock(Level world, Player ep, BlockPos pos, int meta) {
		//ReikaBlockHelper.doBlockHarvest(world, ep, pos, this, this.harvesters);
	}*/

    /**
     * Called when a player removes a block.  This is responsible for
     * actually destroying the block, and the block is intact at time of call.
     * This is called regardless of whether the player can harvest the block or
     * not.
     * <p>
     * Return true if the block is actually destroyed.
     * <p>
     * Note: When used in multiplayer, this is called on both client and
     * server sides!
     *
     * @param state       The current state.
     * @param world       The current world
     * @param pos         Block position in world
     * @param player      The player damaging the block, may be null
     * @param willHarvest True if Block.harvestBlock will be called after this, if the return in true.
     *                    Can be useful to delay the destruction of tile entities till after harvestBlock
     * @param fluid       The current fluid state at current position
     * @return True if the block is actually destroyed.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return world.setBlock(pos, state, 3);
    }

}
