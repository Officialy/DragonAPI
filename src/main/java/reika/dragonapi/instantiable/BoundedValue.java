/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.math.BigDecimal;


public class BoundedValue<N extends Number> {

    private final Class<N> typeClass;

    private final boolean isDecimalType;
    private final double minValue;
    private final double maxValue;
    private double value;
    private double step;

    public BoundedValue(N min, N max) {
        this(min, max, (min.doubleValue() + max.doubleValue()) / 2D);
    }

    public BoundedValue(N min, N max, N init) {
        this(min, max, init.doubleValue());
    }

    private BoundedValue(N min, N max, double init) {
        this.typeClass = (Class<N>) min.getClass();
        this.minValue = min.doubleValue();
        this.maxValue = max.doubleValue();
        this.value = Mth.clamp(init, minValue, maxValue);
        this.isDecimalType = (min instanceof Double || min instanceof Float || min instanceof BigDecimal);
    }

    private BoundedValue(double min, double max, double init, double step, boolean dec, Class type) {
        this.minValue = min;
        this.maxValue = max;
        this.value = init;
        this.step = step;
        this.isDecimalType = dec;
        this.typeClass = type;
    }

    public static BoundedValue load(CompoundTag tag) {
        try {
            return new BoundedValue(tag.getDouble("min"), tag.getDouble("max"), tag.getDouble("val"), tag.getDouble("step"), tag.getBoolean("decimal"), Class.forName(tag.getString("type")));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean increase() {
        if (this.value + step <= this.maxValue) {
            this.value += step;
            return true;
        }
        return false;
    }

    public boolean decrease() {
        if (this.value - step >= this.minValue) {
            this.value -= step;
            return true;
        }
        return false;
    }

    public float getFraction() {
        return (float) ((this.value - this.minValue) / (this.maxValue - this.minValue));
    }

    public void setFraction(double f) {
        value = this.minValue + f * (this.maxValue - this.minValue);
    }

    public double getValue() {
        return value;
    }

    public void setValue(N val) {
        this.value = Mth.clamp(val.doubleValue(), minValue, maxValue);
    }

    public double getStep() {
        return step;
    }

    public BoundedValue setStep(N step) {
        this.step = this.isDecimalType ? step.doubleValue() : 1;
        return this;
    }

    public double getMinValue() {
        return this.minValue;
    }

    public double getMaxValue() {
        return this.maxValue;
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putString("type", this.typeClass.getName());
        tag.putBoolean("decimal", isDecimalType);
        tag.putDouble("val", value);
        tag.putDouble("min", minValue);
        tag.putDouble("max", maxValue);
        tag.putDouble("step", step);
    }

    @Override
    public String toString() {
        return "[" + this.minValue + " > " + this.maxValue + "] @ " + this.value + "x" + this.step + ", " + this.isDecimalType;
    }

}
