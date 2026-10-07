package reika.dragonapi.instantiable.data;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.maps.ValueSortedMap;
import reika.dragonapi.interfaces.ObjectToNBTSerializer;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedAuditTest {
    private static final ObjectToNBTSerializer<String> STRINGS = new ObjectToNBTSerializer<>() {
        public CompoundTag save(String value) { var tag = new CompoundTag(); tag.putString("value", value); return tag; }
        public String construct(CompoundTag tag) { return tag.getStringOr("value", ""); }
    };
    @Test void weightsSurviveSavingAndDoNotTrustRedundantTotals() {
        var weights = new WeightedRandom<String>(); weights.addEntry("first", 3); weights.addEntry("second", 7);
        var tag = new CompoundTag(); weights.saveAdditional("weights", tag, STRINGS);
        assertTrue(tag.contains("weights")); tag.getCompoundOrEmpty("weights").putDouble("total", 999);
        var restored = new WeightedRandom<String>(); restored.load("weights", tag, STRINGS);
        assertEquals(Set.of("first", "second"), restored.getValues());
        assertEquals(10, restored.getTotalWeight()); assertEquals(7, restored.getMaxWeight());
        restored.setSeed(12); for (int i = 0; i < 30; i++) assertNotNull(restored.getRandomEntry());
    }
    @Test void dynamicWeightsFollowTheirCurrentValuesAndClearResetsMode() {
        class Dynamic implements WeightedRandom.DynamicWeight { double weight = 2; public double getWeight() { return weight; } }
        var value = new Dynamic(); var weights = new WeightedRandom<Object>(); weights.addDynamicEntry(value); weights.addEntry("fixed",3);
        assertEquals(5,weights.getTotalWeight()); value.weight = 8; assertEquals(11,weights.getTotalWeight()); assertEquals(8,weights.getMaxWeight());
        weights.clear(); weights.addEntry("new",4); assertEquals(4,weights.getTotalWeight()); assertEquals(4,weights.getMaxWeight());
    }
    @Test void replacingAndRemovingWeightsRebuildsSelectionState() {
        var weights = new WeightedRandom<String>(); weights.addEntry("first", 10); weights.addEntry("first", 2); weights.addEntry("second", 3);
        assertEquals(5, weights.getTotalWeight()); assertEquals(3, weights.getMaxWeight());
        assertEquals(3, weights.remove("second")); assertEquals(0, weights.remove("missing")); assertEquals(2, weights.getMaxWeight());
        weights.setHistorical(); weights.clear(); weights.addEntry("replacement", 1);
        assertEquals("replacement", weights.getRandomEntry());
    }
    @Test void zeroWeightsAreNeverChosenEvenAtTheRngLowerBoundary() {
        var weights = new WeightedRandom<String>(); weights.addEntry("zero", 0);
        weights.setRNG(new RandomSource() {
            public RandomSource fork() { return this; }
            public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional() { throw new UnsupportedOperationException(); }
            public void setSeed(long seed) {} public int nextInt() { return 0; } public int nextInt(int bound) { return 0; }
            public long nextLong() { return 0; } public boolean nextBoolean() { return false; }
            public float nextFloat() { return 0; } public double nextDouble() { return 0; } public double nextGaussian() { return 0; }
        });
        assertNull(weights.getRandomEntry()); weights.addEntry("positive", 1); assertEquals("positive", weights.getRandomEntry());
    }
    @Test void valueSortingRetainsTiesAndPassesValuesToCustomComparators() {
        var map = new ValueSortedMap<String, Integer>(); map.put("first", 7); map.put("second", 7); map.put("third", 3);
        assertEquals(List.of("third", "first", "second"), new ArrayList<>(map.keySet()));
        map.setComparator(Comparator.reverseOrder()); assertEquals(List.of("first", "second", "third"), new ArrayList<>(map.keySet()));
        map.remove("first"); map.put("second", 1); assertEquals(List.of("third", "second"), new ArrayList<>(map.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(99));
        assertEquals(3, map.get("third"));
    }
    @Test void positionOrderingPreservesBothHashCollisionsAndSeededHashValues() {
        var first = new Coordinate(0, 1, 0); var second = new Coordinate(0, 0, 31);
        assertEquals(29822, first.hashCode()); assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(0, first.compareTo(second)); assertEquals(2, new TreeSet<>(List.of(first, second)).size());
    }
    @Test void decimalMathRetainsFractionalNegativeCoordinates() {
        var position = new DecimalPosition(-0.25, 3.5, -8.75);
        assertEquals(new BlockPos(-1, 3, -9), position.getCoordinate());
        assertEquals(new DecimalPosition(0.25, -3.5, 8.75), position.negate());
        var box = position.getAABB(2); assertEquals(-2.25, box.minX); assertEquals(5.5, box.maxY); assertEquals(-6.75, box.maxZ);
    }
    @Test void arrayIteratorVisitsEveryPositionAndRemovesTheReturnedPosition() {
        var empty = new BlockArray(); assertFalse(empty.iterator().hasNext()); assertThrows(NoSuchElementException.class, () -> empty.iterator().next());
        var array = new BlockArray(List.of(BlockPos.ZERO, new BlockPos(2, 0, 0), new BlockPos(1, 0, 0)));
        var iterator = array.iterator(); assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(BlockPos.ZERO, iterator.next()); iterator.remove(); assertFalse(array.hasBlock(BlockPos.ZERO));
        assertThrows(IllegalStateException.class, iterator::remove); assertEquals(new BlockPos(2, 0, 0), iterator.next());
        assertEquals(new BlockPos(1, 0, 0), iterator.next()); assertFalse(iterator.hasNext()); assertThrows(NoSuchElementException.class, iterator::next);
        array.sortBlocksByDistance(BlockPos.ZERO); assertEquals(new BlockPos(2, 0, 0), array.getNthBlock(0));
        assertEquals(BlockPos.ZERO, new BlockArray(List.of(BlockPos.ZERO)).iterator().next());
    }
}
