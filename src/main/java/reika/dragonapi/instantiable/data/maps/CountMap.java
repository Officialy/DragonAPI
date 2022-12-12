/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.maps;

import net.minecraft.nbt.Tag;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.libraries.ReikaNBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CountMap<V> {

    private final HashMap<V, Integer> data = new HashMap();
    private int total;

    public CountMap() {

    }

    public void increment(V key) {
        this.increment(key, 1);
    }

    public void increment(V key, int num) {
        Integer get = data.get(key);
        int has = get != null ? get.intValue() : 0;
        int next = has + num;
        this.set(key, next);
        total += num;
    }

    public void increment(CountMap<V> map) {
        for (V val : map.data.keySet()) {
            this.increment(val, map.data.get(val));
        }
    }

    public void subtract(V key, int num) {
        int has = this.get(key);
        if (num >= has)
            this.remove(key);
        else
            this.increment(key, -num);
    }

    public void set(V key, int num) {
        if (num != 0)
            data.put(key, num);
        else
            data.remove(key);
    }

    public int remove(V key) {
        Integer amt = data.remove(key);
        if (amt == null)
            amt = 0;
        this.total -= amt;
        return amt;
    }

    public int get(V key) {
        Integer get = data.get(key);
        return get != null ? get.intValue() : 0;
    }

    public int size() {
        return data.size();
    }

    public int getTotalCount() {
        return total;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountMap && ((CountMap) o).data.equals(data);
    }

    @Override
    public String toString() {
        return this.data.toString();
    }

    public Set<V> keySet() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public boolean containsKey(V key) {
        return data.containsKey(key);
    }

    public void clear() {
        data.clear();
    }

    public WeightedRandom<V> asWeightedRandom() {
        WeightedRandom<V> w = new WeightedRandom();
        for (V key : data.keySet()) {
            w.addEntry(key, this.get(key));
        }
        return w;
    }

    public double getFraction(V k) {
        if (this.total == 0)
            return 0;
        return this.get(k) / (double) this.getTotalCount();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Map<V, Integer> view() {
        return Collections.unmodifiableMap(data);
    }

    public void load(CompoundTag tag, ReikaNBTHelper.NBTIO<V> converter) {
        total = tag.getInt("total");

        data.clear();
        ListTag li = tag.getList("data", Tag.TAG_COMPOUND);
        for (Object o : li) {
            CompoundTag dat = (CompoundTag) o;
            V key = converter.createFromNBT(dat.get("key"));
            int amt = dat.getInt("value");
            data.put(key, amt);
        }
    }

    public void saveAdditional(CompoundTag tag, ReikaNBTHelper.NBTIO<V> converter) {
        tag.putInt("total", total);
        ListTag li = new ListTag();
        for (V k : data.keySet()) {
            CompoundTag dat = new CompoundTag();
            int amt = this.get(k);
            dat.put("key", converter.convertToNBT(k));
            dat.putInt("value", amt);
            li.add(dat);
        }
        tag.put("data", li);
    }

}
