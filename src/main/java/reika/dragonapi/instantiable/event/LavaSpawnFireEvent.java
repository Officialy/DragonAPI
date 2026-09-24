package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

/**
 * V33a {@code LavaSpawnFireEvent}: cancel it and this lava block sets nothing alight on this random
 * tick. Fired from {@code MixinLavaFluid} at the fire gate of {@code LavaFluid.randomTick}.
 */
public class LavaSpawnFireEvent extends WorldPositionEvent implements ICancellableEvent {

	public LavaSpawnFireEvent(Level world, BlockPos pos) {
		super(world, pos);
	}

	public static boolean fire(Level world, BlockPos pos, boolean original) {
		if (!original)
			return false;
		return !NeoForge.EVENT_BUS.post(new LavaSpawnFireEvent(world, pos)).isCanceled();
	}
}
