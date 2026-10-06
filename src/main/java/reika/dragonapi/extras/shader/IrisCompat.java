package reika.dragonapi.extras.shader;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional Iris integration for the Reika mods' custom {@link RenderPipeline}s, through Iris's public v0 API
 * resolved reflectively so no Iris type reaches a runtime without Iris.
 *
 * <p>With a shader pack active, Iris swaps every pipeline drawn into its world framebuffers for one of the
 * pack's programs. A pipeline it has no program for keeps vanilla's shader and logs "Missing program", so each
 * world-drawn pipeline must be {@linkplain #assign assigned}. Iris picks the program variant whose vertex format
 * matches the pipeline, and applies the pack's {@code blend.<program>} override when the pack declares one, so
 * additive glows belong on {@code EMISSIVE_ENTITIES} (gbuffers_spidereyes), which packs keep additive.
 *
 * <p>Iris's shadow pass re-submits block entities and entities. Glow, line and overlay geometry must not reach
 * the shadow map, so pipelines {@linkplain #excludeFromShadowPass excluded} here are dropped from custom-geometry
 * batches while shadows render (see {@code MixinCustomFeatureRenderer}), instead of guarding every renderer.
 */
public final class IrisCompat {

	private record Api(Object instance, Method enabled, Method shadow, Method assign, Class<?> program) {}

	private static final Set<RenderPipeline> SHADOW_EXCLUDED = ConcurrentHashMap.newKeySet();
	private static Api api;
	private static boolean resolved;

	private IrisCompat() {}

	private static synchronized Api api() {
		if (!resolved) {
			if (ModList.get().isLoaded("iris")) {
				try {
					Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
					Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
					api = new Api(type.getMethod("getInstance").invoke(null), type.getMethod("isShaderPackInUse"),
							type.getMethod("isRenderingShadowPass"),
							type.getMethod("assignPipeline", RenderPipeline.class, program), program);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Loaded Iris does not expose its supported rendering API", e);
				}
			}
			resolved = true;
		}
		return api;
	}

	public static boolean shadersEnabled() {
		return query(false);
	}

	public static boolean shadowPass() {
		return query(true);
	}

	private static boolean query(boolean shadow) {
		Api api = api();
		if (api == null)
			return false;
		try {
			return (boolean)(shadow ? api.shadow() : api.enabled()).invoke(api.instance());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot query the Iris render pass", e);
		}
	}

	/** Assigns {@code pipeline} to the Iris program named by an {@code IrisProgram} constant, e.g. "EMISSIVE_ENTITIES". */
	public static void assign(RenderPipeline pipeline, String program) {
		Api api = api();
		if (api == null)
			return;
		try {
			Object value = api.program().getMethod("valueOf", String.class).invoke(null, program);
			api.assign().invoke(api.instance(), pipeline, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot assign " + pipeline.getLocation() + " to Iris program " + program, e);
		}
	}

	/**
	 * Assigns {@code pipeline}'s shadow-pass program, named by an {@code IrisShadowProgram} constant such as
	 * "SHADOW_ENTITIES", for geometry that should cast shadows; Iris keeps a separate table for that pass.
	 */
	public static void assignShadow(RenderPipeline pipeline, String program) {
		if (api() == null)
			return;
		try {
			Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisShadowProgram");
			Object value = type.getMethod("valueOf", String.class).invoke(null, program);
			Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("assignPipelineShadow", RenderPipeline.class, type)
					.invoke(api.instance(), pipeline, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot assign " + pipeline.getLocation() + " to Iris shadow program " + program, e);
		}
	}

	/** Keeps {@code pipeline}'s custom geometry out of Iris's shadow map. */
	public static void excludeFromShadowPass(RenderPipeline pipeline) {
		SHADOW_EXCLUDED.add(pipeline);
	}

	/** Assigns {@code pipeline} and keeps it out of the shadow pass: the usual treatment for glows and overlays. */
	public static void assignWithoutShadow(RenderPipeline pipeline, String program) {
		assign(pipeline, program);
		excludeFromShadowPass(pipeline);
	}

	/** {@code submits} without shadow-excluded pipelines while Iris renders shadows; otherwise unchanged. */
	public static List<CustomFeatureRenderer.Submit> filterShadowPass(List<CustomFeatureRenderer.Submit> submits) {
		if (SHADOW_EXCLUDED.isEmpty() || submits.isEmpty() || !shadowPass())
			return submits;
		List<CustomFeatureRenderer.Submit> kept = null;
		for (int i = 0; i < submits.size(); i++) {
			CustomFeatureRenderer.Submit submit = submits.get(i);
			boolean excluded = SHADOW_EXCLUDED.contains(submit.renderType().pipeline());
			if (excluded && kept == null)
				kept = new ArrayList<>(submits.subList(0, i));
			else if (!excluded && kept != null)
				kept.add(submit);
		}
		return kept == null ? submits : kept;
	}
}
