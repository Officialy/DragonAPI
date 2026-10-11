package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.util.Mth;
import reika.dragonapi.interfaces.blockentity.ConditionBreakDropsInventory;
import reika.dragonapi.interfaces.block.SemiUnbreakable;
import reika.dragonapi.libraries.ReikaDirectionHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public abstract class BlockTEBase extends Block implements EntityBlock, SemiUnbreakable {

    public BlockTEBase(Properties properties) {
        super(properties);
    }


    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState pState, LivingEntity e, ItemStack pStack) {
        if (e instanceof Player) {
            BlockEntityBase te = (BlockEntityBase) world.getBlockEntity(pos);
            if (te != null)
                te.setPlacer((Player) e);
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BlockEntityBase tile && tile.isUnMineable())
            return 0;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean isUnbreakable(Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof BlockEntityBase tile && tile.isUnMineable();
    }

    @Override
    public void neighborChanged(BlockState pState, Level world, BlockPos pos, Block pBlock, Orientation pFromPos, boolean pIsMoving) {
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

    // Menu slots/data and tile sync tags deliver their own updates. Mutating interactions
    // synchronize in their concrete handlers; passive clicks do not broadcast full tile NBT.
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BlockEntityBase tile
                ? Mth.clamp(tile.getComparatorOverride(), 0, 15) : 0;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    /* framesAPI stuff - @Override
    public boolean canMove(Level world, BlockPos pos) {
        return !BlockEntityMoveEvent.fireTileMoveEvent(world, pos);
    }*/

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        BlockEntity te = level.getBlockEntity(pos);
        boolean drops = te instanceof Container;
        if (te instanceof ConditionBreakDropsInventory) {
            drops &= ((ConditionBreakDropsInventory) te).dropsInventoryOnBroken();
        }
        if (drops)
            ReikaItemHelper.dropInventory(level, pos);
        // BreakAction runs from BlockEntityBase.preRemoveSideEffects, on every removal rather than only this one.
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

}
