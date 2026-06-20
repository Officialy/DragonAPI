package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.interfaces.TileModel;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

import java.util.ArrayList;
import java.util.EnumSet;

// 26.2: Tesselator and MultiBufferSource were removed. World geometry is now submitted through the
// feature pipeline via SubmitNodeCollector#submitCustomGeometry, which hands the lambda a
// PoseStack.Pose (snapshot of the transform) and the RenderType's VertexConsumer. The draw helpers
// therefore take a SubmitNodeCollector obtained from the caller's render context (BER submit, level
// render event, etc.).
public class ReikaRenderHelper {

    private static boolean entityLighting;
    private static boolean generalLighting;
    private static float ptick = -1;
    public static double thirdPersonDistance;

    private static int frame = -1;

    /**
     * Renders a flat circle in the world. Args: collector, radius, center x,y,z, RGBA, angle step
     */
    public static void renderCircle(SubmitNodeCollector collector, double r, double x, double y, double z, int rgba, int step) {
        renderCircle(collector, new PoseStack(), r, x, y, z, rgba, step);
    }

    public static void renderCircle(SubmitNodeCollector collector, PoseStack stack, double r, double x, double y, double z, int rgba, int step) {
        final int red = (rgba >> 16) & 0xFF;
        final int green = (rgba >> 8) & 0xFF;
        final int blue = rgba & 0xFF;
        final int alpha = (rgba >> 24) & 0xFF;
        collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 360; i += step) {
                double a = Math.toRadians(i);
                buffer.addVertex(pose, (float) (x + r * Math.cos(a)), (float) y, (float) (z + r * Math.sin(a)))
                        .setColor(red, green, blue, alpha);
            }
        });
    }

    public static void spawnDropParticles(ClientLevel world, BlockPos pos, Block b) {
        spawnDropParticles(world, pos.getX(), pos.getY(), pos.getZ(), b);
    }

    public static void spawnDropParticles(ClientLevel world, int x, int y, int z, Block b) {
        int n = 12 + DragonAPI.rand.nextInt(12);
        spawnDropParticles(world, x, y, z, b, n);
    }

    public static void spawnDropParticles(ClientLevel world, int x, int y, int z, Block b, int n) {
        for (int i = 0; i < n; i++) {
            double vx = ReikaRandomHelper.getRandomPlusMinus(0D, 0.25);
            double vz = ReikaRandomHelper.getRandomPlusMinus(0D, 0.25);
            double vy = ReikaRandomHelper.getRandomBetween(0.125, 1);
            Minecraft.getInstance().particleEngine.add(new TerrainParticle(world, x + DragonAPI.rand.nextDouble(), y + DragonAPI.rand.nextDouble(), z + DragonAPI.rand.nextDouble(), vx, vy, vz, b.defaultBlockState(), new BlockPos(x, y, z))); //todo fix blockpos xyz
        }
    }

    /**
     * Renders a vertical-plane circle in the world. Args: collector, radius, center x,y,z, RGBA, phi, angle step
     */
    public static void renderVCircle(SubmitNodeCollector collector, double r, double x, double y, double z, int rgba, double phi, int step) {
        renderVCircle(collector, new PoseStack(), r, x, y, z, rgba, phi, step);
    }

    public static void renderVCircle(SubmitNodeCollector collector, PoseStack stack, double r, double x, double y, double z, int rgba, double phi, int step) {
        final int red = (rgba >> 16) & 0xFF;
        final int green = (rgba >> 8) & 0xFF;
        final int blue = rgba & 0xFF;
        final int alpha = (rgba >> 24) & 0xFF;
        collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 360; i += step) {
                int sign = 1;
                double h = r * Math.cos(ReikaPhysicsHelper.degToRad(i));
                if (i >= 180)
                    sign = -1;
                float vx = (float) (x - Math.sin(Math.toRadians(phi)) * (sign) * (Math.sqrt(r * r - h * h)));
                float vy = (float) (y + r * Math.cos(Math.toRadians(i)));
                float vz = (float) (z + r * Math.sin(Math.toRadians(i)) * Math.cos(Math.toRadians(phi)));
                buffer.addVertex(pose, vx, vy, vz).setColor(red, green, blue, alpha);
            }
        });
    }

    public static void rerenderAllChunksLazily() {
        Level world = Minecraft.getInstance().level;
        Player ep = Minecraft.getInstance().player;
        int r = 192;
        int x1 = Mth.floor(ep.getX() - r);
        int x2 = Mth.floor(ep.getX() + r);
        int z1 = Mth.floor(ep.getZ() - r);
        int z2 = Mth.floor(ep.getZ() + r);
        world.sendBlockUpdated(new BlockPos(x1, 0, z1), world.getBlockState(new BlockPos(x2, world.getHeight() - 1, z2)), world.getBlockState(new BlockPos(x1, 0, z1)), 2);
    }

    /**
     * Renders a line between two points in the world. Args: collector, stack, Start xyz, End xyz, rgb
     */
    public static void renderLine(SubmitNodeCollector collector, PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int[] color) {
        collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, buffer) -> {
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                    .setColor(color[0], color[1], color[2], color[3])
                    .setNormal(pose, 1, 1, 1);
            buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                    .setColor(color[0], color[1], color[2], color[3])
                    .setNormal(pose, 1, 1, 1);
        });
    }

    public static void renderTube(SubmitNodeCollector collector, PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int c1, int c2, double r1, double r2, int sides) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        stack.pushPose();
        stack.translate(x1, y1, z1);

        double f7 = Math.sqrt(dx * dx + dz * dz);
        final double f8 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double ang1 = -Math.atan2(dz, dx) * 180 / Math.PI - 90;
        double ang2 = -Math.atan2(f7, dy) * 180 / Math.PI - 90;
        stack.mulPose(Axis.YP.rotationDegrees((float) ang1));
        stack.mulPose(Axis.XP.rotationDegrees((float) ang2));

        // Snapshot the transformed pose; the submitted geometry is drawn after we pop the stack.
        PoseStack snap = new PoseStack();
        snap.last().set(stack.last());
        stack.popPose();

        collector.submitCustomGeometry(snap, RenderTypes.leash(), (pose, buffer) -> {
            for (int i = 0; i <= sides; i++) {
                double f11a = r1 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
                double f12a = r1 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
                double f11b = r2 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
                double f12b = r2 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
                buffer.addVertex(pose, (float) f11a, (float) f12a, 0F)
                        .setColor(c1 & 0xff, (c1 >> 8) & 0xff, (c1 >> 16) & 0xff, (c1 >> 24) & 0xff)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
                buffer.addVertex(pose, (float) f11b, (float) f12b, (float) f8)
                        .setColor(c2 & 0xff, (c2 >> 8) & 0xff, (c2 >> 16) & 0xff, (c2 >> 24) & 0xff)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
            }
        });
    }

    public static void disableLighting() {
//       todo Minecraft.getInstance().entityentityRenderer.disableLightmap(1);
//        RenderHelper.disableStandardItemLighting();
//        GL11.glDisable(GL11.GL_LIGHTING);
    }

    public static void enableLighting() {
        enableEntityLighting();
//        todo RenderHelper.enableStandardItemLighting();
//     todo   GL11.glEnable(GL11.GL_LIGHTING);
    }

    public static void disableEntityLighting() {
//     todo   OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit); //block/sky light grid image
//        RenderSystem.disableTexture();
//        todo OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static void enableEntityLighting() {
//   todo     OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
//        RenderSystem.enableTexture();
//    todo    OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Prepare for drawing primitive geometry by disabling all lighting and textures. Args: Is alpha going to be used
     */
    public static void prepareGeoDraw(boolean alpha) {
        disableLighting();
//        RenderSystem.disableTexture(); // Removed in 1.21
        // Alpha blending is now handled through RenderTypes
    }

    public static void exitGeoDraw() {
        enableLighting();
//        RenderSystem.enableTexture(); // Removed in 1.21
        // Blending is now handled through RenderTypes
    }

    /**
     * Renders a rectangle in-world. Args: collector, r,g,b,a, Start x,y,z, End x,y,z
     */
    public static void renderRectangle(SubmitNodeCollector collector, int r, int g, int b, int a, double x1, double y1, double z1, double x2, double y2, double z2) {
        collector.submitCustomGeometry(new PoseStack(), RenderTypes.debugQuads(), (pose, buffer) -> {
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
            buffer.addVertex(pose, (float) x2, (float) y1, (float) z2).setColor(r, g, b, a);
            buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
            buffer.addVertex(pose, (float) x1, (float) y2, (float) z1).setColor(r, g, b, a);
        });
    }

    public static int getRealFOV() {
        double base = Minecraft.getInstance().options.fov().get(); //-14;
        double diff = ((-40 + 70 - base) / 40F) * 15F;
        double ang = base + diff;
//        ReikaJavaLibrary.pConsole(ang);
        return (int) ang;
    }

    public static void renderEnchantedModel(BlockEntity tile, TileModel model, ArrayList li, float rotation, PoseStack stack, SubmitNodeCollector collector) {
        float f9 = (System.nanoTime() / 100000000) % 64 / 64F;

        stack.pushPose();
        stack.translate(f9, f9, f9);

        stack.translate(0, 2, 2);
        stack.scale(1.0F, -1.0F, -1.0F);
        stack.translate(0.5F, 0.5F, 0.5F);

        stack.mulPose(Axis.YP.rotationDegrees(rotation));

        double d = 1.0125;
        int p = 2;
        stack.translate(0, p, 0);
        stack.scale((float) d, (float) d, (float) d);
        stack.translate(0, -p, 0);

        // Snapshot the transformed pose so the deferred submit draws with it after we pop.
        final PoseStack snap = new PoseStack();
        snap.last().set(stack.last());

        stack.translate(0, p, 0);
        stack.scale((float) (1D / d), (float) (1D / d), (float) (1D / d));
        stack.translate(0, -p, 0);
        stack.popPose();

        final int light = LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(snap, RenderTypes.glintTranslucent(), (pose, buffer) -> {
            model.renderAll(snap, buffer, light, tile, li);
        });
    }

    public static long getRenderFrame() {
        return frame;
    }

    public static float getPartialTickTime() {
        return ptick;
    }

    public static Matrix4f getModelviewMatrix() {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX);
    }

    public static Matrix4f getProjectionMatrix() {
        return getMatrix(GL11.GL_PROJECTION_MATRIX);
    }

    public static Matrix4f getTextureMatrix() {
        return getMatrix(GL11.GL_TEXTURE_MATRIX);
    }

    private static Matrix4f getMatrix(int id) {
        Matrix4f mat = new Matrix4f();
        return mat; // Return identity for now to prevent crashes
    }

    public static class RenderTick implements TickRegistry.TickHandler {

        @Override
        public void tick(TickRegistry.TickType type, Object... tickData) {
            frame++;
            ptick = (Float) tickData[0];
        }

        @Override
        public EnumSet<TickRegistry.TickType> getType() {
            return EnumSet.of(TickRegistry.TickType.RENDER);
        }

        @Override
        public boolean canFire(TickRegistry.Phase p) {
            return p == TickRegistry.Phase.START;
        }

        @Override
        public String getLabel() {
            return null;
        }

    }

}
