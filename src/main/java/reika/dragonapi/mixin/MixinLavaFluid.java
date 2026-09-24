package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import reika.dragonapi.instantiable.event.LavaSpawnFireEvent;

/** Fires {@link LavaSpawnFireEvent} at the fire gate of {@code LavaFluid.randomTick}. */
@Mixin(LavaFluid.class)
public abstract class MixinLavaFluid {
	@Redirect(method = "randomTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;canSpreadFireAround(Lnet/minecraft/core/BlockPos;)Z"))
	private boolean dragonapi$lavaSpawnFireEvent(ServerLevel level, BlockPos pos) {
		return LavaSpawnFireEvent.fire(level, pos, level.canSpreadFireAround(pos));
	}
}
