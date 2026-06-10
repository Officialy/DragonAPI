package reika.dragonapi.libraries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import org.joml.Matrix4f;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

public class ReikaAABBHelper {

    /**
     * Legacy PoseStack-only overload — no-op in 26.1. Use the SubmitNodeCollector overload.
     */
    public static void renderAABB(PoseStack stack, AABB box, int x, int y, int z, int a, int r, int g, int b, boolean line) {
    }

    /**
     * Renders an AABB bounding box via the 26.1 SubmitNodeCollector pipeline.
     * Box is in world coordinates; blockX/Y/Z is the BER origin (subtracted to get local coords).
     * Draws translucent filled faces + opaque wireframe edges.
     *
     * @param fillRT  RenderType for quads (e.g. debugFilledBox or a no-depth variant)
     * @param lineRT  RenderType for lines (e.g. lines() or a no-depth variant)
     */
    public static void renderAABB(PoseStack stack, SubmitNodeCollector collector,
                                   AABB box, int blockX, int blockY, int blockZ,
                                   int a, int r, int g, int b, boolean line,
                                   RenderType fillRT, RenderType lineRT) {
        if (a <= 0) return;
        final float x0 = (float) (box.minX - blockX);
        final float y0 = (float) (box.minY - blockY);
        final float z0 = (float) (box.minZ - blockZ);
        final float x1 = (float) (box.maxX - blockX);
        final float y1 = (float) (box.maxY - blockY);
        final float z1 = (float) (box.maxZ - blockZ);

        int alpha = Math.min(255, a);
        int fillAlpha = Math.max(0, (int) (alpha * 0.375f));
        int fillRgba = (fillAlpha << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        int lineRgba = (alpha << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

        collector.submitCustomGeometry(stack, fillRT, (pose, vc) -> {
            vc.addVertex(pose, x0, y0, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y0, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y0, z1).setColor(fillRgba);
            vc.addVertex(pose, x0, y0, z1).setColor(fillRgba);

            vc.addVertex(pose, x0, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z0).setColor(fillRgba);
            vc.addVertex(pose, x0, y1, z0).setColor(fillRgba);

            vc.addVertex(pose, x0, y0, z0).setColor(fillRgba);
            vc.addVertex(pose, x0, y1, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y0, z0).setColor(fillRgba);

            vc.addVertex(pose, x1, y0, z1).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x0, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x0, y0, z1).setColor(fillRgba);

            vc.addVertex(pose, x0, y0, z1).setColor(fillRgba);
            vc.addVertex(pose, x0, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x0, y1, z0).setColor(fillRgba);
            vc.addVertex(pose, x0, y0, z0).setColor(fillRgba);

            vc.addVertex(pose, x1, y0, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z0).setColor(fillRgba);
            vc.addVertex(pose, x1, y1, z1).setColor(fillRgba);
            vc.addVertex(pose, x1, y0, z1).setColor(fillRgba);
        });

        if (line) {
            collector.submitCustomGeometry(stack, lineRT, (pose, vc) -> {
                aabbEdge(pose, vc, x0, y0, z0, x1, y0, z0, lineRgba);
                aabbEdge(pose, vc, x1, y0, z0, x1, y0, z1, lineRgba);
                aabbEdge(pose, vc, x1, y0, z1, x0, y0, z1, lineRgba);
                aabbEdge(pose, vc, x0, y0, z1, x0, y0, z0, lineRgba);
                aabbEdge(pose, vc, x0, y1, z0, x1, y1, z0, lineRgba);
                aabbEdge(pose, vc, x1, y1, z0, x1, y1, z1, lineRgba);
                aabbEdge(pose, vc, x1, y1, z1, x0, y1, z1, lineRgba);
                aabbEdge(pose, vc, x0, y1, z1, x0, y1, z0, lineRgba);
                aabbEdge(pose, vc, x0, y0, z0, x0, y1, z0, lineRgba);
                aabbEdge(pose, vc, x1, y0, z0, x1, y1, z0, lineRgba);
                aabbEdge(pose, vc, x1, y0, z1, x1, y1, z1, lineRgba);
                aabbEdge(pose, vc, x0, y0, z1, x0, y1, z1, lineRgba);
            });
        }
    }

    private static void aabbEdge(PoseStack.Pose pose, VertexConsumer vc,
                                  float ax, float ay, float az, float bx, float by, float bz, int rgba) {
        float nx = bx - ax, ny = by - ay, nz = bz - az;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        vc.addVertex(pose, ax, ay, az).setColor(rgba).setNormal(pose, nx, ny, nz).setLineWidth(2.0f);
        vc.addVertex(pose, bx, by, bz).setColor(rgba).setNormal(pose, nx, ny, nz).setLineWidth(2.0f);
    }

    @SuppressWarnings("unused")
    private static void renderAABB_legacy(PoseStack stack, AABB box, int x, int y, int z, int a, int r, int g, int b, boolean line) {
        // Old Tesselator API (getBuilder, begin, vertex, end) removed in 26.1 — body preserved below as reference only
        /*
        int[] color = {r, g, b, a};

        float xdiff = (float) box.minX;// - x;
        float ydiff = (float) box.minY;// - y;
        float zdiff = (float) (box.minZ );//- z);
        float xdiff2 = (float) (box.maxX) ;//- x);
        float ydiff2 = (float) box.maxY;// - y;
        float zdiff2 = (float) box.maxZ;// - z;

        float px = par2 + xdiff;
        float py = par4 + ydiff;
        float pz = par6 + zdiff;
        float px2 = par2 + xdiff2;
        float py2 = par4 + ydiff2;
        float pz2 = par6 + zdiff2;
//        if (builder.building())
//            builder.end();
        if (line) {
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px2, py2, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py2, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py2, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px2, py2, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();

            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px2, py, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px2, py, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();

            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px, py, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py2, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();

            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px2, py, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px2, py2, pz).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();

            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px2, py, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px2, py2, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();

            builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP
            builder.vertex(mat, px, py, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            builder.vertex(mat, px, py2, pz2).color(color[0], color[1], color[2], color[3]).endVertex();
            tess.end();
        }
        if (filled) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR); //LINES_LOOP

            builder.vertex(mat, px, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();

            builder.vertex(mat, px2, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();

            builder.vertex(mat, px, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();

            builder.vertex(mat, px, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();

            builder.vertex(mat, px, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();

            builder.vertex(mat, px2, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py2, pz).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            builder.vertex(mat, px2, py2, pz2).color(color[0], color[1], color[2], (int) (color[3] * 0.375F)).endVertex();
            tess.end();
        }

        ReikaRenderHelper.exitGeoDraw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        stack2.popPose();
        */
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
        return new AABB(x, y, z, x + 1, y + 1, z + 1).inflate(range, range, range);
    }

    public static AABB getEntityCenteredAABB(Entity e, double range) {
        return new AABB(e.getX(), e.getY(), e.getZ(), e.getX(), e.getY(), e.getZ()).inflate(range, range, range);
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
