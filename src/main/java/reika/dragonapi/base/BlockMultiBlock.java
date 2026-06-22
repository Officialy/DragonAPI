/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.dragonapi.instantiable.data.blockstruct.filledblockarray.BlockMatchFailCallback;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * Multiblock casing base. State is modelled with blockstate properties in 26.2 — the formed/assembled
 * state lives in {@link #FORMED}, and each concrete casing adds its own named variant property (an
 * {@code EnumProperty} of casing parts). Rendering uses block models / BER; the old IIcon sheet +
 * integer-metadata path is gone.
 */
public abstract class BlockMultiBlock<R> extends Block {

	protected static final Direction[] dirs = Direction.values();

	/** True once this casing is part of an assembled multiblock (drives the formed model + break speed). */
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	public BlockMultiBlock(Properties properties) {
		super(properties);
		// Concrete subclasses call registerDefaultState themselves (they add their own variant property).
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	public abstract R checkForFullMultiBlock(Level world, int x, int y, int z, Direction dir, BlockMatchFailCallback call);

	public abstract void breakMultiBlock(Level world, int x, int y, int z);

	protected abstract void onCreateFullMultiBlock(Level world, int x, int y, int z, R ret);

	/** Whether placing this state should trigger a multiblock-assembly scan (e.g. only the core casing). */
	public abstract boolean canTriggerMultiBlockCheck(Level world, BlockPos pos, BlockState state);

	protected abstract BlockEntity getTileEntityForPosition(Level world, int x, int y, int z);

	protected boolean evaluate(R ret) {
		return ret != null && (!(ret instanceof Boolean b) || b);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, world, pos, oldState, isMoving);
		if (world.isClientSide())
			return;
		if (!this.canTriggerMultiBlockCheck(world, pos, state))
			return;
		R ret = this.checkForFullMultiBlock(world, pos.getX(), pos.getY(), pos.getZ(), Direction.NORTH, null);
		if (this.evaluate(ret))
			this.onCreateFullMultiBlock(world, pos.getX(), pos.getY(), pos.getZ(), ret);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof Container inv)
			ReikaItemHelper.dropInventory(world, pos);
		if (!world.isClientSide()) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			for (Direction dir : dirs) {
				this.breakMultiBlock(world, x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
			}
		}
		return super.onDestroyedByPlayer(state, world, pos, player, toolStack, willHarvest, fluid);
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		float ret = super.getDestroyProgress(state, player, level, pos);
		if (state.getValue(FORMED))
			ret *= 4;
		return ret;
	}

	public boolean hasTileEntity(BlockState state) {
		return false;
	}

	public BlockEntity createTileEntity(Level world, BlockState state) {
		return null;
	}

	// CHROMA-PORT: CustomCopyBehavior (allowCopy/getBlock/getMeta) removed — ChromatiCraft not in build.

}
