package reika.dragonapi.libraries.level;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceKey;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.registries.DeferredHolder;

import reika.dragonapi.DragonAPI;

/**
 * Reproduces, as block tags, the motion and fluid behaviour 26.2 computed from block shapes.
 *
 * <p>26.2 answered "does this block stop movement?" with {@code BlockStateBase#blocksMotion()}:
 * {@code isSolid()}, except for cobwebs and bamboo saplings. 26.3 removed that method and asks
 * {@code #minecraft:blocks_motion} instead. That tag also feeds {@code blocks_motion_in_heightmap*},
 * {@code causes_suffocation} and {@code blocks_fluid_flow}. Vanilla fills it from
 * {@code blocks_motion_no_leaves} plus {@code #minecraft:leaves}, and NeoForge does not add modded
 * blocks, so an untagged solid block would be passable for heightmaps, suffocation and fluid flow.
 *
 * <p>Likewise 26.2 let flowing fluid wash away any block that did not block motion, except doors,
 * standing and wall signs, and a handful of vanilla blocks ({@code FlowingFluid#canHoldAnyFluid}).
 * 26.3 washes away only {@code #minecraft:washed_away_by_fluids}. Vanilla's own list is exactly that
 * rule's output, including water, lava, air and light.
 *
 * <p>{@code isSolid()} is unchanged between the two versions (26.3 only adds debug logging), so
 * applying the 26.2 rule to it gives each modded block its 26.2 behaviour. The tags are per block
 * while the rule was per state. A block whose states disagree is classified by its default state,
 * and the disagreement is logged at datagen time.
 *
 * <p>The shape rule is the 26.2 port's behaviour, not 1.7.10's. 1.7.10 judged fluids, precipitation
 * height and teleport landing by the block's {@code Material.blocksMovement()}, so a thin block made of
 * {@code Material.iron} (a steam line or a wire) stopped fluids and rain and was never washed away. A mod
 * passes {@code legacyMaterialBlocksMovement} to restore that. Such blocks go into
 * {@code blocks_motion_no_leaves} whatever their shape. That tag's suffocation consumer still requires a
 * full collision shape, so a thin block does not become suffocating.
 */
public final class LegacyMotionTags {

	private LegacyMotionTags() {}

	/**
	 * Sorts blocks into the tags that reproduce their 26.2 behaviour.
	 *
	 * @param blocksMotion receives solid non-leaf blocks, for {@code #minecraft:blocks_motion_no_leaves}
	 * @param leaves       receives leaf blocks, for {@code #minecraft:leaves} (26.2's heightmaps
	 *                     separated leaves by {@code instanceof LeavesBlock}; 26.3 uses this tag)
	 * @param washedAway   receives blocks flowing fluid replaced in 26.2, for
	 *                     {@code #minecraft:washed_away_by_fluids}
	 */
	public static void classify(Iterable<? extends Block> blocks, Consumer<Block> blocksMotion,
			Consumer<Block> leaves, Consumer<Block> washedAway) {
		classify(blocks, block -> false, blocksMotion, leaves, washedAway);
	}

	/**
	 * As {@link #classify(Iterable, Consumer, Consumer, Consumer)}, with the 1.7.10 material rule: blocks
	 * matching {@code legacyMaterialBlocksMovement} block motion even when their shape is not solid.
	 */
	public static void classify(Iterable<? extends Block> blocks, Predicate<Block> legacyMaterialBlocksMovement,
			Consumer<Block> blocksMotion, Consumer<Block> leaves, Consumer<Block> washedAway) {
		for (Block block : blocks) {
			if (block instanceof LeavesBlock) {
				leaves.accept(block);
				continue;
			}
			if (legacyMaterialBlocksMovement.test(block) || blocksMotion(block))
				blocksMotion.accept(block);
			else if (washableWhenPassable(block))
				washedAway.accept(block);
		}
	}

	/** {@link #classify} over a mod's block registry entries, handing out their registry keys. */
	public static void classifyEntries(Iterable<? extends DeferredHolder<Block, ? extends Block>> entries,
			Consumer<ResourceKey<Block>> blocksMotion, Consumer<ResourceKey<Block>> leaves,
			Consumer<ResourceKey<Block>> washedAway) {
		classifyEntries(entries, block -> false, blocksMotion, leaves, washedAway);
	}

	/** {@link #classify(Iterable, Predicate, Consumer, Consumer, Consumer)} over registry entries. */
	public static void classifyEntries(Iterable<? extends DeferredHolder<Block, ? extends Block>> entries,
			Predicate<Block> legacyMaterialBlocksMovement, Consumer<ResourceKey<Block>> blocksMotion,
			Consumer<ResourceKey<Block>> leaves, Consumer<ResourceKey<Block>> washedAway) {
		for (DeferredHolder<Block, ? extends Block> holder : entries) {
			ResourceKey<Block> key = holder.getKey();
			classify(java.util.List.of(holder.get()), legacyMaterialBlocksMovement, b -> blocksMotion.accept(key),
					b -> leaves.accept(key), b -> washedAway.accept(key));
		}
	}

	/** 26.2 {@code blocksMotion()} for a modded block, judged on the default state. */
	public static boolean blocksMotion(Block block) {
		boolean byDefault = solid(block.defaultBlockState());
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			if (solid(state) != byDefault) {
				DragonAPI.LOGGER.warn("{} is solid in some states and not others; tagging it by its default state ({})",
						block, byDefault ? "blocks motion" : "passable");
				break;
			}
		}
		return byDefault;
	}

	/** 26.2 {@code canHoldAnyFluid}'s exclusions for a block that does not block motion. */
	private static boolean washableWhenPassable(Block block) {
		return !(block instanceof DoorBlock) && !(block instanceof StandingSignBlock) && !(block instanceof WallSignBlock);
	}

	private static boolean solid(BlockState state) {
		// isSolid() reads a cache that is false until built; build it rather than depend on datagen order.
		state.initCache();
		return state.isSolid();
	}
}
