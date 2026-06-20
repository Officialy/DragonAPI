package reika.dragonapi.instantiable.data;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Proportionality<F> extends CircularDivisionRenderer<F> {

    private final Map<F, Double> data;
    private double totalValue = 0;

    public boolean drawSeparationLines = false;

    public Proportionality() {
        this(null);
    }

    public Proportionality(MultiMap.MapDeterminator<F, Double> md) {
        data = md != null ? md.getMapType() : new HashMap();
    }

    public void addValue(F o, double amt) {
        Double get = data.get(o);
        double val = get != null ? get.doubleValue() : 0;
        data.put(o, val+amt);
        this.totalValue += amt;
        if (o instanceof ColorCallback)
            this.addColorRenderer(o, (ColorCallback)o);
        this.resetColors();
    }

    public double removeValue(F o) {
        Double get = data.remove(o);
        if (get != null) {
            this.totalValue -= get;
        }
        this.resetColors();
        return get != null ? get.doubleValue() : 0;
    }

    public void removeValue(F o, double amt) {
        Double get = data.get(o);
        double val = get != null ? get.doubleValue() : 0;
        double res = val-amt;
        this.totalValue -= Math.min(amt, val);
        if (res > 0)
            data.put(o, res);
        else
            data.remove(o);
        this.resetColors();
    }

    public double getValue(F o) {
        Double get = data.get(o);
        return get != null ? get.doubleValue() : 0;
    }

    public double getFraction(F o) {
        return this.isEmpty() ? 0 : this.getValue(o)/this.totalValue;
    }

    public boolean isEmpty() {
        return this.totalValue == 0 || this.data.isEmpty();
    }

    @Override
    public Collection<F> getElements() {
        return Collections.unmodifiableCollection(this.data.keySet());
    }

    public boolean hasMajority(F o) {
        return this.getFraction(o) >= 0.5;
    }

    public F getLargestCategory() {
        double max = -1;
        F big = null;
        for (F o : data.keySet()) {
            double has = this.getValue(o);
            if (has > max) {
                has = max;
                big = o;
            }
        }
        return big;
    }

    @Override
    public void clear() {
        data.clear();
        this.totalValue = 0;
        this.resetColors();
    }

    @Override
    public F getClickedSection(int x, int y) {
        double d = ReikaMathLibrary.py3d(x-centerX, y-centerY, 0);
        if (d > renderRadius)
            return null;
        double relAng = (Math.toDegrees(Math.atan2(y-centerY, x-centerX))+360)%360-renderOrigin;
        relAng = ((relAng%360)+360)%360;
        double ang = 0;
        for (F o : data.keySet()) {
            double angw = 360D*this.getFraction(o);
            if (ang <= relAng && ang+angw >= relAng) {
                return o;
            }
            ang += angw;
        }
        return null;
    }

    @Override
    public void render(SubmitNodeCollector collector, PoseStack stack, Map<F, Integer> colorMap) {
        double ang = renderOrigin;
        for (F o : data.keySet()) {
            double angw = 360D * this.getFraction(o);
            final int c = this.getColorForElement(o, colorMap);
            final double a1 = ang;
            final double a2 = ang + angw;
            collector.submitCustomGeometry(stack, RenderTypes.debugQuads(), (pose, buffer) -> this.renderSection(buffer, pose, c, a1, a2));
            ang += angw;
        }

        if (drawSeparationLines && data.size() > 1) {
            collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, buffer) -> {
                double a = renderOrigin;
                for (F o : data.keySet()) {
                    double angw = 360D * this.getFraction(o);
                    double d2 = Math.toRadians(a);
                    if (innerRadius == 0) {
                        buffer.addVertex(pose, (float) centerX, (float) centerY, 0).setColor(0xff000000);
                        double r2 = this.getOuterRadiusAt(d2);
                        buffer.addVertex(pose, (float) (centerX + r2 * Math.cos(d2)), (float) (centerY + r2 * Math.sin(d2)), 0).setColor(0xff000000);
                    } else {
                        double r1 = this.getInnerRadiusAt(d2);
                        double r2 = this.getOuterRadiusAt(d2);
                        buffer.addVertex(pose, (float) (centerX + r1 * Math.cos(d2)), (float) (centerY + r1 * Math.sin(d2)), 0).setColor(0xff000000);
                        buffer.addVertex(pose, (float) (centerX + r2 * Math.cos(d2)), (float) (centerY + r2 * Math.sin(d2)), 0).setColor(0xff000000);
                    }
                    a += angw;
                }
            });
        }
    }

    public void save(CompoundTag NBT, ReikaNBTHelper.NBTIO<F> converter) {
        NBT.putDouble("total", totalValue);
        NBT.putBoolean("lines", drawSeparationLines);
        ListTag li = new ListTag();
        for (Map.Entry<F, Double> e : data.entrySet()) {
            CompoundTag tag = new CompoundTag();
            Tag key = converter.convertToNBT(e.getKey());
            tag.putDouble("value", e.getValue());
            tag.put("key", key);
            li.add(tag);
        }
        NBT.put("data", li);
    }

    public void load(CompoundTag NBT, ReikaNBTHelper.NBTIO<F> converter) {
        totalValue = reika.dragonapi.libraries.io.NBTCompat.getDouble(NBT, "total", 0);
        this.drawSeparationLines = reika.dragonapi.libraries.io.NBTCompat.getBoolean(NBT, "lines", false);
        data.clear();
        ListTag li = NBT.getListOrEmpty("data");
        for (Object o : li) {
            CompoundTag tag = (CompoundTag)o;
            F obj = converter.createFromNBT(tag.get("key"));
            double val = reika.dragonapi.libraries.io.NBTCompat.getDouble(tag, "value", 0);
            data.put(obj, val);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (F f : this.data.keySet()) {
            sb.append(f.toString());
            sb.append(": ");
            sb.append(this.getFraction(f) * 100);
            sb.append("%; ");
        }
        return sb.toString();
    }

    public String mapString() {
        return this.data.toString();
    }

    public Proportionality<F> copy() {
        Proportionality ret = new Proportionality();
        ret.data.putAll(this.data);
        ret.totalValue = this.totalValue;
        return ret;
    }

}
