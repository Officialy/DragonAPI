package reika.dragonapi.libraries.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import reika.dragonapi.interfaces.registry.CropType;

public enum ReikaCropHelper implements CropType {
    WHEAT(Blocks.WHEAT, CropBlock.AGE, 7, Items.WHEAT_SEEDS),
    CARROT(Blocks.CARROTS, CropBlock.AGE, 7, Items.CARROT),
    POTATO(Blocks.POTATOES, CropBlock.AGE, 7, Items.POTATO),
    NETHERWART(Blocks.NETHER_WART, NetherWartBlock.AGE, 3, Items.NETHER_WART),
    COCOA(Blocks.COCOA, CocoaBlock.AGE, 2, Items.COCOA_BEANS);

    public final Block blockID;
    public final int ripeAge;
    private final IntegerProperty age;
    private final Item seed;
    public static final ReikaCropHelper[] cropList = values();
    private static final Map<Block, ReikaCropHelper> CROPS = new IdentityHashMap<>();

    static {
        for (ReikaCropHelper crop : cropList) CROPS.put(crop.blockID, crop);
    }

    ReikaCropHelper(Block block, IntegerProperty age, int ripeAge, Item seed) {
        this.blockID = block;
        this.age = age;
        this.ripeAge = ripeAge;
        this.seed = seed;
    }

    public static ReikaCropHelper getCrop(Block block) { return CROPS.get(block); }
    public static boolean isCrop(Block block) { return CROPS.containsKey(block); }
    @Override public boolean isCrop(BlockState state) { return state.is(blockID); }
    @Override public boolean existsInGame() { return true; }
    @Override public boolean destroyOnHarvest() { return false; }
    @Override public boolean neverDropsSecondSeed() { return false; }
    @Override public boolean isSeedItem(ItemStack stack) { return stack.is(seed); }
    public ItemStack getSeedItem() { return new ItemStack(seed); }

    @Override public boolean isRipe(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isCrop(state) && state.getValue(age) == ripeAge;
    }

    @Override public int getGrowthState(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(age);
    }

    @Override public void setHarvested(Level level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(age, 0), 3);
    }

    @Override public void makeRipe(Level level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(age, ripeAge), 3);
    }

    @Override public List<ItemStack> getDrops(Level level, BlockPos pos, int fortune) {
        if (!(level instanceof ServerLevel server))
            throw new IllegalArgumentException("Crop loot is server-authoritative");
        ItemStack tool = new ItemStack(Items.IRON_HOE);
        if (fortune > 0)
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE), fortune);
        return new ArrayList<>(Block.getDrops(level.getBlockState(pos), server, pos,
                level.getBlockEntity(pos), null, tool));
    }
}
