/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.instantiable.data.maps.CountMap;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import net.minecraft.nbt.CompoundTag;

import java.util.Collection;
import java.util.HashSet;


public class StatisticalRandom<K> {

    private final CountMap<K> data = new CountMap();

    private final HashSet<K> options = new HashSet();

    private ReikaNBTHelper.NBTIO<K> converter;

    public StatisticalRandom() {

    }

    public StatisticalRandom(Collection<K> set) {
        options.addAll(set);
    }

    public StatisticalRandom(K... set) {
        this(ReikaJavaLibrary.makeListFromArray(set));
    }

    public StatisticalRandom(Class<? extends K> set) {
        if (!set.isEnum())
            throw new MisuseException("You can only specify enum types via a class reference!");
        K[] data = set.getEnumConstants();
        options.addAll(ReikaJavaLibrary.makeListFromArray(data));
        this.setNBTConverter((ReikaNBTHelper.NBTIO<K>) new ReikaNBTHelper.EnumNBTConverter((Class<? extends Enum>) set));
    }

    public void setNBTConverter(ReikaNBTHelper.NBTIO<K> c) {
        this.converter = c;
    }

    public K roll() {
        return this.roll(null);
    }

    public K roll(WeightedRandom<K> base) {
        K result = this.genRandom(base).getRandomEntry();
        data.increment(result);
        return result;
    }

    private WeightedRandom<K> genRandom(WeightedRandom<K> base) {
        WeightedRandom<K> w = new WeightedRandom();
        for (K k : options) {
            double wt = this.getWeightOf(base, k);
            if (wt > 0) { //very early on, ones already obtained have negative weights, so are out of selection, to ensure some of all
                w.addEntry(k, wt);
            }
        }
        return w;
    }

    private double getWeightOf(WeightedRandom<K> src, K k) {
        double base = 1D / options.size();
        double frac = data.getFraction(k);
        if (src != null) {
            base *= src.getWeight(k) / src.getTotalWeight();
        }
        double chance = base - (frac - base);
        return chance;
    }

    public void load(CompoundTag tag) {
        data.clear();
        HashSet<K> set = new HashSet();
        data.load(tag.getCompound("data"), converter);
        ReikaNBTHelper.readCollectionFromNBT(set, tag, "set", converter);
        options.addAll(set);
    }

    public void saveAdditional(CompoundTag tag) {
        CompoundTag nbt = new CompoundTag();
        data.saveAdditional(nbt, converter);
        tag.put("data", nbt);
        ReikaNBTHelper.writeCollectionToNBT(options, tag, "set", converter);
    }

    @Override
    public String toString() {
        return data + " > " + this.genRandom(null);
    }

}
