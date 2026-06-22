package reika.dragonapi.instantiable.data.blockstruct.filledblockarray;

import net.minecraft.world.level.Level;

import reika.dragonapi.interfaces.BlockCheck;

public interface BlockMatchFailCallback {

	void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek);

}
