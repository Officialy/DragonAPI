package reika.dragonapi.libraries.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
// IFluidBlock removed in NeoForge - fluid blocks are handled differently
import net.minecraft.core.registries.BuiltInRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.BlockTieredResource;
import reika.dragonapi.extras.BlockProperties;
import reika.dragonapi.instantiable.data.maps.BlockMap;
import reika.dragonapi.interfaces.block.SemiUnbreakable;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.ArrayList;
import java.util.Collection;

public class ReikaBlockHelper {

    private static final BlockMap<ItemStack> silkTouchDrops = new BlockMap<>();

    static {
        addSilkTouchDrop(Blocks.REDSTONE_WIRE, Items.REDSTONE);
        addSilkTouchDrop(Blocks.PUMPKIN_STEM, Items.PUMPKIN_SEEDS);
        addSilkTouchDrop(Blocks.MELON_STEM, Items.MELON_SEEDS);
        addSilkTouchDrop(Blocks.WHEAT, Items.WHEAT);
        addSilkTouchDrop(Blocks.CARROTS, Items.CARROT);
        addSilkTouchDrop(Blocks.POTATOES, Items.POTATO);
        addSilkTouchDrop(Blocks.NETHER_WART, Items.NETHER_WART);
        addSilkTouchDrop(Blocks.BED.blue(), Items.BED.blue());
        addSilkTouchDrop(Blocks.BREWING_STAND, Items.BREWING_STAND);
        addSilkTouchDrop(Blocks.CAULDRON, Items.CAULDRON);
        addSilkTouchDrop(Blocks.FLOWER_POT, Items.FLOWER_POT);
        addSilkTouchDrop(Blocks.TRIPWIRE, Items.STRING);
        //addSilkTouchDrop(Blocks.WOODEN_DOOR, Items.WOODEN_DOOR);
        addSilkTouchDrop(Blocks.IRON_DOOR, Items.IRON_DOOR);
        addSilkTouchDrop(Blocks.SUGAR_CANE, Items.SUGAR_CANE);
        addSilkTouchDrop(Blocks.FARMLAND, Blocks.DIRT);
    }

