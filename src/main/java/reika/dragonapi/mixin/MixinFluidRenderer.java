package reika.dragonapi.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import reika.dragonapi.instantiable.event.client.FluidModelEvent;

/** Offers each meshed fluid block's model to {@link FluidModelEvent}. */
@Mixin(FluidRenderer.class)
public abstract class MixinFluidRenderer {
	@Redirect(method = "tesselate", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"))
	private FluidModel dragonapi$fluidModelEvent(FluidStateModelSet models, FluidState state, BlockAndTintGetter level,
			BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState) {
		return FluidModelEvent.fire(level, pos, blockState, fluidState, models.get(state));
	}
}
