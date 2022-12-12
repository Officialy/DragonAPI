/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.math;

import net.minecraft.nbt.Tag;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;

public class MovingAverage {

    private final double[] data;

    public MovingAverage(int dataPoints) {
        data = new double[dataPoints];
    }

    public static MovingAverage load(CompoundTag tag) {
        int size = tag.getInt("size");
        MovingAverage mv = new MovingAverage(size);
        ListTag li = tag.getList("data", Tag.TAG_DOUBLE);
        for (int i = 0; i < li.size(); i++) {
            mv.data[i] = (li.getList(i).getDouble(i));
        }
        return mv;
    }

    public MovingAverage addValue(double val) {
        ReikaArrayHelper.cycleArray(data, val);
        return this;
    }

    public double getAverage() {
        double avg = 0;
        for (int i = 0; i < data.length; i++) {
            avg += data[i];
        }
        return avg / data.length;
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putInt("size", data.length);
        ListTag li = new ListTag();

        for (double d : data) {
            li.add(DoubleTag.valueOf(d));
        }

        tag.put("data", li);
    }

}
