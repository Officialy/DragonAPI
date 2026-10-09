package reika.dragonapi.modregistry;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.registry.TreeType;
import static org.junit.jupiter.api.Assertions.*;

class TreeBreakDepthTest {
    @Test void catalogueRetainsExactlyTheOriginalSpecialLimitsAndDefault() {
        var expected = Map.of(ModWoodList.SEQUOIA, 350, ModWoodList.TWILIGHTOAK, 200,
                ModWoodList.DARKWOOD, 32, ModWoodList.GIANTPINKTREE, 180);
        for (var wood : ModWoodList.values()) {
            assertEquals(expected.getOrDefault(wood, 30).intValue(), wood.getRecursiveBreakDepth(), wood.name());
        }
        assertEquals(30, ModWoodList.NATURADARKWOOD.getRecursiveBreakDepth(), "Natura darkwood is not Twilight Darkwood");
        assertEquals(30, ModWoodList.REDWOOD.getRecursiveBreakDepth(), "BOP redwood is not Natura Sequoia");
        assertEquals(30, ModWoodList.PINKBIRCH.getRecursiveBreakDepth(), "Pink birch is not the giant tree");
    }

    @Test void specialDepthDoesNotReplaceOrEnlargeOriginalSpatialBounds() {
        var expected = Map.of(ModWoodList.SEQUOIA, new BlockBox(-51, -251, -51, 51, 251, 51),
                ModWoodList.TWILIGHTOAK, new BlockBox(-31, -181, -31, 31, 181, 31),
                ModWoodList.DARKWOOD, new BlockBox(-61, -21, -61, 61, 21, 61),
                ModWoodList.GIANTPINKTREE, new BlockBox(-49, -193, -49, 49, 193, 49));
        expected.forEach((wood, box) -> {
            var actual = wood.getTypicalMaximumSize();
            assertSame(wood.bounds, actual);
            assertEquals(box.minX, actual.minX); assertEquals(box.minY, actual.minY); assertEquals(box.minZ, actual.minZ);
            assertEquals(box.maxX, actual.maxX); assertEquals(box.maxY, actual.maxY); assertEquals(box.maxZ, actual.maxZ);
        });
    }

    @Test void existingTreeProvidersInheritTheOriginalOrdinaryLimit() {
        TreeType tree = new TreeType() {
            @Override public BlockKey getItem() { return new BlockKey(Blocks.OAK_LOG); }
            @Override public Block getLogID() { return Blocks.OAK_LOG; }
            @Override public Block getLeafID() { return Blocks.OAK_LEAVES; }
            @Override public Block getSaplingID() { return Blocks.OAK_SAPLING; }
            @Override public Block getPlankID() { return Blocks.OAK_PLANKS; }
            @Override public boolean canBePlacedSideways() { return true; }
            @Override public boolean exists() { return true; }
            @Override public BlockKey getBasicLeaf() { return new BlockKey(Blocks.OAK_LEAVES); }
            @Override public BlockBox getTypicalMaximumSize() { return BlockBox.origin().expand(8, 24, 8); }
        };
        assertEquals(30, tree.getRecursiveBreakDepth());
    }
}
