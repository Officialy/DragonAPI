package reika.dragonapi.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import reika.dragonapi.instantiable.event.client.SkyColorEvent;

/**
 * Fires DragonAPI's {@link SkyColorEvent} for the client sky colour. 1.7.10 biomes computed their own sky
 * colour ({@code getSkyColorByTemp}); 26.2 resolves it through environment attributes, where a biome can
 * only give a constant, so this adds a final sky-colour layer that offers the result to listeners once per
 * attribute cache tick.
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void dragonapi$skyColorEvent(EnvironmentAttributeSystem.Builder builder,
			CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> callback) {
		callback.getReturnValue().addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
				(color, cacheTickId) -> SkyColorEvent.fire(color));
	}
}
