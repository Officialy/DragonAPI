package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import reika.dragonapi.instantiable.event.BlockEntityMoveEvent;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.interfaces.blockentity.ConditionBreakDropsInventory;
import reika.dragonapi.libraries.ReikaDirectionHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public abstract class BlockTEBase extends Block implements EntityBlock {

    public BlockTEBase(Properties properties) {
        super(properties);
    }


    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState pState, @Nullable LivingEntity e, ItemStack pStack) {
        if (e instanceof Player) {
            BlockEntityBase te = (BlockEntityBase) world.getBlockEntity(pos);
            if (te != null)
                te.setPlacer((Player) e);
        }
    }

/*    @Override
    public final float getBlockHardness(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof BlockEntityBase && ((BlockEntityBase) te).isUnMineable())
            return -1;
        return blockHardness;
    }*/

    @Override
    public void neighborChanged(BlockState pState, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        BlockEntityBase te = (BlockEntityBase)world.getBlockEntity(pos);
        if (te != null)
            te.onBlockUpdate();
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        Direction dir = ReikaDirectionHelper.getDirectionBetween(pos, neighbor);
        BlockEntityBase te = (BlockEntityBase) level.getBlockEntity(pos);
        if (te != null)
            te.updateCache(dir);
    }

    public void updateTileCache(Level world, BlockPos pos) {
        BlockEntityBase te = (BlockEntityBase) world.getBlockEntity(pos);
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            te.updateCache(dir);
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        BlockEntityBase te = (BlockEntityBase) pLevel.getBlockEntity(pPos);
        te.syncAllData(true);
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }


/*    @Override
    public final boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public final int getComparatorInputOverride(Level world, BlockPos pos, int par5) {
        return ((BlockEntityBase) world.getBlockEntity(pos)).getComparatorOverride();
    }*/

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    /* framesAPI stuff - @Override
    public boolean canMove(Level world, BlockPos pos) {
        return !BlockEntityMoveEvent.fireTileMoveEvent(world, pos);
    }*/

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        BlockEntity te = level.getBlockEntity(pos);
        boolean drops = te instanceof Container;
        if (te instanceof ConditionBreakDropsInventory) {
            drops &= ((ConditionBreakDropsInventory) te).dropsInventoryOnBroken();
        }
        if (drops)
            ReikaItemHelper.dropInventory(level, pos);
        if (te instanceof BreakAction) {
            ((BreakAction) te).breakBlock();
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

}
