package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.interfaces.TileModel;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;

public class ReikaRenderHelper {

    static RenderType type;
    private static boolean entityLighting;
    private static boolean generalLighting;
    private static float ptick = -1;
    public static double thirdPersonDistance;

    private static int frame = -1;

    /**
     * Converts a biome to a color multiplier (for use in things like leaf textures).
     * Args: Level, x, z, material (grass, water, etc), bit
     * public static float biomeToColorMultiplier(Level world, int x, int y, int z, Material mat, int bit) {
     * int[] color = ReikaBiomeHelper.biomeToRGB(world, x, y, z, mat);
     * float mult = ReikaColorAPI.RGBtoColorMultiplier(color, bit);
     * return mult;
     * }
     */

    /**
     * Renders a flat circle in the world. Args: radius, center x,y,z, RGBA, angle step
     */
    public static void renderCircle(double r, double x, double y, double z, int rgba, int step) {
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderer = tessellator.getBuilder();

        //if (renderer.isDrawing)
        //    renderer.draw();
        renderer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR); //GL11.GL_LINE_LOOP
        for (int i = 0; i < 360; i += step) {
            double a = Math.toRadians(i);
            renderer.vertex(x + r * Math.cos(a), y, z + r * Math.sin(a)).color(rgba, rgba >> 24 & 255, rgba, rgba).endVertex();
        }
        //renderer.draw();
        tessellator.end();
        //GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
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
     * Renders a vertical-plane circle in the world. Args: radius, center x,y,z, RGBA, phi, angle step
     */
    public static void renderVCircle(double r, double x, double y, double z, int rgba, double phi, int step) {
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderer = tessellator.getBuilder();

        renderer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR); //GL11.GL_LINE_LOOP
        renderer.color(rgba, rgba >> 24 & 255, rgba, rgba); //TODO fix coloring
        for (int i = 0; i < 360; i += step) {
            int sign = 1;
            double h = r * Math.cos(ReikaPhysicsHelper.degToRad(i));
            if (i >= 180)
                sign = -1;
            renderer.vertex(x - Math.sin(Math.toRadians(phi)) * (sign) * (Math.sqrt(r * r - h * h)), y + r * Math.cos(Math.toRadians(i)), z + r * Math.sin(Math.toRadians(i)) * Math.cos(Math.toRadians(phi))).endVertex();
        }
        //renderer.draw();
        tessellator.end();
        //GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

//    public static void rerenderAllChunks() {
//        Minecraft.getInstance().gameRenderer.loadRenderers();
//    }

    public static void rerenderAllChunksLazily() {
        Level world = Minecraft.getInstance().level;
        Player ep = Minecraft.getInstance().player;
        int r = 192;
        int x1 = Mth.floor(ep.getX() - r);
        int x2 = Mth.floor(ep.getX() + r);
        int z1 = Mth.floor(ep.getZ() - r);
        int z2 = Mth.floor(ep.getZ() + r);
        world.blockUpdated(new BlockPos(x1, 0, z1), world.getBlockState(new BlockPos(x2, world.getHeight() - 1, z2)).getBlock());
    }

    /**
     * Renders a line between two points in the world. Args: Start xyz, End xyz, rgb
     */
    public static void renderLine(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int[] color) {
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderer = tessellator.getBuilder();

        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        renderer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR_NORMAL); //GL11.GL_LINE_LOOP
        renderer.vertex(stack.last().pose(), (float) x1, (float) y1, (float) z1).color(color[0], color[1], color[2], color[3]).normal(stack.last().normal(), 1, 1, 1).endVertex();
        renderer.vertex(stack.last().pose(), (float) x2, (float) y2, (float) z2).color(color[0], color[1], color[2], color[3]).normal(stack.last().normal(), 1, 1, 1).endVertex();
        tessellator.end();

