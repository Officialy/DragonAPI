package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
// ModelResourceLocation removed - use ResourceLocation instead
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.DragonAPI;
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

    private static void draw(RenderType type, BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh != null) {
            type.draw(mesh);
        }
    }

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
    /**
     * Renders a flat circle in the world. Args: radius, center x,y,z, RGBA, angle step
     */
    public static void renderCircle(double r, double x, double y, double z, int rgba, int step) {
        renderCircle(new PoseStack(), r, x, y, z, rgba, step);
    }

    public static void renderCircle(PoseStack stack, double r, double x, double y, double z, int rgba, int step) {
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderType type = RenderType.debugLineStrip(1.0D);
        // In 1.21, Tesselator.getInstance().begin() returns a BufferBuilder.
        // We need to ensure we are using the correct mode and format.
        // debugLineStrip uses Mode.LINE_STRIP and POSITION_COLOR.
        BufferBuilder renderer = Tesselator.getInstance().begin(type.mode(), type.format());
        
        int red = (rgba >> 16) & 0xFF;
        int green = (rgba >> 8) & 0xFF;
        int blue = rgba & 0xFF;
        int alpha = (rgba >> 24) & 0xFF;
        Matrix4f matrix = stack.last().pose();
        for (int i = 0; i < 360; i += step) {
            double a = Math.toRadians(i);
            renderer.addVertex(matrix, (float)(x + r * Math.cos(a)), (float)y, (float)(z + r * Math.sin(a)))
                    .setColor(red, green, blue, alpha);
        }
        draw(type, renderer);
        //GL11.glDisable(GL12.GL_RESCALE_NORMAL);
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
     * Renders a vertical-plane circle in the world. Args: radius, center x,y,z, RGBA, phi, angle step
     */
    public static void renderVCircle(double r, double x, double y, double z, int rgba, double phi, int step) {
        renderVCircle(new PoseStack(), r, x, y, z, rgba, phi, step);
    }

    public static void renderVCircle(PoseStack stack, double r, double x, double y, double z, int rgba, double phi, int step) {
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderType type = RenderType.debugLineStrip(1.0D);
        BufferBuilder renderer = Tesselator.getInstance().begin(type.mode(), type.format());

        int red = (rgba >> 16) & 0xFF;
        int green = (rgba >> 8) & 0xFF;
        int blue = rgba & 0xFF;
        int alpha = (rgba >> 24) & 0xFF;
        Matrix4f matrix = stack.last().pose();
        
        for (int i = 0; i < 360; i += step) {
            int sign = 1;
            double h = r * Math.cos(ReikaPhysicsHelper.degToRad(i));
            if (i >= 180)
                sign = -1;
            float vx = (float)(x - Math.sin(Math.toRadians(phi)) * (sign) * (Math.sqrt(r * r - h * h)));
            float vy = (float)(y + r * Math.cos(Math.toRadians(i)));
            float vz = (float)(z + r * Math.sin(Math.toRadians(i)) * Math.cos(Math.toRadians(phi)));
            renderer.addVertex(matrix, vx, vy, vz).setColor(red, green, blue, alpha);
        }

        draw(type, renderer);
        //GL11.glDisable(GL12.GL_RESCALE_NORMAL);
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
        world.sendBlockUpdated(new BlockPos(x1, 0, z1), world.getBlockState(new BlockPos(x2, world.getHeight() - 1, z2)), world.getBlockState(new BlockPos(x1, 0, z1)), 2);
    }

    /**
     * Renders a line between two points in the world. Args: Start xyz, End xyz, rgb
     */
    public static void renderLine(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int[] color) {
        RenderType renderType = RenderType.lines();
        BufferBuilder renderer = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
        
        renderer.addVertex(stack.last().pose(), (float) x1, (float) y1, (float) z1)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(stack.last(), 1, 1, 1);
        renderer.addVertex(stack.last().pose(), (float) x2, (float) y2, (float) z2)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(stack.last(), 1, 1, 1);
        
        draw(renderType, renderer);
        
    }

    public static void renderTube(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2, int c1, int c2, double r1, double r2, int sides) {
        Tesselator tessellator = Tesselator.getInstance();

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
        stack.mulPose(Axis.YP.rotationDegrees((float) ang1));
        stack.mulPose(Axis.XP.rotationDegrees((float) ang2));

        RenderType renderType = RenderType.leash();
        BufferBuilder renderer = tessellator.begin(renderType.mode(), renderType.format());
        Matrix4f matrix = stack.last().pose();
        
        for (int i = 0; i <= sides; i++) {
            double f11a = r1 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
            double f12a = r1 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
            double f11b = r2 * Math.sin(i % sides * Math.PI * 2 / sides) * 0.75;
            double f12b = r2 * Math.cos(i % sides * Math.PI * 2 / sides) * 0.75;
            double f13 = i % sides / (double) sides;
            renderer.addVertex(matrix, (float)f11a, (float)f12a, 0F)
                    .setColor(c1 & 0xff, (c1 >> 8) & 0xff, (c1 >> 16) & 0xff, (c1 >> 24) & 0xff)
                    .setLight(LightTexture.FULL_BRIGHT);
            renderer.addVertex(matrix, (float)f11b, (float)f12b, (float)f8)
                    .setColor(c2 & 0xff, (c2 >> 8) & 0xff, (c2 >> 16) & 0xff, (c2 >> 24) & 0xff)
                    .setLight(LightTexture.FULL_BRIGHT);
        }
        draw(renderType, renderer);

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
     * Renders a rectangle in-world. Args: r,g,b,a, Start x,y,z, End x,y,z
     */
    public static void renderRectangle(int r, int g, int b, int a, double x1, double y1, double z1, double x2, double y2, double z2) {
        RenderType type = RenderType.debugQuads();
        var renderer = Tesselator.getInstance().begin(type.mode(), type.format());
        renderer.addVertex((float)x1, (float)y1, (float)z1).setColor(r, g, b, a);
        renderer.addVertex((float)x2, (float)y1, (float)z2).setColor(r, g, b, a);
        renderer.addVertex((float)x2, (float)y2, (float)z2).setColor(r, g, b, a);
        renderer.addVertex((float)x1, (float)y2, (float)z1).setColor(r, g, b, a);
        draw(type, renderer);
    }

    public static int getRealFOV() {
        double base = Minecraft.getInstance().options.fov().get(); //-14;
        double diff = ((-40 + 70 - base) / 40F) * 15F;
        double ang = base + diff;
//        ReikaJavaLibrary.pConsole(ang);
        return (int) ang;
    }

    public static void renderEnchantedModel(BlockEntity tile, TileModel model, ArrayList li, float rotation, PoseStack stack, MultiBufferSource source) {
        // int x = tile.getBlockPos().getX();
        // int y = tile.getBlockPos().getY();
        // int z = tile.getBlockPos().getZ();
        float f9 = (System.nanoTime() / 100000000) % 64 / 64F;
        
        // ReikaTextureHelper.bindEnchantmentTexture(); // Handled by RenderType.glintTranslucent()

        // source.getBuffer(type); // Not needed if we get specific buffer later

        stack.pushPose();
        stack.translate(f9, f9, f9);
        
        // GL11.glDepthFunc(GL11.GL_LEQUAL); // RenderSystem.depthFunc(GL11.GL_LEQUAL);
        // But usually we don't mess with depth func in mod code unless necessary.
        // RenderType.glintTranslucent() handles its own state.

        stack.translate(0, 2, 2);
        stack.scale(1.0F, -1.0F, -1.0F);
        stack.translate(0.5F, 0.5F, 0.5F);
        
        stack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // GL11.glDepthMask(false); // RenderSystem.depthMask(false);
        // Again, RenderType handles this.

        // GL11.glDisable(GL11.GL_LIGHTING); // No-op

        double d = 1.0125;
        int p = 2;
        stack.translate(0, p, 0);
        stack.scale((float) d, (float) d, (float) d);
        stack.translate(0, -p, 0);

        VertexConsumer vertexconsumer = source.getBuffer(RenderType.glintTranslucent());
        // We need to pass the packed overlay and light. 
        // For enchantment glint, usually light is ignored or full bright?
        // Let's use the tile's light if possible, or full bright.
        int light = LightTexture.FULL_BRIGHT; 
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // TileModel.renderAll needs to be updated to accept light/overlay if it doesn't already
        // Assuming it takes (PoseStack, VertexConsumer, int light, BlockEntity, ArrayList)
        model.renderAll(stack, vertexconsumer, light, tile, li);

        stack.translate(0, p, 0);
        stack.scale((float) (1D / d), (float) (1D / d), (float) (1D / d));
        stack.translate(0, -p, 0);

        // GL11.glLoadIdentity(); // This would clear the matrix, which is bad for PoseStack!
        // stack.popPose() handles restoring the state.

        // GL11.glDepthMask(true);
        // GL11.glEnable(GL11.GL_LIGHTING);
        // GL11.glPopMatrix();
        // GL11.glDepthFunc(GL11.GL_LEQUAL);

        stack.popPose();
        // GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // RenderSystem.setShaderColor(1,1,1,1);
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
        // FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        // GL11.glGetFloat(id); //id, buf   //TODO this might be broken as its GL11 stuff
        // buf.rewind();
        Matrix4f mat = new Matrix4f();
        // mat.set(buf);
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


