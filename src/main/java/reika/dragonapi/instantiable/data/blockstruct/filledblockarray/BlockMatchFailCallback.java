package reika.dragonapi.instantiable.data.blockstruct.filledblockarray;

import net.minecraft.world.level.Level;

import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.interfaces.BlockCheck;

/**
 * V33a had one {@code FilledBlockArray.BlockMatchFailCallback}; the package reorganisation also produced this top-level
 * copy, which multiblock blocks take. Extending the nested one lets a single callback serve both
 * {@link FilledBlockArray#matchInWorld} and {@code BlockMultiBlock.checkForFullMultiBlock}.
 */
public interface BlockMatchFailCallback extends FilledBlockArray.BlockMatchFailCallback {

	@Override
	void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek);

}
