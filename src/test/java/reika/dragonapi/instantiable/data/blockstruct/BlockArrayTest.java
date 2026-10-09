package reika.dragonapi.instantiable.data.blockstruct;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import static org.junit.jupiter.api.Assertions.*;

class BlockArrayTest {
    @Test void rotationsUseExactIntegerPivotsAndRetainOrderSettingsAndSource() {
        var positions = List.of(new BlockPos(-7, 2, 9), new BlockPos(-3, -1, 4), new BlockPos(-5, 6, 8));
        var source = new BlockArray(positions);
        source.maxDepth = 17; source.extraSpread = true; source.taxiCabDistance = true;
        source.clampToChunkLoad = true; source.bounds = new BlockBox(-20, 20);
        var left = source.rotate90Degrees(-5, 8, true);
        assertEquals(List.of(new BlockPos(-4, 2, 10), new BlockPos(-9, -1, 6), new BlockPos(-5, 6, 8)), list(left));
        assertEquals(positions, list(left.rotate90Degrees(-5, 8, false)));
        assertEquals(positions, list(source.rotate180Degrees(-5, 8).rotate180Degrees(-5, 8)));
        assertEquals(positions, list(source));
        assertEquals(-9, left.getMinX()); assertEquals(-4, left.getMaxX());
        assertEquals(17, left.maxDepth); assertSame(source.bounds, left.bounds);
        assertTrue(left.extraSpread && left.taxiCabDistance && left.clampToChunkLoad);
        assertTrue(new BlockArray().rotate90Degrees(3, -4, true).isEmpty());
        source.clear(); assertEquals(Integer.MAX_VALUE, source.getMinX());
    }

    private static List<BlockPos> list(BlockArray array) {
        var result = new ArrayList<BlockPos>(); array.forEach(result::add); return result;
    }

    @Test void worldFreeFilledTransformsPreserveStatesAlternativesAndIndependentMembership() {
        var source = new FilledBlockArray(null);
        var state = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        source.setBlock(3, 5, 7, state);
        source.addBlock(3, 5, 7, Blocks.STONE);
        source.setEmpty(4, 5, 7, true, false, Blocks.STONE);
        var rotated = source.rotate90Degrees(2, 6, true);
        assertEquals(state, rotated.getBlockKeyAt(3, 5, 5).blockID);
        assertTrue(rotated.hasBlockAt(3, 5, 5, Blocks.STONE.defaultBlockState()));
        rotated.addBlock(3, 5, 5, Blocks.DIRT);
        assertFalse(source.hasBlockAt(3, 5, 7, Blocks.DIRT.defaultBlockState()), "Alternative containers cannot alias the source");
        rotated.remove(new BlockPos(3, 5, 5));
        assertEquals(2, source.getSize()); assertEquals(1, rotated.getSize());
        assertEquals(source.keySet(), source.rotate180Degrees(2, 6).rotate180Degrees(2, 6).keySet());
        assertEquals(source.keySet(), source.flipX().flipX().keySet());
    }

    @Test void replacementsCopiesAndMergesKeepEveryLayerConsistentWithoutAWorld() {
        var source = new FilledBlockArray(null); source.setBlock(1, 2, 3, Blocks.STONE);
        source.setBlock(1, 2, 3, Blocks.DIRT);
        assertEquals(0, source.getNumberOf(Blocks.STONE)); assertEquals(1, source.getNumberOf(Blocks.DIRT));
        var target = new FilledBlockArray(null); target.setBlock(9, 8, 7, Blocks.GOLD_BLOCK);
        source.copyTo(target);
        assertFalse(target.hasBlock(9, 8, 7)); assertEquals(0, target.getNumberOf(Blocks.GOLD_BLOCK));
        assertEquals(1, target.getNumberOf(Blocks.DIRT));
        source.copyTo(source); assertEquals(1, source.getSize());
        var declared = new StructuredBlockArray(null);
        declared.addAll(source);
        assertEquals(Blocks.DIRT, declared.getBlockAt(1, 2, 3));
        var merged = new FilledBlockArray(null); merged.addAll(declared);
        assertTrue(merged.hasBlockAt(1, 2, 3, Blocks.DIRT.defaultBlockState()));
        merged.clear(); assertTrue(merged.keySet().isEmpty()); assertEquals(0, merged.getNumberOf(Blocks.DIRT));
    }

    @Test void rectangularBlueprintImportRetainsOriginalOrientationInAllFourDirections() {
        var blueprint = new SlicedBlockBlueprint();
        blueprint.addMapping('s', Blocks.STONE); blueprint.addAntiMapping('d', Blocks.DIRT);
        blueprint.addSlice("s-x", "xds"); blueprint.addSlice("s");
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            var array = new FilledBlockArray(null); array.fillFrom(blueprint, -4, 3, 6, direction);
            assertEquals(5, array.getSize());
            assertTrue(array.hasBlockAt(-4, 3, 6, Blocks.STONE.defaultBlockState()));
            assertTrue(array.hasBlock(-4, 4, 6));
            assertFalse(array.hasBlock(-4, 5, 6));
            BlockPos anti = new BlockPos(-4, 4, 6).relative(direction.getCounterClockWise());
            assertFalse(array.hasBlockAt(anti.getX(), anti.getY(), anti.getZ(), Blocks.DIRT.defaultBlockState()));
            assertTrue(array.hasBlockAt(anti.getX(), anti.getY(), anti.getZ(), Blocks.STONE.defaultBlockState()));
            BlockPos next = new BlockPos(-4, 3, 6).relative(direction);
            assertEquals(Blocks.STONE, array.getBlockAt(next.getX(), next.getY(), next.getZ()));
        }
        var untouched = new FilledBlockArray(null);
        assertThrows(MisuseException.class, () -> untouched.fillFrom(blueprint, 0, 0, 0, Direction.UP));
        assertTrue(untouched.isEmpty());
        assertThrows(MisuseException.class, () -> blueprint.addSlice(""));
    }
}
