package reika.dragonapi.instantiable.data;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

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


    public final void render(SubmitNodeCollector collector, PoseStack stack) {
        this.render(collector, stack, null);
    }


    public abstract void render(SubmitNodeCollector collector, PoseStack stack, Map<F, Integer> colorMap);

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

    // 26.2: emitted as QUADS (RenderTypes.debugQuads) since TRIANGLE_FAN/STRIP immediate-mode is gone.
    // Re-triangulate per 0.25° arc step into a degenerate quad; colour is set per-vertex.
    protected final void renderSection(VertexConsumer renderer, PoseStack.Pose pose, int color, double ang1, double ang2) {
        if (innerRadius == 0) {
            double prev = ang1;
            for (double d = ang1 + 0.25; d <= ang2 + 1e-9; d += 0.25) {
                double pa = Math.toRadians(prev);
                double da = Math.toRadians(d);
                double rp = this.getOuterRadiusAt(pa);
                double rd = this.getOuterRadiusAt(da);
                renderer.addVertex(pose, (float) centerX, (float) centerY, 0).setColor(color);
                renderer.addVertex(pose, (float) (centerX + rp * Math.cos(pa)), (float) (centerY + rp * Math.sin(pa)), 0).setColor(color);
                renderer.addVertex(pose, (float) (centerX + rd * Math.cos(da)), (float) (centerY + rd * Math.sin(da)), 0).setColor(color);
                renderer.addVertex(pose, (float) centerX, (float) centerY, 0).setColor(color);
                prev = d;
            }
        } else {
            double prev = ang1;
            for (double d = ang1 + 0.25; d <= ang2 + 1e-9; d += 0.25) {
                double pa = Math.toRadians(prev);
                double da = Math.toRadians(d);
                double ip = this.getInnerRadiusAt(pa);
                double op = this.getOuterRadiusAt(pa);
                double id = this.getInnerRadiusAt(da);
                double od = this.getOuterRadiusAt(da);
                renderer.addVertex(pose, (float) (centerX + ip * Math.cos(pa)), (float) (centerY + ip * Math.sin(pa)), 0).setColor(color);
                renderer.addVertex(pose, (float) (centerX + op * Math.cos(pa)), (float) (centerY + op * Math.sin(pa)), 0).setColor(color);
                renderer.addVertex(pose, (float) (centerX + od * Math.cos(da)), (float) (centerY + od * Math.sin(da)), 0).setColor(color);
                renderer.addVertex(pose, (float) (centerX + id * Math.cos(da)), (float) (centerY + id * Math.sin(da)), 0).setColor(color);
                prev = d;
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
