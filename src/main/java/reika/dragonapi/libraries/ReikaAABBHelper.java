package reika.dragonapi.libraries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

public class ReikaAABBHelper {

    /**
     * Renders an AABB bounding box in the world. Very useful for debug purposes, or as a user-friendliness feature.
     * Args: World, AABB, Render par2,4,6, x,y,z of machine, root alpha value (-ve for solid color), RGB, solid outline yes/no
     */
    public static void renderAABB(PoseStack stack, AABB box, double par2, double par4, double par6, int x, int y, int z, int a, int r, int g, int b, boolean line) {
        int[] color = {r, g, b, a};
        ReikaRenderHelper.prepareGeoDraw(true);
        stack.pushPose();
        stack.translate((float) par2, (float) par4 + 2.0F, (float) par6 + 1.0F);
        stack.scale(1.0F, -1.0F, -1.0F);
        stack.translate(0.5F, 0.5F, 0.5F);
        stack.popPose();
//        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.enableBlend();
        if (color[3] > 255 && color[3] > 0)
            color[3] = 255;
        if (color[3] < 0)
            color[3] *= -1;
        boolean filled = true;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder var5 = tess.getBuilder();

        double xdiff = box.minX - x;
        double ydiff = box.minY - y;
        double zdiff = box.minZ - z;
        double xdiff2 = box.maxX - x;
        double ydiff2 = box.maxY - y;
        double zdiff2 = box.maxZ - z;

        double px = par2 + xdiff;
        double py = par4 + ydiff;
        double pz = par6 + zdiff;
        double px2 = par2 + xdiff2;
        double py2 = par4 + ydiff2;
        double pz2 = par6 + zdiff2;
        if (var5.building())
            var5.end();
        if (line) {
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px2, py2, pz);
            var5.vertex(px, py2, pz);
            var5.vertex(px, py2, pz2);
            var5.vertex(px2, py2, pz2);
            var5.end();
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px2, py, pz);
            var5.vertex(px, py, pz);
            var5.vertex(px, py, pz2);
            var5.vertex(px2, py, pz2);
            var5.end();
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px, py, pz);
            var5.vertex(px, py2, pz);
            var5.end();
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px2, py, pz);
            var5.vertex(px2, py2, pz);
            var5.end();
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px2, py, pz2);
            var5.vertex(px2, py2, pz2);
            var5.end();
            var5.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
//            var5.setBrightness(240);
            var5.color(color[0], color[1], color[2], color[3]);
            var5.vertex(px, py, pz2);
            var5.vertex(px, py2, pz2);
            var5.end();
        }
        if (filled) {
            var5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION); //LINES_LOOP
