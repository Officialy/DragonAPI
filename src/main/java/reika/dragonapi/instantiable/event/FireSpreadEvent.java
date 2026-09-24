package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

/**
 * V33a {@code FireSpreadEvent}: cancel it and this fire does not tick — it neither spreads, ages nor
 * burns out, exactly as if fire ticking were switched off for it.
 *
 * <p>DragonAPI 1.7.10 replaced {@code BlockFire.updateTick}'s {@code doFireTick} game-rule test with this
 * event. The 26.2 successor of that test is {@code ServerLevel.canSpreadFireAround}, which
 * {@code FireBlock.tick} asks at the same point; {@code MixinFireBlock} fires the event there.
 */
public class FireSpreadEvent extends WorldPositionEvent implements ICancellableEvent {

	public FireSpreadEvent(Level world, BlockPos pos) {
		super(world, pos);
	}

	/** Whether the fire may tick: the vanilla answer first, then the event. */
	public static boolean fire(Level world, BlockPos pos, boolean original) {
		if (!original)
			return false;
		return !NeoForge.EVENT_BUS.post(new FireSpreadEvent(world, pos)).isCanceled();
	}
}