    public static boolean isGroundType(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b == Blocks.DIRT.defaultBlockState() || b == Blocks.GRASS_BLOCK.defaultBlockState() ||
                b == Blocks.STONE.defaultBlockState() || b == Blocks.SAND.defaultBlockState() ||
                b == Blocks.SANDSTONE.defaultBlockState() || b == Blocks.CLAY.defaultBlockState() ||
                b == Blocks.GRAVEL.defaultBlockState() || b == Blocks.SNOW.defaultBlockState())
            return true;
        MapColor mat = b.getMapColor(world, pos);
        return mat == MapColor.STONE;// || mat.isReplaceable();//todo? b.isReplaceableOreGen(world, pos, Blocks.stone);
    }

    public static boolean isGroundType(WorldGenLevel world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b == Blocks.DIRT.defaultBlockState() || b == Blocks.GRASS_BLOCK.defaultBlockState() ||
                b == Blocks.STONE.defaultBlockState() || b == Blocks.SAND.defaultBlockState() ||
                b == Blocks.SANDSTONE.defaultBlockState() || b == Blocks.CLAY.defaultBlockState() ||
                b == Blocks.GRAVEL.defaultBlockState() || b == Blocks.SNOW.defaultBlockState())
            return true;
        MapColor mat = b.getMapColor(world, pos);
        return mat == MapColor.STONE;// || mat.isReplaceable();//todo? b.isReplaceableOreGen(world, pos, Blocks.stone);
    }

    public static boolean isWood(BlockGetter world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        return b.defaultMapColor() == MapColor.WOOD;
    }

    public static boolean isLeaf(BlockGetter world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        return b instanceof LeavesBlock;
    }

    public static boolean isStairBlock(Block id) {
        if (id == Blocks.STONE_STAIRS)
            return true;
        if (id == Blocks.STONE_BRICK_STAIRS)
            return true;
        if (id == Blocks.BRICK_STAIRS)
            return true;
        if (id == Blocks.SANDSTONE_STAIRS)
            return true;
        if (id == Blocks.OAK_STAIRS)
            return true;
        if (id == Blocks.NETHER_BRICK_STAIRS)
            return true;
        if (id == Blocks.SPRUCE_STAIRS)
            return true;
        if (id == Blocks.BIRCH_STAIRS)
            return true;
        if (id == Blocks.JUNGLE_STAIRS)
            return true;
        if (id instanceof StairBlock)
            return true; //Should work, though if a mod is using stairs and isn't a stair, it won't work right
        return id == Blocks.QUARTZ_STAIRS;
    }

    /**
     * Returns true if the block has a hitbox. Args: World, x, y, z
     */
    public static boolean isCollideable(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b.getBlock() == Blocks.AIR)
            return false;
        return (b.getPistonPushReaction() == PushReaction.IMMOVEABLE && !BlockProperties.isNonSolid(b.getBlock()) && b.getCollisionShape(world, pos) != null);
    }

    public static ItemStack getSilkTouch(Level world, BlockPos pos, Block id, Player ep, boolean dropFluids) {
        if (id == Blocks.AIR || id == Blocks.MOVING_PISTON || id == Blocks.PISTON_HEAD || id == Blocks.FIRE)
            return null;
        if (id == Blocks.NETHER_PORTAL || id == Blocks.END_PORTAL)
            return null;
        if ((id instanceof DoorBlock || id instanceof BedBlock))
            return null;
        ItemStack get = silkTouchDrops.get(id);
        if (get != null)
            return get;
        if (Item.BY_BLOCK.get(id).getDefaultInstance() == ItemStack.EMPTY) {
            DragonAPI.LOGGER.error("Something tried to silktouch null-item block " + BuiltInRegistries.BLOCK.getKey(id).getNamespace());
            return null;
        }
        if (ReikaBlockHelper.isLiquid(id.defaultBlockState()))// && !(dropFluids && ReikaWorldHelper.isLiquidSourceBlock(world, pos)))
            return null;
        //if (ModList.THAUMCRAFT.isLoaded() && id == BlockEntry.NODE.getBlock())
        //   return null;
        if (id instanceof BlockTieredResource b) {
            if (ep != null && b.isPlayerSufficientTier(world, pos, ep)) {
                return ReikaItemHelper.collateItemList(b.getHarvestResources(world, pos, 0, ep)).get(0);
            } else {
                Collection<ItemStack> li = b.getNoHarvestResources(world, pos, 0, ep);
                return li.isEmpty() ? null : new ArrayList<>(li).get(0);
            }
        }
        return new ItemStack(id, 1);
    }

    public static double getBlockVolume(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        VoxelShape shape = b.getShape(world, pos);
		if (shape.isEmpty())
			return 0;
		var bounds = shape.bounds();
		double dx = bounds.maxX-bounds.minX;
		double dy = bounds.maxY-bounds.minY;
		double dz = bounds.maxZ-bounds.minZ;
        return dx*dy*dz;
    }

    private static void addSilkTouchDrop(Block b, Block drop) {
        addSilkTouchDrop(b, new ItemStack(drop));
    }

    private static void addSilkTouchDrop(Block b, Item drop) {
        addSilkTouchDrop(b, new ItemStack(drop));
    }

    private static void addSilkTouchDrop(Block b, ItemStack drop) {
        silkTouchDrops.put(b, drop);
    }

    /**
     * Tests if a block is a liquid Blocks. Args: ID
     */
    public static boolean isLiquid(BlockState b) {
        if (b.getBlock() == Blocks.AIR)
            return false;
        return b.getBlock() instanceof LiquidBlock || !b.getFluidState().isEmpty();
    }

    public static boolean isUnbreakable(Level world, BlockPos pos, Block id, Player ep) {
//        if (id.getBlockHardness(world, pos) < 0 || (ep != null && id.getPlayerRelativeBlockHardness(ep, world, pos) < 0))
//            return true; todo if block is unbreakable
        return id instanceof SemiUnbreakable && ((SemiUnbreakable) id).isUnbreakable(world, pos);
    }


    private static java.lang.reflect.Method pistonMoveBlocks;

    /**
     * 1.7.10 {@code extendPiston}: extends an unpowered piston as if it had just received a signal, by calling the
     * piston's private move routine directly (upstream reflected {@code tryExtend}). The piston, still unpowered,
     * retracts by itself on its next neighbour update, exactly as it did upstream. This mirrors 26.3's own
     * {@code PistonBaseBlock.triggerEvent} extend branch: the move event, the move, the extended state, the sound
     * and the game event.
     */
    public static boolean extendPiston(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock piston)
                || state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED))
            return false;
        net.minecraft.core.Direction direction = state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING);
        if (net.neoforged.neoforge.event.EventHooks.onPistonMovePre(level, pos, direction, true))
            return false;
        try {
            if (pistonMoveBlocks == null) {
                pistonMoveBlocks = net.minecraft.world.level.block.piston.PistonBaseBlock.class.getDeclaredMethod(
                        "moveBlocks", Level.class, BlockPos.class, net.minecraft.core.Direction.class, boolean.class);
                pistonMoveBlocks.setAccessible(true);
            }
            if (!(Boolean)pistonMoveBlocks.invoke(piston, level, pos, direction, true))
                return false;
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not drive piston at " + pos, e);
        }
        BlockState extended = state.setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED, true);
        level.setBlock(pos, extended, 67);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.BLOCKS,
                0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_ACTIVATE, pos,
                net.minecraft.world.level.gameevent.GameEvent.Context.of(extended));
        return true;
    }
}

