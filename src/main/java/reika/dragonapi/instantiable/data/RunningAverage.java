/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data;


import net.minecraft.nbt.CompoundTag;

public class RunningAverage {

    private int numberDataPoints;
    private double currentAverage;

    public void addValue(double val) {
        currentAverage = numberDataPoints == 0 ? val : (currentAverage * numberDataPoints + val) / (1 + numberDataPoints);
        numberDataPoints++;
    }

    public double getAverage() {
        return currentAverage;
    }

    public void load(String key, CompoundTag nbt) {
        CompoundTag tag = nbt.getCompound(key);
        numberDataPoints = tag.getInt("npoints");
        currentAverage = tag.getDouble("avg");
    }

    public void saveAdditional(String key, CompoundTag nbt) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("npoints", numberDataPoints);
        tag.putDouble("avg", currentAverage);
        nbt.put(key, tag);
    }

}
