package reika.dragonapi.mixin;

import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import reika.dragonapi.extras.shader.IrisCompat;

import java.util.List;

/**
 * Drops shadow-excluded pipelines from Iris's shadow pass; see {@link IrisCompat}.
 *
 * <p>Every custom-geometry submission (block entities, entities, {@code submitSpecial} and the custom-geometry
 * event) is built here, and Iris's shadow pass re-runs the block-entity and entity submitters, so filtering the
 * batch keeps glows out of the shadow map without a guard in each renderer.
 */
@Mixin(CustomFeatureRenderer.class)
public abstract class MixinCustomFeatureRenderer {
	@ModifyVariable(method = "buildGroup", at = @At("HEAD"), argsOnly = true)
	private List<CustomFeatureRenderer.Submit> dragonapi$dropShadowExcluded(List<CustomFeatureRenderer.Submit> submits) {
		return IrisCompat.filterShadowPass(submits);
	}
}
