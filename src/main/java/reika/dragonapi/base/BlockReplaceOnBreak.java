package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Player removal that replaces the old block with a subclass-selected complete state. */
public abstract class BlockReplaceOnBreak extends Block {

    protected BlockReplaceOnBreak(Properties p) {
        super(p);
    }

    @Override
    public final boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        BlockState replacement = getReplacementState(world, pos, state, player, toolStack, willHarvest, fluid);
        return world.setBlock(pos, java.util.Objects.requireNonNull(replacement,
                "Replacement state from " + getClass().getName() + " at " + pos), Block.UPDATE_ALL);
    }

    /**
     * Successor of the original getBlockReplacedWith/getMetaReplacedWith pair. The caller retains
     * the old state and entity for native harvesting after this removal callback succeeds, so this
     * hook chooses the replacement without manually dropping either block's loot.
     */
    public abstract BlockState getReplacementState(Level world, BlockPos pos, BlockState oldState,
            Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid);

    /** Original self-replacing policy: preserve the exact old state and native unchanged-state result. */
    public static class BlockReplaceSelfOnBreak extends BlockReplaceOnBreak {
        protected BlockReplaceSelfOnBreak(Properties properties) {
            super(properties);
        }

        @Override
        public final BlockState getReplacementState(Level world, BlockPos pos, BlockState oldState,
                Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
            return oldState;
        }
    }
}