//            var5.setBrightness(240);
            //var5.setBrightness(255);
            var5.color(color[0], color[1], color[2], (int) (color[3] * 0.375F));

            var5.vertex(px, py, pz);
            var5.vertex(px2, py, pz);
            var5.vertex(px2, py, pz2);
            var5.vertex(px, py, pz2);

            var5.vertex(px2, py, pz);
            var5.vertex(px2, py2, pz);
            var5.vertex(px2, py2, pz2);
            var5.vertex(px2, py, pz2);

            var5.vertex(px, py2, pz);
            var5.vertex(px, py, pz);
            var5.vertex(px, py, pz2);
            var5.vertex(px, py2, pz2);

            var5.vertex(px, py2, pz2);
            var5.vertex(px, py, pz2);
            var5.vertex(px2, py, pz2);
            var5.vertex(px2, py2, pz2);

            var5.vertex(px, py, pz);
            var5.vertex(px, py2, pz);
            var5.vertex(px2, py2, pz);
            var5.vertex(px2, py, pz);

            var5.vertex(px2, py2, pz);
            var5.vertex(px, py2, pz);
            var5.vertex(px, py2, pz2);
            var5.vertex(px2, py2, pz2);
            var5.end();
        }

        ReikaRenderHelper.exitGeoDraw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public static AABB getBlockAABB(BlockPos te) {
        return getBlockAABB(te.getX(), te.getY(), te.getZ());
    }

    public static AABB getBlockAABB(BlockEntity te) {
        return getBlockAABB(te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ());
    }

    /**
     * Returns a 1-block bounding box. Args: x, y, z
     */
    public static AABB getBlockAABB(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    public static AABB getZeroAABB() {
        return new AABB(0, 0, 0, 0, 0, 0);
    }

    /**
     * Returns a sized bounding box centered on a Blocks. Args: x, y, z
     */
    public static AABB getBlockCenteredAABB(int x, int y, int z, double range) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1).expandTowards(range, range, range);
    }

    public static AABB getEntityCenteredAABB(Entity e, double range) {
        return new AABB(e.getX(), e.getY(), e.getZ(), e.getY(), e.getY(), e.getZ()).inflate(range, range, range); //todo either inflate or expandTowards
    }

    public static AABB getSizedBlockAABB(int x, int y, int z, float size) {
        size = size / 2F;
        return new AABB(x + 0.5 - size, y + 0.5 - size, z + 0.5 - size, x + 0.5 + size, y + 0.5 + size, z + 0.5 + size);
    }

    /*todo the getBlockBoundsMinX doesnt exist

       public static HashSet<BlockPos> getBlocksIntersectingAABB(AABB box, Level world, boolean checkCollideable) {
        HashSet<BlockPos> c = new HashSet<>();
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (checkCollideable) {
                        if (!b.isCollidable())
                            continue;
                    }
                    if (x == minX && box.minX >= x + b.getBlockBoundsMaxX()) {
                        continue;
                    } else if (x == maxX && box.maxX <= x + b.getBlockBoundsMinX()) {
                        continue;
                    } else if (y == minY && box.minY >= y + b.getBlockBoundsMaxY()) {
                        continue;
                    } else if (y == maxY && box.maxY <= y + b.getBlockBoundsMinY()) {
                        continue;
                    } else if (z == minZ && box.minZ >= z + b.getBlockBoundsMaxZ()) {
                        continue;
                    } else if (z == maxZ && box.maxZ <= z + b.getBlockBoundsMinZ()) {
                        continue;
                    }
                    c.add(new BlockPos(x, y, z));
                }
            }
        }
        return c;
    }*/

    public static double getVolume(AABB box) {
        return (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
    }

    public static AABB getBeamBox(int x, int y, int z, Direction dir, int dist) {
        AABB box = ReikaAABBHelper.getBlockAABB(x, y, z);
        int dx = dir.getStepX() * dist;
        int dy = dir.getStepY() * dist;
        int dz = dir.getStepZ() * dist;
        box = box.expandTowards(dx, dy, dz);
        return box;
    }

    public static AABB getBeamBox(int x, int y, int z, Direction dir, int d1, int d2) {
        int x1 = x + dir.getStepX() * d1;
        int y1 = y + dir.getStepY() * d1;
        int z1 = z + dir.getStepZ() * d1;
        int x2 = dir.getStepX() * d2;
        int y2 = dir.getStepY() * d2;
        int z2 = dir.getStepZ() * d2;
        return getBeamBox(x1, y1, z1, x2, y2, z2);
    }

    public static AABB getBeamBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return getBlockAABB(x1, y1, z1).expandTowards(x2, y2, z2);
    }

    public static AABB copyAABB(AABB box) {
        return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static AABB fromPoints(DecimalPosition... points) {
        double[] limits = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int i = 0; i < points.length; i++) {
            limits[0] = Math.min(limits[0], points[i].xCoord);
            limits[1] = Math.min(limits[1], points[i].yCoord);
            limits[2] = Math.min(limits[2], points[i].zCoord);
            limits[3] = Math.max(limits[3], points[i].xCoord);
            limits[4] = Math.max(limits[4], points[i].yCoord);
            limits[5] = Math.max(limits[5], points[i].zCoord);
        }
        return new AABB(limits[0], limits[1], limits[2], limits[3], limits[4], limits[5]);
    }

    public static AABB scaleAABB(AABB box, double sx, double sy, double sz) {
        double dx = (sx - 1) * (box.maxX - box.minX);
        double dy = (sy - 1) * (box.maxY - box.minY);
        double dz = (sz - 1) * (box.maxZ - box.minZ);
        return box.expandTowards(dx, dy, dz);
    }

    public static void compressAABB(AABB box, double dx, double dy, double dz) {
        box.setMinX(dx += box.maxX);
        box.setMinY(dy += box.maxY);
        box.setMinZ(dz += box.maxZ);

        box.setMaxX(dx -= box.minX);
        box.setMaxY(dy -= box.minY);
        box.setMaxZ(dz -= box.minZ);
    }

    public static void fillAABB(AABB box, int nx, int ny, int nz, int mx, int my, int mz) {
        box.setMinX(Math.min(box.minX, nx));
        box.setMinY(Math.min(box.minY, ny));
        box.setMinZ(Math.min(box.minZ, nz));

        box.setMaxX(Math.max(box.maxX, mx));
        box.setMaxY(Math.max(box.maxY, my));
        box.setMaxZ(Math.max(box.maxZ, mz));
    }

    public static AABB getPointAABB(double x, double y, double z, double r) {
        return getPointAABB(x, y, z, r, r);
    }

    public static AABB getPointAABB(double x, double y, double z, double rxz, double ry) {
        return new AABB(x - rxz, y - ry, z - rxz, x + rxz, y + ry, z + rxz);
    }

    public static boolean fullyContains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.maxX >= inner.maxX && outer.minY <= inner.minY && outer.maxY >= inner.maxY && outer.minZ <= inner.minZ && outer.maxZ >= inner.maxZ;
    }

}
