package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.common.NeoForge;
import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

public class IceFreezeEvent extends WorldPositionEvent {

	public final boolean needsEdge;

	public IceFreezeEvent(Level world, BlockPos pos, boolean edge) {
		super(world, pos);
		needsEdge = edge;
	}

	public final boolean wouldFreezeNaturally() {
		return false;//todo world.canBlockFreeze(xCoord, yCoord, zCoord, needsEdge);
	}

	public static boolean fire(Level world, int x, BlockPos pos, boolean edge) {
		IceFreezeEvent evt = new IceFreezeEvent(world, pos, edge);
		NeoForge.EVENT_BUS.post(evt);
		switch(evt.getResult()) {
			case ALLOW:
				return true;
			case DEFAULT:
			default:
				return evt.wouldFreezeNaturally();
			case DENY:
				return false;
		}
	}

	public static boolean fire_IgnoreVanilla(Level world, BlockPos pos) {
		IceFreezeEvent evt = new IceFreezeEvent(world, pos, false);
		NeoForge.EVENT_BUS.post(evt);
		return evt.getResult() != Result.DENY;
	}

    
}
