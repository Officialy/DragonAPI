package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;

import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

/**
 * V33a {@code GrassSustainCropEvent}: lets grass act as farmland for crops at a position.
 *
 * <p>DragonAPI 1.7.10 patched {@code BlockGrass.canSustainPlant} and {@code isFertile} to ask this.
 * {@link Result#ALLOW} made grass sustain what farmland sustains (1.7.10's {@code Crop} plant type:
 * wheat, carrots, potatoes and the melon and pumpkin stems) and count as fertile; {@link Result#DENY}
 * made it sustain nothing. {@code MixinGrassBlock} adds the same two answers to the 26.2 grass block.
 *
 * <p>Asked only for crops: upstream asked for every plant, but a non-crop could only be affected by a
 * DENY, and a hook on every plant-on-grass survival check is too hot to post for nothing. Only posted
 * with a real {@link Level}.
 */
public class GrassSustainCropEvent extends WorldPositionEvent {

	public enum Result { ALLOW, DEFAULT, DENY }

	public final BlockState crop;
	private Result result = Result.DEFAULT;

	public GrassSustainCropEvent(Level world, BlockPos pos, BlockState crop) {
		super(world, pos);
		this.crop = crop;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	public static boolean isCrop(BlockState plant) {
		var b = plant.getBlock();
		return b instanceof CropBlock || b instanceof StemBlock || b instanceof AttachedStemBlock;
	}

	/** {@code canSustainPlant} for grass: TRUE for crops when allowed, FALSE when denied. */
	public static TriState fireSustain(Level world, BlockPos pos, BlockState plant) {
		if (!isCrop(plant))
			return TriState.DEFAULT;
		GrassSustainCropEvent e = NeoForge.EVENT_BUS.post(new GrassSustainCropEvent(world, pos, plant));
		return switch (e.getResult()) {
			case ALLOW -> TriState.TRUE;
			case DENY -> TriState.FALSE;
			default -> TriState.DEFAULT;
		};
	}

	/** {@code isFertile} for grass: fertile when allowed, as watered farmland is. */
	public static boolean fireFertility(Level world, BlockPos pos, boolean original) {
		GrassSustainCropEvent e = NeoForge.EVENT_BUS.post(new GrassSustainCropEvent(world, pos, Blocks.WHEAT.defaultBlockState()));
		return switch (e.getResult()) {
			case ALLOW -> true;
			case DENY -> false;
			default -> original;
		};
	}
}
