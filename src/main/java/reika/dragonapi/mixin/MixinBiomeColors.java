package reika.dragonapi.mixin;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.dragonapi.instantiable.event.client.WaterColorEvent;

/**
 * Restores V33a's {@link WaterColorEvent}.
 *
 * <p>DragonAPI 1.7.10 ASM-patched {@code BlockLiquid.colorMultiplier}. Every water tint in 26.2 —
 * NeoForge's water {@code FluidTintSource}, splash and drip particles, and anything else colouring
 * water from the world — funnels through {@code BiomeColors.getAverageWaterColor}, so injecting at its
 * return reaches all of them with vanilla's biome-blended colour already computed.
 */
@Mixin(BiomeColors.class)
public abstract class MixinBiomeColors {
	@Inject(method = "getAverageWaterColor(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
			at = @At("RETURN"), cancellable = true)
	private static void dragonapi$fireWaterColor(BlockAndTintGetter level, BlockPos pos,
			CallbackInfoReturnable<Integer> callback) {
		callback.setReturnValue(WaterColorEvent.fire(level, pos, callback.getReturnValueI()));
	}
}
