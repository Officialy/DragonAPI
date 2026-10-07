package reika.dragonapi.interfaces.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * A block entity that stands in for another block's computer peripheral (the ChromatiCraft World Rift, which in 1.7.10
 * implemented ComputerCraft's {@code IPeripheral} by forwarding every call to the tile beyond its linked partner).
 *
 * <p>Computer-mod free: {@link reika.dragonapi.modinteract.CCHooks} resolves the relayed peripheral when one is asked
 * for. The relay must call {@code level.invalidateCapabilities(pos)} whenever its target changes (link, unlink,
 * direction, the far neighbour changing) so attached computers re-query it.
 */
public interface PeripheralRelay {

	/** The block whose peripheral this one stands in for, or null while there is none. */
	@Nullable
	Target getRelayedPeripheral();

	record Target(Level level, BlockPos pos, @Nullable Direction side) {

	}

}
