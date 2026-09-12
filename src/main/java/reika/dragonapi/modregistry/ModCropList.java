package reika.dragonapi.modregistry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import reika.dragonapi.interfaces.registry.CropType;
import reika.dragonapi.libraries.registry.ReikaCropHelper;

/**
 * Runtime crop registry for post-metadata Minecraft. The old enum was a fixed list of reflective
 * integrations for 1.7.10 mods; modern crop blocks expose their growth state directly, so ordinary
 * {@link CropBlock}s can be adapted without knowing their owning mod. Crops with unusual mechanics
 * can register a complete {@link CropType} implementation instead.
 */
public final class ModCropList {

    private static final Map<Block, CropType> BLOCK_CROPS = new IdentityHashMap<>();
    private static final Map<Block, CropType> GENERATED_CROPS = new IdentityHashMap<>();
    private static final List<CropType> CUSTOM_CROPS = new CopyOnWriteArrayList<>();

    public static synchronized void registerCrop(Block block, CropType crop) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(crop, "crop");
        CropType previous = BLOCK_CROPS.putIfAbsent(block, crop);
        if (previous != null && previous != crop)
            throw new IllegalStateException("A crop handler is already registered for " + block);
    }

    public static void registerCrop(CropType crop) {
        Objects.requireNonNull(crop, "crop");
        if (!CUSTOM_CROPS.contains(crop)) CUSTOM_CROPS.add(crop);
    }

    public static CropType getModCrop(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (ReikaCropHelper.isCrop(block)) return null;
        synchronized (ModCropList.class) {
            CropType crop = BLOCK_CROPS.get(block);
            if (crop != null) return crop;
            crop = GENERATED_CROPS.get(block);
            if (crop != null) return crop;
        }
        for (CropType crop : CUSTOM_CROPS) {
            if (crop.existsInGame() && crop.isCrop(state)) return crop;
        }
        CropType generated = createGenericCrop(level, pos, state);
        if (generated == null) return null;
        synchronized (ModCropList.class) {
            return GENERATED_CROPS.computeIfAbsent(block, ignored -> generated);
        }
    }

    public static boolean isModCrop(Level level, BlockPos pos, BlockState state) {
        return getModCrop(level, pos, state) != null;
    }

    private static CropType createGenericCrop(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            IntegerProperty age = findAgeProperty(state, cropBlock.getAge(state), cropBlock.getMaxAge());
            if (age == null) return null;
            return new AgeCrop(state.getBlock(), age, cropBlock.getMaxAge(), 0,
                    state.getCloneItemStack(level, pos, false).getItem());
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return new AgeCrop(state.getBlock(), SweetBerryBushBlock.AGE,
                    SweetBerryBushBlock.MAX_AGE, 1, Items.SWEET_BERRIES);
        }
        return null;
    }

    private static IntegerProperty findAgeProperty(BlockState state, int currentAge, int maximumAge) {
        IntegerProperty fallback = null;
        for (Property<?> property : state.getProperties()) {
            if (!(property instanceof IntegerProperty integer)) continue;
            List<Integer> values = integer.getPossibleValues();
            if (values.getFirst() != 0 || values.getLast() != maximumAge
                    || state.getValue(integer) != currentAge) continue;
            if ("age".equals(integer.getName())) return integer;
            if (fallback == null) fallback = integer;
        }
        return fallback;
    }

    private static final class AgeCrop implements CropType {
        private final Block block;
        private final IntegerProperty age;
        private final int ripeAge;
        private final int harvestedAge;
        private final Item seed;

        private AgeCrop(Block block, IntegerProperty age, int ripeAge, int harvestedAge, Item seed) {
            this.block = block;
            this.age = age;
            this.ripeAge = ripeAge;
            this.harvestedAge = harvestedAge;
            this.seed = seed;
        }

        @Override public boolean existsInGame() { return true; }
        @Override public boolean destroyOnHarvest() { return false; }
        @Override public boolean neverDropsSecondSeed() { return seed == Items.AIR; }
        @Override public boolean isCrop(BlockState state) { return state.is(block) && state.hasProperty(age); }
        @Override public boolean isSeedItem(ItemStack stack) { return seed != Items.AIR && stack.is(seed); }

        @Override
        public boolean isRipe(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return isCrop(state) && state.getValue(age) >= ripeAge;
        }

        @Override
        public int getGrowthState(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return isCrop(state) ? state.getValue(age) : -1;
        }

        @Override
        public void setHarvested(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (isCrop(state)) level.setBlock(pos, state.setValue(age, harvestedAge), 3);
        }

        @Override
        public void makeRipe(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (isCrop(state)) level.setBlock(pos, state.setValue(age, ripeAge), 3);
        }

        @Override
        public List<ItemStack> getDrops(Level level, BlockPos pos, int fortune) {
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

    private ModCropList() {}
}
