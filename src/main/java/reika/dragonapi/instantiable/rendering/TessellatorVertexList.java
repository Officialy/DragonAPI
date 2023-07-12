package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class TessellatorVertexList {

    private ArrayList<TessellatorVertex> data = new ArrayList<>();
    private final LinkedList<ArrayList<TessellatorVertex>> list = new LinkedList<>();

    public final double originX;
    public final double originY;
    public final double originZ;

    public TessellatorVertexList() {
        this(0, 0, 0);
    }

    public TessellatorVertexList(double x, double y, double z) {
        originX = x;
        originY = y;
        originZ = z;
    }

    public void vertex(double x, double y, double z) {
        data.add(new TessellatorVertex(x, y, z));
    }

    public void addVertexWithUV(double x, double y, double z, float u, float v) {
        data.add(new TessellatorVertex(x, y, z, u, v));
    }

    public void addVertexWithUVColor(double x, double y, double z, float u, float v, int color) {
        data.add(new TessellatorVertex(x, y, z, u, v, color));
    }

    public void render() {
        for (TessellatorVertex v : data) {
            v.addToTessellator();
        }
    }

    public void reverse() {
        Collections.reverse(data);
    }

    public static class TessellatorVertex {
        private double posX;
        private double posY;
        private double posZ;

        private final float posU;
        private final float posV;

        private final int colorData;

        private boolean hasUV;
        private boolean hasColor;

        public TessellatorVertex(double x, double y, double z) {
            this(x, y, z, 0, 0);
            hasUV = false;
        }

        public TessellatorVertex(double x, double y, double z, float u, float v) {
            this(x, y, z, u, v, 0);
            hasColor = false;
        }

        public TessellatorVertex(double x, double y, double z, float u, float v, int color) {
            posX = x;
            posY = y;
            posZ = z;
            posU = u;
            posV = v;
            colorData = color;
            hasUV = true;
            hasColor = true;
        }

        public void addToTessellator() {
            if (hasColor) {
                Tesselator.getInstance().getBuilder().color(colorData);
            }
            if (hasUV)
                Tesselator.getInstance().getBuilder().vertex(posX, posY, posZ).uv(posU, posV);
            else
                Tesselator.getInstance().getBuilder().vertex(posX, posY, posZ);
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof TessellatorVertex v) {
                if (v.posX == posX && v.posY == posY && v.posZ == posZ) {
                    return hasUV ? (v.hasUV && v.posU == posU && v.posV == posV) : !v.hasUV;
                }
            }
            return false;
        }

        public double x() {
            return posX;
        }

        public double y() {
            return posY;
        }

        public double z() {
            return posZ;
        }

        public TessellatorVertex copy() {
            if (hasColor)
                return new TessellatorVertex(posX, posY, posZ, posU, posV, colorData);
            return hasUV ? new TessellatorVertex(posX, posY, posZ, posU, posV) : new TessellatorVertex(posX, posY, posZ);
        }

        public void offset(double dx, double dy, double dz) {
            posX += dx;
            posY += dy;
            posZ += dz;
        }

        public void rotate(double rx, double ry, double rz, double ox, double oy, double oz) {
            Vec3 ret = ReikaVectorHelper.rotateVector(new Vec3(posX-ox, posY-oy, posZ-oz), rx, ry, rz);
            posX = ox+ret.x;
            posY = oy+ret.y;
            posZ = oz+ret.z;
        }
    }

    public void offset(double dx, double dy, double dz) {
        for (TessellatorVertex v : data) {
            v.offset(dx, dy, dz);
        }
    }

    public void scale(double dx, double dy, double dz) {
        for (TessellatorVertex v : data) {
            v.posX *= dx;
            v.posY *= dy;
            v.posZ *= dz;
        }
    }

    public void clamp(double minx, double miny, double minz, double maxx, double maxy, double maxz) {
        for (TessellatorVertex v : data) {
            v.posX = Mth.clamp(v.posX, minx, maxx);
            v.posY = Mth.clamp(v.posY, miny, maxy);
            v.posZ = Mth.clamp(v.posZ, minz, maxz);
        }
    }

    public void invertX() {
        for (TessellatorVertex v : data) {
            v.posX = 1-v.posX;
        }
        this.reverse();
    }

    public void invertY() {
        for (TessellatorVertex v : data) {
            v.posY = 1-v.posY;
        }
        this.reverse();
    }

    public void invertZ() {
        for (TessellatorVertex v : data) {
            v.posZ = 1-v.posZ;
        }
        this.reverse();
    }

    /** CW about +X */
    public void rotateYtoZ() {
        for (TessellatorVertex v : data) {
            double z = v.posZ-originZ;
            v.posZ = originZ+v.posY-originY;
            v.posY = originY+z;
        }
        this.reverse();
    }

    /** CW about +Y */
    public void rotateXtoZ() {
        for (TessellatorVertex v : data) {
            double z = v.posZ-originZ;
            v.posZ = originZ+v.posX-originX;
            v.posX = originX+z;
        }
        this.reverse();
    }

    /** CW about +Z */
    public void rotateYtoX() {
        for (TessellatorVertex v : data) {
            double x = v.posX-originX;
            v.posX = originX+v.posY-originY;
            v.posY = originY+x;
        }
        this.reverse();
    }

    public void rotateNonOrthogonal(double rx, double ry, double rz) {
        this.rotateNonOrthogonal(rx, ry, rz, originX, originY, originZ);
    }

    public void rotateNonOrthogonal(double rx, double ry, double rz, double ox, double oy, double oz) {
        for (TessellatorVertex v : data) {
            v.rotate(rx, ry, rz, ox, oy, oz);
        }
    }

    public void center() {
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (TessellatorVertex v : data) {
            cx += v.posX;
            cy += v.posY;
            cz += v.posZ;
        }
        cx /= data.size();
        cy /= data.size();
        cz /= data.size();
        this.offset(-cx, -cy, -cz);
    }

    public void clear() {
        data.clear();
    }

    public void push() {
        list.addLast(data);
        data = new ArrayList<>();
    }

    public void pop() {
        if (list.isEmpty())
            throw new IllegalStateException("Popped an empty list!");
        data = list.pollLast();
    }

    public void addAll(TessellatorVertexList li) {
        list.addAll(li.list);
        data.addAll(li.data);
    }
}
