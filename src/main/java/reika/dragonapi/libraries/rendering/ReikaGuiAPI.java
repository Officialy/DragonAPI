package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.data.maps.RectangleMap;
import reika.dragonapi.instantiable.data.maps.RegionMap;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.objects.LineType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static reika.dragonapi.DragonAPI.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ReikaGuiAPI extends Screen {

    public static final ReikaGuiAPI instance = new ReikaGuiAPI();
    public static int NEI_DEPTH = 0;
    private final RectangleMap<String> tooltips = new RectangleMap<>();
    private final RegionMap<ItemStack> items = new RegionMap<>();
    private final boolean cacheRenders = ModList.NEI.isLoaded();
    private int xSize;
    private int ySize;

    private float zLevel = 0; //todo temporary, check if it works first

    private ReikaGuiAPI() {
        super(Component.nullToEmpty(null));
        minecraft = Minecraft.getInstance();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void setup() {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    public static void drawRectWithUV(PoseStack matrixStack, BufferBuilder buffer, int x, int y, int w, int h, int col, float u0, float v0, float u1, float v1) {
        if (w <= 0 || h <= 0) {
            return;
        }

        Matrix4f m = matrixStack.last().pose();
        buffer.vertex(m, x, y + h, 0).color(col).uv(u0, v1).endVertex();
        buffer.vertex(m, x + w, y + h, 0).color(col).uv(u1, v1).endVertex();
        buffer.vertex(m, x + w, y, 0).color(col).uv(u1, v0).endVertex();
        buffer.vertex(m, x, y, 0).color(col).uv(u0, v0).endVertex();
    }

    public int getScreenXInset() {
        return (width - xSize) / 2;
    }

    public int getScreenYInset() {
        return (height - ySize) / 2 - 8;
    }

    @SubscribeEvent
    public void preDrawScreen(ScreenEvent evt) {
        if (cacheRenders) {
            tooltips.clear();
            items.clear();
        }
    }

    /**
     * Renders the specified text to the screen, center-aligned.
     */
    public void drawCenteredStringNoShadow(PoseStack poseStack, Font par1FontRenderer, String par2Str, int par3, int par4, int par5) {
        par1FontRenderer.draw(poseStack, par2Str, par3 - par1FontRenderer.width(par2Str) / 2, par4, par5);
    }

    @Override
    public void render(PoseStack p_96562_, int p_96563_, int p_96564_, float p_96565_) {
        super.render(p_96562_, p_96563_, p_96564_, p_96565_);
    }

    /**
     * Draws a textured rectangle at the stored z-value. Args: x, y, u, v, width, height
     */
    public void drawTexturedModalRectInvert(int x, int y, int u, int v, int w, int h, int scale) {
        y += ySize / 2;
        y -= scale;
        v -= scale;
        h = scale;

        float var7 = 0.00390625F;
        float var8 = 0.00390625F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        buffer.vertex(x + 0, y + h, zLevel).uv((u + 0) * var7, (v + h) * var8);
        buffer.vertex(x + w, y + h, zLevel).uv((u + w) * var7, (v + h) * var8);
        buffer.vertex(x + w, y + 0, zLevel).uv((u + w) * var7, (v + 0) * var8);
        buffer.vertex(x + 0, y + 0, zLevel).uv((u + 0) * var7, (v + 0) * var8);
        tesselator.end();
    }

    /**
     * Draws a solid color rectangle with the specified coordinates and color. Modified and simplified from the original
     * in that it automatically handles alpha channel (makes completely transparent full opaque) and changes 4-point method
     * to x-y-width-height. Args: x, y-topleft, width, height, color, alpha on/off
     */
    public void drawRect(PoseStack matrixStack, int x, int y, int width, int height, int color, boolean enableAlpha) {
        int var5;
        if (!enableAlpha) {
            color = color | 0xff000000;
        }

//        if (x < width) {
//            var5 = x;
//            x = width;
//            width = var5;
//        }
//
//        if (y < height) {
//            var5 = y;
//            y = height;
//            height = var5;
//        }
        width += x;
        height += y;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, height, 0).color(color).endVertex();
        buffer.vertex(matrix, width, height, 0).color(color).endVertex();
        buffer.vertex(matrix, width, y, 0).color(color).endVertex();
        buffer.vertex(matrix, x, y, 0).color(color).endVertex();
//        ReikaJavaLibrary.pConsole(x + " " + y + " " + width + " " + height);
        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public void drawTexturedRect(PoseStack matrixStack, int x, int y, int w, int h, int color, float u0, float v0, float u1, float v1) {
        RenderSystem.enableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        Tesselator tesselator = Tesselator.getInstance();

        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        drawRectWithUV(matrixStack, buffer, x, y, w, h, color, u0, v0, u1, v1);
        tesselator.end();
    }

    /**
     * Draws a dotted line between two points. Args: poseStack, start x,y, end x,y, thickness, color
     */
    public void dottedLine(PoseStack poseStack, int x, int y, int x2, int y2, int t, int color) {
        if (x == x2 && y == y2)
            return;
        if (x != x2 && y == y2) {
            for (int i = x; i < x2 - t; i++) {
                this.drawRect(poseStack, i, y, i + t, y, color, false);
            }
        }
        if (y != y2 && x == x2) {
            for (int i = y; i < y2 - t; i++) {
                this.drawRect(poseStack, x, i, x, i + t, color, false);
            }
        }
        if (x != x2 && y != y2) {
            int xdiff = x2 - x;
            int ydiff = y2 - y;
            double slope = (double) ydiff / (double) xdiff;
            while (x < x2 - t) {
                this.drawRect(poseStack, x, y, x + t, y + t, color, false);
                x += xdiff;
                y += xdiff * slope;
            }
        }
    }

    /**
     * Draws a line between two points. Args: Start x,y, end x,y, color
     */
    public void drawLine(PoseStack matrixStack, int x, int y, int x2, int y2, int color) {
        this.drawLine(matrixStack, x, y, x2, y2, color, LineType.SOLID);
    }

    public void drawLine(PoseStack matrixStack, int x, int y, int x2, int y2, int color, LineType type) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        matrixStack.pushPose();

        if (RenderSystem.getShaderLineWidth() < 1.5F)
            RenderSystem.lineWidth(1.5F);

        //GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        int alpha = ReikaColorAPI.getAlpha(color);
        if (alpha == 0)
            alpha = 255;
        int red = ReikaColorAPI.getRed(color);
        int green = ReikaColorAPI.getGreen(color);
        int blue = ReikaColorAPI.getBlue(color);

        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        if (type != LineType.SOLID) {
            type.setMode(2);
        }

        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        buffer.vertex(matrix, x, y, 0).color(red / 255F, green / 255F, blue / 255F, alpha / 255F).normal(1, 1, 1).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(red / 255F, green / 255F, blue / 255F, alpha / 255F).normal(1, 1, 1).endVertex();
        tesselator.end();
        matrixStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
    }

    public void drawCircle(double x, double y, double radius, int color) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        int alpha = ReikaColorAPI.getAlpha(color);
        if (alpha == 0)
            alpha = 255;
        int red = ReikaColorAPI.getRed(color);
        int green = ReikaColorAPI.getGreen(color);
        int blue = ReikaColorAPI.getBlue(color);
        GL11.glDisable(GL11.GL_LIGHTING);
        //GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        RenderSystem.setShaderColor(red / 255F, green / 255F, blue / 255F, alpha / 255F);
        for (int i = 0; i < 360; i++) {
            GL11.glVertex2d(x + radius * Math.cos(Math.toRadians(i)), y + radius * Math.sin(Math.toRadians(i)));
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    /**
     * Draws a "fill bar" (rectangle from bottom up).
     * Args: left x, top y, width, bottom y, color, height, maxheight, alpha on/off
     */
    public void fillBar(PoseStack matrixStack, int x, int y, int w, int bottom, int c, int height, int maxHeight, boolean alpha) {
        int var5;
        if (!alpha) {
            int color = c;
            c /= 1000000; // make alpha-only
            c *= 1000000; // pad back to alpha bitspace
            c = 0xff000000 + (color - c); //subtract original color alpha, then add FF
        }

        y += (maxHeight - height);
        w += x;

        if (x < w) {
            var5 = x;
            x = w;
            w = var5;
        }

        if (y < bottom) {
            var5 = y;
            y = bottom;
            bottom = var5;
        }

        float var10 = (c >> 24 & 255) / 255.0F;
        float var6 = (c >> 16 & 255) / 255.0F;
        float var7 = (c >> 8 & 255) / 255.0F;
        float var8 = (c & 255) / 255.0F;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
//        RenderSystem.glDisable(GL11.GL_LIGHTING);


        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.vertex(matrix, x, bottom, 0).color(var6, var7, var8, var10).endVertex();
        buffer.vertex(matrix, w, bottom, 0).color(var6, var7, var8, var10).endVertex();
        buffer.vertex(matrix, w, y, 0).color(var6, var7, var8, var10).endVertex();
        buffer.vertex(matrix, x, y, 0).color(var6, var7, var8, var10).endVertex();
        tesselator.end();
        matrixStack.popPose();
    }

    public void drawItemStack(PoseStack matrixStack, ItemRenderer renderer, ItemStack is, int x, int y) {
        this.drawItemStack(matrixStack, renderer, Minecraft.getInstance().font, is, x, y);
    }

    /**
     * Note that this must be called after any and all texture and text rendering, as the lighting conditions are left a bit off
     */
    public void drawItemStack(PoseStack matrixStack, ItemRenderer renderer, Font fr, ItemStack is, int x, int y) {
        Font font = null;
        if (is == null)
            return;
        if (is.getItem() == null)
            return;
        if (is != null && is.getItem() != null)
            font = Minecraft.getInstance().font; // todo is.getItem().getFontRenderer(is);
        if (font == null)
            font = fr;

        Tesselator tesselator = Tesselator.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();

        matrixStack.pushPose();
        matrixStack.translate(x, y, 0);

        minecraft.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        setup();
        matrixStack.translate(8, 8, itemRenderer.blitOffset);
        matrixStack.scale(1, -1, 1);
        matrixStack.scale(16, 16, 16);
        MultiBufferSource.BufferSource renderTypeBufferImpl = minecraft.renderBuffers().bufferSource();

        short short1 = 240;
        short short2 = 240;


        BakedModel bakedModel = itemRenderer.getModel(is, minecraft.level, minecraft.player, 0);

        boolean flatItems = !bakedModel.usesBlockLight();


        itemRenderer.render(is, ItemTransforms.TransformType.GUI, false, matrixStack, renderTypeBufferImpl, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
        renderTypeBufferImpl.endBatch();
        RenderSystem.enableDepthTest();

        if (flatItems) {
            Lighting.setupForFlatItems();
        }

        if (cacheRenders)
            items.addRegionByWH(x, y, 16, 16, is.copy());

        matrixStack.popPose();
    }

	/*
	public void drawItemStackWithTooltip(ItemRenderer renderer, ItemStack is, int x, int y) {
		this.drawItemStackWithTooltip(renderer, Minecraft.getMinecraft().fontRenderer, is, x, y);
	}
	 */

    public void drawItemStackWithTooltip(PoseStack pose, ItemRenderer renderer, Font fr, ItemStack is, int x, int y, double mouseX, double mouseY) {
        pose.translate(0.0F, 0.0F, 32.0F);
        Font f2 = Minecraft.getInstance().font;//is.getItem().getFontRenderer(is);
        if (f2 != null)
            fr = f2;
        this.drawItemStack(pose, renderer, fr, is, x, y);

        pose.translate(0.0F, 0.0F, 32.0F);
        if (this.isMouseInBox(x, x + 16, y, y + 16, mouseX, mouseY)) {
            String sg = is.getDisplayName().getString();
            if (sg == null) {
                sg = is + "{" + is.getTag() + "}";
            }
            boolean right = mouseX < minecraft.screen.width / 2.0;
            if (right)
                this.drawTooltipAt(pose, fr, sg, (int) (mouseX + fr.width(sg) + 12), (int) mouseY);
            else
                this.drawTooltip(pose, fr, sg, mouseX, mouseY);
        }
        pose.translate(0.0F, 0.0F, -64.0F);
    }

    public void drawMultilineTooltip(PoseStack pose, List<String> li, int x, int y) {
        pose.translate(0.0F, 0.0F, 64.0F);
        int dy = y;
        for (String s : li) {
            this.drawTooltipAt(pose, minecraft.font, s, x, dy);
            dy += 17;
        }
        pose.translate(0.0F, 0.0F, -64.0F);
    }

    public void drawMultilineTooltip(PoseStack pose, ItemStack is, int x, int y, double mouseX, double mouseY) {
        if (this.isMouseInBox(x, x + 16, y, y + 16, mouseX, mouseY)) {
            List<String> li = new ArrayList<>();
            li.add(is.getDisplayName().getString());
            //todo is.getItem().addInformation(is, Minecraft.getInstance().player, li, true);
            is.getItem().getDescription();
            this.drawMultilineTooltip(pose, li, x, y);
        }
    }

    public void drawTooltip(PoseStack pose, Font f, String s, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) mouseX, (int) mouseY);
    }

    public void drawTooltip(PoseStack pose, Font f, String s, int dx, int dy, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) (mouseX + dx), (int) (mouseY + dy));
    }

    public void drawTooltipAt(PoseStack matrixStack, Font f, String s, int mx, int my) {
        if (s == null)
            s = "[null]";

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        int k = f.width(s); //had DelegateFontRenderer.stripFlags
        int j2 = mx + 12;
        int k2 = my - 12;
        int i1 = 8;

        if (j2 + k > width)
            j2 -= 28 + k;

        if (k2 + i1 + 6 > height)
            ;//k2 = height - i1 - 6;

        zLevel = 300.0F;
        //todo itemRenderer.zLevel = 300.0F;

        int j1 = -267386864;
        this.fillGradient(matrixStack, j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
        this.fillGradient(matrixStack, j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
        this.fillGradient(matrixStack, j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
        this.fillGradient(matrixStack, j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        this.fillGradient(matrixStack, j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
        int k1 = 1347420415;
        int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
        this.fillGradient(matrixStack, j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        this.fillGradient(matrixStack, j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
        this.fillGradient(matrixStack, j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
        this.fillGradient(matrixStack, j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);


        RenderSystem.enableTexture();
        f.drawShadow(matrixStack, s, j2, k2, 0xffffffff);

        if (cacheRenders)
            tooltips.addItem(s, mx, my + 8, f.width(s) + 24, f.lineHeight + 8);
    }

    public void drawSplitTooltipAt(PoseStack pose, Font f, List<String> li, int mx, int my) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
//        RenderSystem.glDisable(GL11.GL_LIGHTING);
        int k = -1;
        for (String s : li) {
            k = Math.max(k, f.width(s)); //DelegateFontRenderer.stripFlags
        }
        int j2 = mx + 12;
        int k2 = my - 12;
        int i1 = 8 * li.size() + (2 * li.size() - 1) - 1;

        if (j2 + k > width)
            j2 -= 28 + k;

        if (k2 + i1 + 6 > height)
            ;//k2 = height - i1 - 6;

        zLevel = 300.0F;
        //itemRender.zLevel = 300.0F;
        int j1 = -267386864;
        this.fillGradient(pose, j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
        this.fillGradient(pose, j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
        this.fillGradient(pose, j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
        this.fillGradient(pose, j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        this.fillGradient(pose, j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
        int k1 = 1347420415;
        int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
        this.fillGradient(pose, j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        this.fillGradient(pose, j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
        this.fillGradient(pose, j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
        this.fillGradient(pose, j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);

        RenderSystem.enableTexture();

        for (int i = 0; i < li.size(); i++) {
            String s = li.get(i);
            f.drawShadow(pose, s, j2, k2 + i * 10, 0xffffffff);
            if (cacheRenders)
                tooltips.addItem(s, mx, my + 8 + i * 10, f.width(s) + 24, f.lineHeight + 8);
        }

        pose.popPose();
    }

    public Map<String, Rectangle> getTooltips() {
        return tooltips.view();
    }

    public Map<Rectangle, ItemStack> getRenderedItems() {
        return items.view();
    }

    /**
     * This function is computationally expensive!
     */
    public ItemStack getItemRenderAt(int x, int y) {
        return items.getRegion(x, y);
    }

    public boolean isMouseInBox(int minX, int maxX, int minY, int maxY, double mouseX, double mouseY) {
        return mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;
    }

    public void renderStatic(PoseStack pose, int minx, int miny, int maxx, int maxy) {
        for (int i = minx; i <= maxx; i++) {
            for (int k = miny; k <= maxy; k++) {
                int br = ReikaRandomHelper.getRandomBetween(0, 255);
                int color = ReikaColorAPI.GStoHex(br);
                this.drawRect(pose, i, k, i + 1, k + 1, 0xff000000 | color, false);
            }
        }
    }

    public void drawRectFrame(PoseStack matrixStack, int minx, int miny, int w, int h, int color) {
        this.drawRectFrame(matrixStack, minx, miny, w, h, color, LineType.SOLID);
    }

    public void drawRectFrame(PoseStack matrixStack, int minx, int miny, int w, int h, int color, LineType type) {
        int maxx = minx + w;
        int maxy = miny + h;
        this.drawLine(matrixStack, minx, miny, maxx, miny, color, type);
        this.drawLine(matrixStack, minx, maxy, maxx, maxy, color, type);
        this.drawLine(matrixStack, minx, miny, minx, maxy, color, type);
        this.drawLine(matrixStack, maxx, miny, maxx, maxy, color, type);
    }

    public float getZLevel() {
        return zLevel;
    }

    public void setZLevel(float z) {
        zLevel = z;
    }
}
