package reika.dragonapi.instantiable.data;

import org.junit.jupiter.api.Test;
import net.minecraft.util.RandomSource;
import reika.dragonapi.exception.MisuseException;
import static org.junit.jupiter.api.Assertions.*;

class InvertedWeightedRandomTest {
    @Test void emptyAndZeroWeightsCannotOverwriteAPositiveInterval() {
        var random=new WeightedRandom.InvertedWeightedRandom<String>();
        assertNull(random.getRandomEntry());random.addEntry(0,"zero");assertTrue(random.isEmpty());
        random.addEntry(1,"positive");random.addEntry(0,"never");
        assertEquals(1,random.size());assertEquals(1,random.getTotalWeight());
        random.setRNG(boundary(0));assertEquals("positive",random.getRandomEntry());
    }
    @Test void invalidAndUnrepresentableWeightsFailBeforeChangingState() {
        var random=new WeightedRandom.InvertedWeightedRandom<String>();random.addEntry(1,"valid");
        for(double weight:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.MIN_VALUE}) {
            assertThrows(MisuseException.class,() -> random.addEntry(weight,"bad"));
            assertEquals(1,random.size());assertEquals(1,random.getTotalWeight());
        }
        random.clear();random.addEntry(Double.MAX_VALUE,"max");
        assertThrows(MisuseException.class,() -> random.addEntry(Double.MAX_VALUE,"overflow"));
        assertEquals(1,random.size());assertEquals(Double.MAX_VALUE,random.getTotalWeight());
        assertThrows(NullPointerException.class,() -> random.setRNG(null));
    }
    @Test void exactThresholdSelectsTheFollowingHalfOpenInterval() {
        var random=new WeightedRandom.InvertedWeightedRandom<String>();
        random.addEntry(1,"first");random.addEntry(1,"second");random.addEntry(2,"third");
        for(var entry:java.util.Map.of(0D,"first",0.25D,"second",0.5D,"third",Math.nextDown(1D),"third").entrySet()) {
            random.setRNG(boundary(entry.getKey()));assertEquals(entry.getValue(),random.getRandomEntry());
        }
    }
    @Test void seededStreamsRepeatAndClearResetsTotalButAllowsDuplicateResults() {
        var first=new WeightedRandom.InvertedWeightedRandom<String>();var second=new WeightedRandom.InvertedWeightedRandom<String>();
        for(var random:java.util.List.of(first,second)) { random.addEntry(1,"same");random.addEntry(2,"same");random.addEntry(4,"other");random.setSeed(42); }
        assertEquals(3,first.size());
        for(int i=0;i<100;i++) assertEquals(first.getRandomEntry(),second.getRandomEntry());
        first.clear();assertEquals(0,first.getTotalWeight());assertNull(first.getRandomEntry());
        first.addEntry(2,"fresh");assertEquals("fresh",first.getRandomEntry());
    }
    private static RandomSource boundary(double value) {
        return new RandomSource() {
            public RandomSource fork(){return this;}
            public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional(){throw new UnsupportedOperationException();}
            public void setSeed(long seed){}public int nextInt(){return 0;}public int nextInt(int bound){return 0;}
            public long nextLong(){return 0;}public boolean nextBoolean(){return false;}
            public float nextFloat(){return (float)value;}public double nextDouble(){return value;}public double nextGaussian(){return 0;}
        };
    }
}
