package reika.dragonapi.instantiable.data;

import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.Interpolation;
import reika.dragonapi.instantiable.StatisticalRandom;
import reika.dragonapi.interfaces.ObjectToNBTSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;

public class WeightedRandom<V> {

    private RandomSource rand = RandomSource.create();
    private StatisticalRandom<V> weighted;

    private final HashMap<V, Double> data = new HashMap<>();
    private double maxWeight = 0;
    private double weightSum;
    private boolean isDynamic = false;

    public double addEntry(V obj, double weight) {
        if (weight < 0)
            throw new MisuseException("You cannot have an entry with a negative weight!");
        data.put(obj, weight);
        this.weightSum += weight;
        this.maxWeight = Math.max(this.maxWeight, weight);
        this.isDynamic |= obj instanceof DynamicWeight;
        return this.weightSum;
    }

    public double addDynamicEntry(DynamicWeight wt) {
        return this.addEntry((V) wt, wt.getWeight());
    }

    public double remove(V val) {
        double ret = data.remove(val);
        this.weightSum -= ret;
        return ret;
    }

    public V getRandomEntry() {
        if (this.weighted != null) {
            return this.weighted.roll(this);
        }
        double d = rand.nextDouble() * this.getTotalWeight();
        double p = 0;
        for (V obj : data.keySet()) {
            p += this.getWeight(obj);
            if (d <= p) {
                return obj;
            }
        }
        return null;
    }

    public V getRandomEntry(V fallback, double wt) {
        double sum = this.getTotalWeight() + wt;
        double d = rand.nextDouble() * sum;
        double p = 0;
        for (V obj : data.keySet()) {
            p += this.getWeight(obj);
            if (d <= p) {
                return obj;
            }
        }
        return fallback;
    }

    public double getWeight(V obj) {
        if (obj instanceof DynamicWeight)
            return ((DynamicWeight) obj).getWeight();
        Double get = data.get(obj);
        return get != null ? get.doubleValue() : 0;
    }

    public double getMaxWeight() {
        if (this.isDynamic) {
            double max = 0;
            for (V obj : this.data.keySet()) {
                double wt = this.getWeight(obj);
                max = Math.max(max, wt);
            }
            return max;
        }
        return this.maxWeight;
    }

    public double getTotalWeight() {
        if (this.isDynamic) {
            double sum = 0;
            for (V obj : this.data.keySet()) {
                double wt = this.getWeight(obj);
                sum += wt;
            }
            return sum;
        }
        return this.weightSum;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }

    public boolean hasEntry(V obj) {
        return data.containsKey(obj);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public void setSeed(long seed) {
        rand.setSeed(seed);
    }

    public void clear() {
        this.data.clear();
        this.maxWeight = 0;
        this.weightSum = 0;
    }

    public static class InvertedWeightedRandom<V> {

        private final Random rand = new Random();

        private final NavigableMap<Double, V> data = new TreeMap<Double, V>();
        private double weightSum;

        public void addEntry(double weight, V result) {
            weightSum += weight;
            data.put(weightSum, result);
        }

        public V getRandomEntry() {
            double value = rand.nextDouble() * this.weightSum;
            //ReikaJavaLibrary.pConsole(value+" of "+this.data.toString());
            return data.ceilingEntry(value).getValue();
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }

        public int size() {
            return data.size();
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    public interface DynamicWeight {

        double getWeight();

    }

    public static WeightedRandom<BlockPos> fromArray(int[][] arr) {
        WeightedRandom<BlockPos> w = new WeightedRandom();
        int dx = arr.length / 2;
        for (int i = 0; i < arr.length; i++) {
            int dz = arr[i].length / 2;
            for (int k = 0; k < arr[i].length; k++) {
                if (arr[i][k] > 0) {
                    BlockPos c = new BlockPos(i - dx, 0, k - dz);
                    w.addEntry(c, arr[i][k]);
                }
            }
        }
        return w;
    }

    public static WeightedRandom<BlockPos> fromArray(double[][] arr) {
        WeightedRandom<BlockPos> w = new WeightedRandom();
        int dx = arr.length / 2;
        for (int i = 0; i < arr.length; i++) {
            int dz = arr[i].length / 2;
            for (int k = 0; k < arr[i].length; k++) {
                if (arr[i][k] > 0) {
                    BlockPos c = new BlockPos(i - dx, 0, k - dz);
                    w.addEntry(c, arr[i][k]);
                }
            }
        }
        return w;
    }

    public static WeightedRandom<Double> fromInterpolation(Interpolation lin, double dstep) {
        double k = lin.getLowestKey();
        WeightedRandom<Double> ret = new WeightedRandom();
        for (double d = k; d <= lin.getHighestKey(); d += dstep) {
            ret.addEntry(d, lin.getValue(d));
        }
        return ret;
    }

    public static WeightedRandom<Integer> fromIntInterpolation(Interpolation lin) {
        double k = lin.getLowestKey();
        WeightedRandom<Integer> ret = new WeightedRandom();
        for (int d = (int) k; d <= lin.getHighestKey(); d++) {
            ret.addEntry(d, lin.getValue(d));
        }
        return ret;
    }

    public Set<V> getValues() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public void setRNG(RandomSource r) {
        rand = r;
    }
    public void saveAdditional(String s, CompoundTag tag, ObjectToNBTSerializer<V> serializer) {
        CompoundTag nbt = new CompoundTag();
        ListTag li = new ListTag();
        for (V key : this.data.keySet()) {
            Double wt = this.data.get(key);
            CompoundTag e = new CompoundTag();
            e.put("key", serializer.save(key));
            e.putDouble("weight", wt);
            li.add(e);
        }
        nbt.put("entries", li);
        nbt.putDouble("total", weightSum);
        nbt.putDouble("max", maxWeight);
        nbt.putBoolean("dynamic", isDynamic);
    }

    public void load(String s, CompoundTag tag, ObjectToNBTSerializer<V> serializer) {
        if (!tag.contains(s))
            return;
        CompoundTag data = tag.getCompound(s);
        this.clear();
        ListTag li = data.getList("entries", Tag.TAG_COMPOUND);
        for (Object o : li) {
            CompoundTag e = (CompoundTag) o;
            V key = serializer.construct(e.getCompound("key"));
            double wt = e.getDouble("weight");
            this.data.put(key, wt);
        }
        this.weightSum = data.getDouble("total");
        this.maxWeight = data.getDouble("max");
        this.isDynamic = data.getBoolean("dynamic");
    }

    public void setHistorical() {
        weighted = new StatisticalRandom(this.data.keySet());
    }

    public double getProbability(V val) {
        return this.getWeight(val) / this.getTotalWeight();
    }

}