        //GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        //GL11.glEnable(GL11.GL_CULL_FACE);
        //GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderSystem.lineWidth(1f);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
    }

    public static void renderTube(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int c1, int c2, double r1, double r2, int sides) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder renderer = tessellator.getBuilder();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        stack.pushPose();
        stack.translate(x1, y1, z1);

        //ReikaJavaLibrary.pConsole(x1+","+y1+","+z1+"  >  "+x2+","+y2+","+z2);

        double f7 = Math.sqrt(dx * dx + dz * dz);
        double f8 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double ang1 = -Math.atan2(dz, dx) * 180 / Math.PI - 90;
        double ang2 = -Math.atan2(f7, dy) * 180 / Math.PI - 90;
        stack.mulPose(new Quaternionf((float) ang1, 0, 1, 0));
        stack.mulPose(new Quaternionf((float) ang2, 1, 0, 0));

        renderer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR); //GL11.GL_TRIANGLE_STRIP
        //renderer.setBrightness(240); todo fix brightness
        for (int i = 0; i <= sides; i++) {
            double f11a = r1 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
            double f12a = r1 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
            double f11b = r2 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
            double f12b = r2 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
            double f13 = i % sides * 1 / sides;
            renderer.vertex(f11a, f12a, 0).color(c1 & 0xffffff, c1 >> 24 & 255, c1, c1).endVertex();
            renderer.vertex(f11b, f12b, f8).color(c2 & 0xffffff, c2 >> 24 & 255, c2, c2).endVertex();
        }
        tessellator.end();

        stack.popPose();
    }

	/*
	public static void updateAllWorldRenderers() {
		try {
			Field f = RenderGlobal.class.getDeclaredField("worldRenderers");
			f.setAccessible(true);
			WorldRenderer[] w = (WorldRenderer[])f.get(Minecraft.getInstance().renderGlobal);
			for (int i = 0; i < w.length; i++) {
				w[i].markDirty();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/

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
        RenderSystem.disableTexture();
//        todo OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static void enableEntityLighting() {
//   todo     OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        RenderSystem.enableTexture();
//    todo    OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Prepare for drawing primitive geometry by disabling all lighting and textures. Args: Is alpha going to be used
     */
    public static void prepareGeoDraw(boolean alpha) {
        disableLighting();
        RenderSystem.disableTexture();
        if (alpha)
            RenderSystem.enableBlend();
    }

    public static void exitGeoDraw() {
        enableLighting();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Renders a rectangle in-world. Args: r,g,b,a, Start x,y,z, End x,y,z
     */
    public static void renderRectangle(int r, int g, int b, int a, double x1, double y1, double z1, double x2, double y2, double z2) {
        var tessellator = Tesselator.getInstance();
        var renderer = tessellator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        renderer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        renderer.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
        renderer.vertex(x2, y1, z2).color(r, g, b, a).endVertex();
        renderer.vertex(x2, y2, z2).color(r, g, b, a).endVertex();
        renderer.vertex(x1, y2, z1).color(r, g, b, a).endVertex();
        tessellator.end();
    }

    public static int getRealFOV() {
        double base = Minecraft.getInstance().options.fov().get(); //-14;
        double diff = ((-40 + 70 - base) / 40F) * 15F;
        double ang = base + diff;
//        ReikaJavaLibrary.pConsole(ang);
        return (int) ang;
    }

    public static void renderEnchantedModel(BlockEntity tile, TileModel model, ArrayList li, float rotation, PoseStack stack, MultiBufferSource source) {
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        float f9 = (System.nanoTime() / 100000000) % 64 / 64F;
        //ReikaTextureHelper.bindEnchantmentTexture();

        source.getBuffer(type);
        RenderSystem.enableBlend();
//        ReikaGLHelper.BlendMode.OVERLAYDARK.apply();
        float f10 = 0.5F;
        RenderSystem.setShaderColor(f10, f10, f10, 1.0F);
        stack.pushPose();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        stack.translate(f9, f9, f9);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glPushMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        stack.translate(0, 2, 2);
        stack.scale(1.0F, -1.0F, -1.0F);
        stack.translate(0.5F, 0.5F, 0.5F);
        float f11 = 0.76F;
        RenderSystem.setShaderColor(0.5F * f11, 0.25F * f11, 0.8F * f11, 1.0F);
        stack.mulPose(Axis.YP.rotationDegrees(rotation));
        GL11.glDepthMask(false);

        GL11.glDisable(GL11.GL_LIGHTING);

        double d = 1.0125;
        int p = 2;
        stack.translate(0, p, 0);
        stack.scale((float) d, (float) d, (float) d);
        stack.translate(0, -p, 0);

        VertexConsumer vertexconsumer = source.getBuffer(RenderType.glintTranslucent()); //todo check if this is the enchantment glint?
        model.renderAll(stack, vertexconsumer, /*todo LIGHT*/0, tile, li);

        stack.translate(0, p, 0);
        stack.scale((float) (1D / d), (float) (1D / d), (float) (1D / d));
        stack.translate(0, -p, 0);

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glDepthMask(true);

        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glEnable(GL11.GL_ALPHA_TEST);

        GL11.glPopMatrix();
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        //ReikaGLHelper.RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        stack.popPose();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(id); //id, buf   //TODO this might be broken as its GL11 stuff
        buf.rewind();
        Matrix4f mat = new Matrix4f();
        mat.set(buf);
        return mat;
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
        public boolean canFire(TickEvent.Phase p) {
            return p == TickEvent.Phase.START;
        }

        @Override
        public String getLabel() {
            return null;
        }

    }

}

