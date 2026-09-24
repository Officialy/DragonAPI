package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import reika.dragonapi.instantiable.event.FireSpreadEvent;

/** Fires {@link FireSpreadEvent} at the fire-tick gate of {@code FireBlock.tick}. */
@Mixin(FireBlock.class)
public abstract class MixinFireBlock {
	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;canSpreadFireAround(Lnet/minecraft/core/BlockPos;)Z"))
	private boolean dragonapi$fireSpreadEvent(ServerLevel level, BlockPos pos) {
		return FireSpreadEvent.fire(level, pos, level.canSpreadFireAround(pos));
	}
}
