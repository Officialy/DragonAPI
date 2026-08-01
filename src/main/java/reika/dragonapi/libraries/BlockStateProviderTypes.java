package reika.dragonapi.libraries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

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

	public static final DeferredRegister<BlockStateProviderType<?>> REGISTRY =
			DeferredRegister.create(BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, DragonAPI.MODID);

	/** Uniform pick from an explicit list of states. */
	public static final DeferredHolder<BlockStateProviderType<?>, BlockStateProviderType<RandomSingleStateProvider>>
			RANDOM_SINGLE_STATE_PROVIDER = REGISTRY.register("random_single_state",
					() -> new BlockStateProviderType<>(RandomSingleStateProvider.CODEC));

	/** Uniform pick from every block in a tag, resolved lazily so datapack contents are visible. */
	public static final DeferredHolder<BlockStateProviderType<?>, BlockStateProviderType<RandomTagSingleStateProvider>>
			RANDOM_TAG_SINGLE_STATE_PROVIDER = REGISTRY.register("random_tag_single_state",
					() -> new BlockStateProviderType<>(RandomTagSingleStateProvider.CODEC));

	private BlockStateProviderTypes() {}
}
