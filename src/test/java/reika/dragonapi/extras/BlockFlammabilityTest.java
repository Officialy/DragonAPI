package reika.dragonapi.extras;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockFlammabilityTest {
    @Test void commonAndModernWoodRetainNativeFlammability() {
        for (var block : new Block[]{Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
                Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS,
                Blocks.CHERRY_PLANKS, Blocks.PALE_OAK_PLANKS, Blocks.POPLAR_PLANKS, Blocks.BAMBOO_PLANKS,
                Blocks.BAMBOO_MOSAIC, Blocks.STRIPPED_OAK_LOG, Blocks.POPLAR_LOG, Blocks.OAK_WOOD,
                Blocks.OAK_LEAVES, Blocks.YELLOW_POPLAR_LEAVES, Blocks.OAK_SLAB, Blocks.OAK_STAIRS})
            assertTrue(BlockProperties.isFlammable(block), block.toString());
        for (var type : SlabType.values())
            assertTrue(BlockProperties.isFlammable(Blocks.OAK_SLAB.defaultBlockState()
                    .setValue(BlockStateProperties.SLAB_TYPE, type)), type.toString());
    }

    @Test void everyWoolColourAndShapeRetainsNativeFlammability() {
        Blocks.WOOL.forEach(block -> assertTrue(BlockProperties.isFlammable(block), block.toString()));
        Blocks.WOOL_STAIRS.forEach(block -> assertTrue(BlockProperties.isFlammable(block), block.toString()));
        Blocks.WOOL_SLAB.forEach(block -> assertTrue(BlockProperties.isFlammable(block), block.toString()));
        Blocks.CARPET.forEach(block -> assertTrue(BlockProperties.isFlammable(block), block.toString()));
    }

    @Test void reinitializingPolicyRetainsStateSensitiveWaterlogging() {
        BlockProperties.setFlammable();
        for (var block : new Block[]{Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.MANGROVE_ROOTS}) {
            var dry = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
            assertTrue(BlockProperties.isFlammable(dry));
            assertFalse(BlockProperties.isFlammable(dry.setValue(BlockStateProperties.WATERLOGGED, true)));
        }
    }
}
