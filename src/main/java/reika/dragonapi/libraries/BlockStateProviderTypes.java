package reika.dragonapi.libraries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.dragonapi.DragonAPI;

/**
 * Block-state providers shared by the Reika mods' worldgen.
 *
 * <p>Vanilla's {@code WeightedStateProvider} needs every state listed up front with a weight, which
 * cannot express "pick uniformly from whatever is in this block tag" — the tag is resolved from
 * datapacks after the feature is built. These two fill that gap.
 */
public final class BlockStateProviderTypes {

	public static final DeferredRegister<MapCodec<? extends BlockStateProvider>> REGISTRY =
			DeferredRegister.create(Registries.BLOCK_STATE_PROVIDER_TYPE, DragonAPI.MODID);

	/** Uniform pick from an explicit list of states. */
	public static final DeferredHolder<MapCodec<? extends BlockStateProvider>, MapCodec<RandomSingleStateProvider>>
			RANDOM_SINGLE_STATE_PROVIDER = REGISTRY.register("random_single_state",
					() -> RandomSingleStateProvider.CODEC);

	/** Uniform pick from every block in a tag, resolved lazily so datapack contents are visible. */
	public static final DeferredHolder<MapCodec<? extends BlockStateProvider>, MapCodec<RandomTagSingleStateProvider>>
			RANDOM_TAG_SINGLE_STATE_PROVIDER = REGISTRY.register("random_tag_single_state",
					() -> RandomTagSingleStateProvider.CODEC);

	private BlockStateProviderTypes() {}
}
