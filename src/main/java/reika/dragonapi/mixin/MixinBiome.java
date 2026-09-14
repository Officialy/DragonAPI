package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.dragonapi.instantiable.event.IceFreezeEvent;

/** Restores DragonAPI's cancellable freeze hook after vanilla has completed its natural checks. */
@Mixin(Biome.class)
public abstract class MixinBiome {
	@Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
			at = @At("RETURN"), cancellable = true)
	private void dragonapi$fireIceFreeze(LevelReader reader, BlockPos pos, boolean checkNeighbors,
			CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValueZ() && reader instanceof Level level
				&& !IceFreezeEvent.isNaturalCheckInProgress()
				&& !IceFreezeEvent.fire_IgnoreVanilla(level, pos, checkNeighbors))
			callback.setReturnValue(false);
	}
}
