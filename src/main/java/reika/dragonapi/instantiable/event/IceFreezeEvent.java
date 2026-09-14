package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

public class IceFreezeEvent extends WorldPositionEvent implements ICancellableEvent {
	private static final ThreadLocal<Boolean> NATURAL_CHECK =
			ThreadLocal.withInitial(() -> Boolean.FALSE);

	public final boolean needsEdge;

	public IceFreezeEvent(Level world, BlockPos pos, boolean edge) {
		super(world, pos);
		needsEdge = edge;
	}

	public final boolean wouldFreezeNaturally() {
		BlockPos pos = new BlockPos(xCoord, yCoord, zCoord);
		NATURAL_CHECK.set(Boolean.TRUE);
		try {
			return world.getBiome(pos).value().shouldFreeze(world, pos, needsEdge);
		}
		finally {
			NATURAL_CHECK.remove();
		}
	}

	public static boolean isNaturalCheckInProgress() {
		return NATURAL_CHECK.get();
	}

	public static boolean fire(Level world, int x, BlockPos pos, boolean edge) {
		IceFreezeEvent evt = new IceFreezeEvent(world, pos, edge);
		NeoForge.EVENT_BUS.post(evt);
		if (evt.isCanceled()) {
            return false;
        } else {
            return evt.wouldFreezeNaturally(); // todo: implement ALLOW
        }
	}

	public static boolean fire_IgnoreVanilla(Level world, BlockPos pos) {
		return fire_IgnoreVanilla(world, pos, false);
	}

	public static boolean fire_IgnoreVanilla(Level world, BlockPos pos, boolean edge) {
		IceFreezeEvent evt = new IceFreezeEvent(world, pos, edge);
		NeoForge.EVENT_BUS.post(evt);
		return !evt.isCanceled();
	}

    
}
