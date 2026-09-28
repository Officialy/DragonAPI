package reika.dragonapi.mixin;

import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.WeatherAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import reika.dragonapi.instantiable.event.client.WeatherSkyStrengthEvent;

/**
 * Fires V33a's {@link WeatherSkyStrengthEvent} for a client level's rain and thunder strengths before the
 * weather layers of its environment attributes (sky, fog, cloud and daylight darkening) use them.
 * DragonAPI 1.7.10 routed the renderer's weather-strength reads through the event; in 26.2 those reads
 * are the level's {@code WeatherAccess} (26.3: built in {@code addDynamicLayers}), which vanilla samples at partial tick 1. Server levels keep the
 * vanilla access, as upstream's event was client-only.
 */
@Mixin(EnvironmentAttributeSystem.class)
public abstract class MixinEnvironmentAttributeSystem {
	@Redirect(method = "addDynamicLayers", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/WeatherAttributes$WeatherAccess;from(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/attribute/WeatherAttributes$WeatherAccess;"))
	private static WeatherAttributes.WeatherAccess dragonapi$weatherSkyStrength(Level level) {
		WeatherAttributes.WeatherAccess original = WeatherAttributes.WeatherAccess.from(level);
		if (!level.isClientSide())
			return original;
		return new WeatherAttributes.WeatherAccess() {
			@Override
			public float rainLevel() {
				return WeatherSkyStrengthEvent.fire_Rain(level, 1.0F);
			}

			@Override
			public float thunderLevel() {
				return WeatherSkyStrengthEvent.fire_Thunder(level, 1.0F);
			}
		};
	}
}
