package reika.dragonapi.extras;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import reika.dragonapi.libraries.level.LegacyFlammabilityTags;

import java.util.HashMap;
import java.util.function.Predicate;

public class BlockProperties {
    /** Live native fire data and reloadable legacy exceptions, rather than a frozen block list. */
    private static volatile Predicate<BlockState> flammablePolicy;

    /** A catalogue of all soft (replaceable, like water, tall grass, etc) blocks by ID. */
    private static final HashMap<Block, Boolean> softBlocksArray = new HashMap();

    /** A catalogue of all nonsolid (no hitbox) blocks by ID. */
    private static final HashMap<Block, Boolean> nonSolidArray = new HashMap<>();

    /** A catalogue of all block colors by ID. */
    public static int[] blockColorArray = new int[4096];

    public static void setNonSolid() {
        nonSolidArray.put(Blocks.AIR, true);
        //nonSolidArray.put(Blocks.piston_extenstion, true); //block 36
        nonSolidArray.put(Blocks.WATER, true);
        nonSolidArray.put(Blocks.LAVA, true);
        nonSolidArray.put(Blocks.TALL_GRASS, true);
        nonSolidArray.put(Blocks.DEAD_BUSH, true);
        nonSolidArray.put(Blocks.FIRE, true);
        nonSolidArray.put(Blocks.SNOW, true);
        nonSolidArray.put(Blocks.VINE, true);
        nonSolidArray.put(Blocks.TORCH, true);
//        nonSolidArray.put(Blocks.SAPLING, true);
        nonSolidArray.put(Blocks.RAIL, true);
        nonSolidArray.put(Blocks.POWERED_RAIL, true);
        nonSolidArray.put(Blocks.DETECTOR_RAIL, true);
        nonSolidArray.put(Blocks.DANDELION, true);
        nonSolidArray.put(Blocks.POPPY, true);
        nonSolidArray.put(Blocks.BROWN_MUSHROOM, true);
        nonSolidArray.put(Blocks.RED_MUSHROOM, true);
        nonSolidArray.put(Blocks.REDSTONE_WIRE, true);
        nonSolidArray.put(Blocks.WHEAT, true);
        nonSolidArray.put(Blocks.OAK_SIGN, true);
        nonSolidArray.put(Blocks.OAK_DOOR, true);
        nonSolidArray.put(Blocks.IRON_DOOR, true);
        nonSolidArray.put(Blocks.LADDER, true);
        nonSolidArray.put(Blocks.OAK_PRESSURE_PLATE, true);
        nonSolidArray.put(Blocks.STONE_PRESSURE_PLATE, true);
        nonSolidArray.put(Blocks.LEVER, true);
        nonSolidArray.put(Blocks.STONE_BUTTON, true);
        nonSolidArray.put(Blocks.CARROTS, true);
        nonSolidArray.put(Blocks.POTATOES, true);
        nonSolidArray.put(Blocks.REDSTONE_TORCH, true);
        nonSolidArray.put(Blocks.SUGAR_CANE, true);
        nonSolidArray.put(Blocks.NETHER_PORTAL, true);
        nonSolidArray.put(Blocks.REPEATER, true);
        nonSolidArray.put(Blocks.OAK_TRAPDOOR, true);
        //nonSolidArray.put(Blocks.IRON_BARS, true);
        //nonSolidArray.put(Blocks.GLASS_PANE, true);
        nonSolidArray.put(Blocks.PUMPKIN_STEM, true);
        nonSolidArray.put(Blocks.MELON_STEM, true);
        nonSolidArray.put(Blocks.LILY_PAD, true);
        nonSolidArray.put(Blocks.END_PORTAL, true);
        nonSolidArray.put(Blocks.NETHER_WART, true);
        nonSolidArray.put(Blocks.TRIPWIRE, true);
        nonSolidArray.put(Blocks.TRIPWIRE_HOOK, true);
        nonSolidArray.put(Blocks.FLOWER_POT, true);
        nonSolidArray.put(Blocks.OAK_BUTTON, true);
        nonSolidArray.put(Blocks.SKELETON_SKULL, true);

    }

    public static void setSoft() {
        softBlocksArray.put(Blocks.AIR, true);
        softBlocksArray.put(Blocks.MOVING_PISTON, true);
        softBlocksArray.put(Blocks.WATER, true);
        softBlocksArray.put(Blocks.LAVA, true);
        softBlocksArray.put(Blocks.TALL_GRASS, true);
        softBlocksArray.put(Blocks.DEAD_BUSH, true);
        softBlocksArray.put(Blocks.FIRE, true);
        softBlocksArray.put(Blocks.SNOW, true);
        softBlocksArray.put(Blocks.VINE, true);
    }

    public static void setFlammable() {
        // A world-free catalogue cannot call contextual block overrides. This is the same
        // native table used by NeoForge's default getFlammability implementation, and querying
        // it live includes modern species without declaring fireproof Nether planks flammable.
        FireBlock fire = (FireBlock)Blocks.FIRE;
        flammablePolicy = state -> fire.getBurnOdds(state) > 0
                || (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                    || !state.getValue(BlockStateProperties.WATERLOGGED))
                    && state.is(LegacyFlammabilityTags.FLAMMABLE);
    }

    static {
        setNonSolid();
        setSoft();
        setFlammable();
    }

    public static boolean isFlammable(Block b) {
        return isFlammable(b.defaultBlockState());
    }

    /** State-aware catalogue query; tag membership is resolved again after every reload. */
    public static boolean isFlammable(BlockState state) {
        return flammablePolicy.test(state);
    }

    public static boolean isSoft(Block b) {
        return softBlocksArray.containsKey(b) && softBlocksArray.get(b);
    }

    public static boolean isNonSolid(Block b) {
        return nonSolidArray.containsKey(b) && nonSolidArray.get(b);
    }
}
