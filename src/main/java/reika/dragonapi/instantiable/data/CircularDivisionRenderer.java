package reika.dragonapi.instantiable.data;

import com.mojang.blaze3d.vertex.BufferBuilder;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

public abstract class CircularDivisionRenderer<F> {

    private static final ArrayList<Integer> defaultColors = new ArrayList<>();

    static {
        defaultColors.add(0xff0000);
        defaultColors.add(0x00ff00);
        defaultColors.add(0x0000ff);
        defaultColors.add(0xffff00);
        defaultColors.add(0xff00ff);
        defaultColors.add(0x00ffff);
        defaultColors.add(0xa0a0a0);
    }

    protected final Random rand = new Random();
    private final HashMap<F, ColorCallback> renderColors = new HashMap();
    private final HashMap<F, Integer> entryColors = new HashMap();
    public boolean squareRender = false;
    protected double centerX;
    protected double centerY;
    protected double renderRadius;
    protected double renderOrigin;
    protected double innerRadius;
    private int currentDefaultColorIndex = 0;

    public abstract Collection<F> getElements();

    public abstract void clear();

    public abstract F getClickedSection(int x, int y);

    public final void addColorRenderer(F type, ColorCallback call) {
        this.renderColors.put(type, call);
    }

    public final void setGeometry(double x, double y, double r, double zeroAng) {
        this.setGeometry(x, y, r, 0, zeroAng);
    }

    public final void setGeometry(double x, double y, double r, double ir, double zeroAng) {
        centerX = x;
        centerY = y;
        renderRadius = r;
        innerRadius = ir;
        renderOrigin = zeroAng;
    }


    public final void render() {
        this.render(null);
    }


    public abstract void render(Map<F, Integer> colorMap);

    public final void resetColors() {
        this.entryColors.clear();
    }

    protected final int getColorForElement(F o, Map<F, Integer> colorMap) {
        if (this.entryColors.isEmpty()) {
            this.calculateEntryColors(colorMap);
        }
        return this.entryColors.get(o);
    }

    private final void calculateEntryColors(Map<F, Integer> colorMap) {
        if (this.entryColors.isEmpty()) {
            currentDefaultColorIndex = 0;
            for (F o : this.getElements()) {
                entryColors.put(o, this.calcColorForElement(o, colorMap));
            }
        }
    }

    private final int calcColorForElement(F o, Map<F, Integer> colorMap) {
        int c = 0;
        if (colorMap != null && colorMap.containsKey(o)) {
            c = colorMap.get(o);
        } else {
            ColorCallback call = renderColors.get(o);
            if (call != null) {
                c = call.getColor(o);
            } else {
                if (currentDefaultColorIndex >= defaultColors.size()) {
                    int newcolor = ReikaColorAPI.RGBtoHex(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
                    defaultColors.add(newcolor);
                    c = newcolor;
                } else {
                    c = defaultColors.get(currentDefaultColorIndex);
                }
                currentDefaultColorIndex++;
            }
        }
        return c;
    }

    protected final void renderSection(BufferBuilder renderer, double ang1, double ang2) {
        if (innerRadius == 0) {
            renderer.vertex(centerX, centerY, 0);
            for (double d = ang1; d <= ang2; d += 0.25) {
                double d2 = Math.toRadians(d);
                double r2 = this.getOuterRadiusAt(d2);
                double dx = centerX + r2 * Math.cos(d2);
                double dy = centerY + r2 * Math.sin(d2);
                renderer.vertex(dx, dy, 0);
            }
            double d2 = Math.toRadians(ang2);
            double r2 = this.getOuterRadiusAt(d2);
            double dx = centerX + r2 * Math.cos(d2);
            double dy = centerY + r2 * Math.sin(d2);
            renderer.vertex(dx, dy, 0);
        } else {
            for (double d = ang1; d <= ang2; d += 0.25) {
                double d2 = Math.toRadians(d);
                double r1 = this.getInnerRadiusAt(d2);
                double r2 = this.getOuterRadiusAt(d2);
                double dx1 = centerX + r1 * Math.cos(d2);
                double dy1 = centerY + r1 * Math.sin(d2);
                double dx2 = centerX + r2 * Math.cos(d2);
                double dy2 = centerY + r2 * Math.sin(d2);
                renderer.vertex(dx1, dy1, 0);
                renderer.vertex(dx2, dy2, 0);
            }
        }
    }

    public final double getInnerRadiusAt(double ang) {
        if (squareRender) {
            return this.innerRadius * Math.min(Math.abs(1D / Math.cos(ang)), Math.abs(1D / Math.sin(ang)));
        } else {
            return this.innerRadius;
        }
    }

    public final double getOuterRadiusAt(double ang) {
        if (squareRender) {
            return this.renderRadius * Math.min(Math.abs(1D / Math.cos(ang)), Math.abs(1D / Math.sin(ang)));
        } else {
            return this.renderRadius;
        }
    }

    public interface ColorCallback {

        int getColor(Object key);

    }

    public static class IntColorCallback implements ColorCallback {

        public final int color;

        public IntColorCallback(int c) {
            color = c;
        }

        public int getColor(Object key) {
            return color;
        }

    }

}
