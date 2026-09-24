package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;

import reika.dragonapi.instantiable.event.GrassSustainCropEvent;

/**
 * Gives the 26.2 grass block V33a's {@link GrassSustainCropEvent}: the two {@code IBlockExtension}
 * methods vanilla grass leaves at their defaults, answered through the event when a real level is at
 * hand. Outside a level (worldgen regions, render caches) they keep their defaults.
 */
@Mixin(GrassBlock.class)
public abstract class MixinGrassBlock {

	public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
		if (facing != Direction.UP || !(level instanceof Level world))
			return TriState.DEFAULT;
		return GrassSustainCropEvent.fireSustain(world, soilPosition, plant);
	}

	public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
		if (!(level instanceof Level world))
			return false;
		return GrassSustainCropEvent.fireFertility(world, pos, false);
	}
}
