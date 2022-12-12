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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public abstract class BlockTieredResource extends Block {

	protected static final Random rand = new Random();

	public BlockTieredResource(Material mat) {
		super(Properties.of(mat));
	}

	//@Override
	public final Item getItemDropped(Random r, int fortune) {
		return null;
	}

	@Override
	public List<ItemStack> getDrops(BlockState p_220076_1_, LootContext.Builder p_220076_2_) {
		return new ArrayList<>();
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
		return false;
	}

	public abstract Collection<ItemStack> getHarvestResources(Level world, BlockPos pos, int fortune, Player player);

	public Collection<ItemStack> getNoHarvestResources(Level world, BlockPos pos, int fortune, Player player) {
		return new ArrayList<>();
	}

	public abstract boolean isPlayerSufficientTier(BlockGetter world, BlockPos pos, Player ep);


	public final void onBlockPlacedBy(Level world, BlockPos pos, LivingEntity elb, ItemStack is) {
		if (elb instanceof Player) {
			Player ep = (Player) elb;
			if (!this.isPlayerSufficientTier(world, pos, ep)) {
				//if (world.isClientSide()) //isClientSide()
					//ReikaRenderHelper.spawnDropParticles(world, pos, this);
				//ReikaSoundHelper.playBreakSound(world, pos, this);
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 1);
			}
		}
	}

	@Override
	public void onPlace(BlockState p_220082_1_, Level p_220082_2_, BlockPos p_220082_3_, BlockState p_220082_4_, boolean p_220082_5_) {
		super.onPlace(p_220082_1_, p_220082_2_, p_220082_3_, p_220082_4_, p_220082_5_);
	}
}
