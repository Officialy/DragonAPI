package reika.dragonapi.modregistry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WoodSpeciesTest {
    @Test void distinctBlocksRetainEveryStateWithDefaultFirst() {
        var wood = WoodSpecies.ofBlocks(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING, false);
        assertSame(Blocks.OAK_LOG.defaultBlockState(), wood.logs().getFirst());
        assertEquals(3, wood.logs().size());
        assertTrue(wood.logs().stream().anyMatch(s -> s.getValue(BlockStateProperties.AXIS) == Direction.Axis.X));
        assertEquals(28, wood.leaves().size());
        assertEquals(2, wood.saplings().size());
        var states = new ArrayList<>(wood.logs());
        var copy = new WoodSpecies(states, wood.leaves(), List.of(), false);
        states.clear(); assertEquals(3, copy.logs().size());
        assertThrows(UnsupportedOperationException.class, () -> copy.logs().clear());
    }

    @Test void sharedBlockDiscriminatorsKeepDistinctImmutableStates() {
        var up = Blocks.MUSHROOM_STEM.defaultBlockState().setValue(BlockStateProperties.UP, true);
        var down = up.setValue(BlockStateProperties.UP, false);
        var wood = new WoodSpecies(List.of(up, down), List.of(Blocks.OAK_LEAVES.defaultBlockState()), List.of(), false);
        assertEquals(List.of(up, down), wood.logs());
        assertNotSame(up, down);
        assertThrows(UnsupportedOperationException.class, () -> wood.logs().clear());
    }

    @Test void leafAndSaplingAssociationsPreserveMultipleBlocks() {
        var wood = new WoodSpecies(List.of(Blocks.OAK_LOG.defaultBlockState()),
                List.of(Blocks.OAK_LEAVES.defaultBlockState(), Blocks.AZALEA_LEAVES.defaultBlockState()),
                List.of(Blocks.OAK_SAPLING.defaultBlockState(), Blocks.BIRCH_SAPLING.defaultBlockState()), false);
        assertEquals(2, wood.leaves().size()); assertEquals(2, wood.saplings().size());
        assertTrue(wood.saplings().get(1).is(Blocks.BIRCH_SAPLING));
        assertThrows(UnsupportedOperationException.class, () -> wood.leaves().clear());
        assertThrows(UnsupportedOperationException.class, () -> wood.saplings().clear());
    }

    @Test void naturalLeafClassificationRequiresMembershipAndRespectsAuthoredNonDecay() {
        var persistent = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        var natural = persistent.setValue(BlockStateProperties.PERSISTENT, false);
        var wood = WoodSpecies.ofBlocks(Blocks.OAK_LOG, Blocks.OAK_LEAVES, null, false);
        assertTrue(wood.isNaturalLeaf(natural)); assertFalse(wood.isNaturalLeaf(persistent));
        assertFalse(wood.isNaturalLeaf(Blocks.BIRCH_LEAVES.defaultBlockState()));
        assertTrue(WoodSpecies.ofBlocks(Blocks.OAK_LOG, Blocks.OAK_LEAVES, null, true).isNaturalLeaf(persistent));
        assertNull(WoodSpecies.stateOf(null)); assertNull(WoodSpecies.stateOf(ItemStack.EMPTY));

        assertThrows(IllegalArgumentException.class, () -> new WoodSpecies(List.of(), List.of(natural), List.of(), false));
    }

    @SuppressWarnings("deprecation")
    @Test void historicalOreOrdinalDecoderRemainsLosslessAndDoesNotReadItemDamage() {
        for (var ore : ModOreList.values()) {
            assertSame(ore, ModOreList.getByLegacyOrdinal(ore.ordinal()));
            assertSame(ore, ModOreList.getEntryFromDamage(ore.ordinal()));
            assertSame(ore, ModOreList.getByEnumName(ore.name()));
        }
        assertNull(ModOreList.getByLegacyOrdinal(-1));
        assertNull(ModOreList.getByLegacyOrdinal(Integer.MAX_VALUE));
        assertNull(ModOreList.getByLegacyOrdinal(ModOreList.values().length));
    }
}
