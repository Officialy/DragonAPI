package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.ModList;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.maps.RectangleMap;
import reika.dragonapi.instantiable.data.maps.RegionMap;
import reika.dragonapi.libraries.ReikaRecipeHelper;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.objects.LineType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// 1.21.5: instance handlers; registered via NeoForge.EVENT_BUS.register(this) in the constructor.
// Do NOT add @EventBusSubscriber — it requires static @SubscribeEvent methods, and eager class load
// during mod construction would trigger Screen.<init> NPE (Minecraft.getInstance() is still null).
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
        super(Component.empty());
        
        NeoForge.EVENT_BUS.register(this);
    }

    public static void setup() {
        // RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        // RenderSystem.enableBlend();
        // RenderSystem.defaultBlendFunc();
        // RenderSystem.blendFunc(770, 771);
        // RenderSystem.enableDepthTest();
        // RenderSystem.enableBlend();
    }

    public static void drawRectWithUV(PoseStack matrixStack, BufferBuilder buffer, int x, int y, int w, int h, int col, float u0, float v0, float u1, float v1) {
        if (w <= 0 || h <= 0) {
            return;
        }

        PoseStack.Pose pose = matrixStack.last();
        buffer.addVertex(pose, x, y + h, 0).setColor(col).setUv(u0, v1);
        buffer.addVertex(pose, x + w, y + h, 0).setColor(col).setUv(u1, v1);
        buffer.addVertex(pose, x + w, y, 0).setColor(col).setUv(u1, v0);
        buffer.addVertex(pose, x, y, 0).setColor(col).setUv(u0, v0);
    }

    public int getScreenXInset() {
        return (width - xSize) / 2;
    }

    public int getScreenYInset() {
        return (height - ySize) / 2 - 8;
    }

    // 1.21.5/NeoForge 26.x: cannot register listeners on the abstract ScreenEvent base — must
    // use a concrete subclass. ScreenEvent.Render.Pre fires once per frame before the screen's
    // render() call, which matches the legacy "clear per-frame caches" intent.
    @SubscribeEvent
    public void preDrawScreen(ScreenEvent.Render.Pre evt) {
        if (cacheRenders) {
            tooltips.clear();
            items.clear();
        }
    }

    /**
     * Renders the specified text to the screen, center-aligned.
     */
    public void drawCenteredStringNoShadow(GuiGraphicsExtractor graphics, Font par1FontRenderer, String par2Str, int par3, int par4, int par5) {
        graphics.text(par1FontRenderer, par2Str, par3 - par1FontRenderer.width(par2Str) / 2, par4, par5);
    }

    /**
     * Renders the specified text to the screen, center-aligned.
     */
    public void drawCenteredStringNoShadow(PoseStack stack, Font par1FontRenderer, String par2Str, int par3, int par4, int par5, MultiBufferSource bufferSource) {
        par1FontRenderer.drawInBatch(par2Str, par3 - par1FontRenderer.width(par2Str) / 2, par4, par5, false, stack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    /**
     * Draws a textured rectangle at the stored z-value. Args: x, y, u, v, width, height
     */
    public void drawTexturedModalRectInvert(int x, int y, int u, int v, int w, int h, int scale) {
        // TODO: Port to 26.1 rendering API (Tesselator.getBuilder() + begin() + vertex() + end() removed)
    }

    /**
     * Draws a solid color rectangle with the specified coordinates and color. Modified and simplified from the original
     * in that it automatically handles alpha channel (makes completely transparent full opaque) and changes 4-point method
     * to x-y-width-height. Args: x, y-topleft, width, height, color, alpha on/off
     */
    public void drawRect(PoseStack matrixStack, int x, int y, int width, int height, int color, boolean enableAlpha) {
        // TODO: Port to 26.1 rendering API (Tesselator.getBuilder() + begin() + vertex().endVertex() + end() removed)
        // Use GuiGraphicsExtractor overload instead when available
    }

    public void drawRect(GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y, int width, int height, int color, boolean enableAlpha) {
        int c = enableAlpha ? color : (color | 0xff000000);
        GuiGraphicsExtractor.fill(x, y, width, height, c);
    }

    /**
     * 26.1: fill-bar overload that takes the new {@link GuiGraphicsExtractor} draw target.
     * Routes through {@link GuiGraphicsExtractor#fill} for the body and (optionally) draws a
     * 1-px border around it. Matches the legacy {@code fillBar(PoseStack, ...)} parameter
     * order so call-sites can be migrated by swapping the first argument.
     *
     * <p>Args: graphics, left x, top y, width, bottom y (NOT height), colour, current
     * fill-height, max fill-height, alpha-enabled.
     */
    public void fillBar(GuiGraphicsExtractor graphics, int x, int y, int w, int bottom, int c, int height, int maxHeight, boolean alpha) {
        if (maxHeight <= 0) return;
        int barH = (int) (((double) height / (double) maxHeight) * (bottom - y));
        if (barH <= 0) return;
        int topY = bottom - barH;
        int col = alpha ? c : (c | 0xff000000);
        graphics.fill(x, topY, x + w, bottom, col);
    }

    /**
     * 26.1: line overload that takes the new {@link GuiGraphicsExtractor} draw target.
     * Implemented as a chain of 1-px {@link GuiGraphicsExtractor#fill fill} rectangles —
     * fully axis-aligned lines are crisp, diagonals approximate via Bresenham. {@link LineType}
     * is honoured for SOLID/DASHED/DOTTED.
     */
    public void drawLine(GuiGraphicsExtractor graphics, int x, int y, int x2, int y2, int color, LineType type) {
        int col = color | (((color >>> 24) == 0) ? 0xff000000 : 0);
        if (x == x2 && y == y2) {
            graphics.fill(x, y, x + 1, y + 1, col);
            return;
        }
        // Pure horizontal / vertical: one fill call.
        if (y == y2) {
            int xa = Math.min(x, x2), xb = Math.max(x, x2);
            graphics.fill(xa, y, xb + 1, y + 1, col);
            return;
        }
        if (x == x2) {
            int ya = Math.min(y, y2), yb = Math.max(y, y2);
            graphics.fill(x, ya, x + 1, yb + 1, col);
            return;
        }
        // Bresenham diagonal — emit 1-px pixels via fill. Respect LineType by skipping pixels.
        int dx = Math.abs(x2 - x), dy = Math.abs(y2 - y);
        int sx = x < x2 ? 1 : -1, sy = y < y2 ? 1 : -1;
        int err = dx - dy;
        int step = 0;
        int dashLen = type == LineType.DASHED ? 4 : (type == LineType.DOTTED ? 2 : Integer.MAX_VALUE);
        int gapLen  = type == LineType.DASHED ? 3 : (type == LineType.DOTTED ? 2 : 0);
        while (true) {
            int phase = step % (dashLen + gapLen);
            if (phase < dashLen)
                graphics.fill(x, y, x + 1, y + 1, col);
            if (x == x2 && y == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
            step++;
        }
    }

    public void drawLine(GuiGraphicsExtractor graphics, int x, int y, int x2, int y2, int color) {
        this.drawLine(graphics, x, y, x2, y2, color, LineType.SOLID);
    }

    public void drawTexturedRect(PoseStack matrixStack, int x, int y, int w, int h, int color, float u0, float v0, float u1, float v1) {
        // TODO: Port to 26.1 BufferBuilder API (setShader + Tesselator.getBuilder() + begin() + end() all removed)
        // New pattern: new BufferBuilder(ByteBufferBuilder, Mode, Format) + addVertex() + buildOrThrow() + RenderType.draw()
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
        // TODO: Port to 26.1 rendering API (setShader + Tesselator.getBuilder() + begin() + end() all removed)
        // New pattern: new BufferBuilder(ByteBufferBuilder, LINES, POSITION_COLOR_NORMAL_LINE_WIDTH) + addVertex() + buildOrThrow() + RenderType.draw()
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
        // RenderSystem.enableBlend();
//        RenderSystem.disableTexture();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        // RenderSystem.setShaderColor(red / 255F, green / 255F, blue / 255F, alpha / 255F);
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
        // TODO: Port to 26.1 rendering API (Tesselator.getBuilder() + begin() + vertex().endVertex() + end() removed)
    }

    @SuppressWarnings("unused")
    private void fillBar_legacy(PoseStack matrixStack, int x, int y, int w, int bottom, int c, int height, int maxHeight, boolean alpha) {
        // Old Tesselator API removed in 26.1
    }

    public void drawItemStack(GuiGraphicsExtractor GuiGraphicsExtractor, Font fr, ItemStack is, int x, int y) { // Removed GuiGraphicsExtractor renderer
        if (is == null || is.isEmpty())
            return;

        GuiGraphicsExtractor.item(is, x, y);
        GuiGraphicsExtractor.itemDecorations(fr, is, x, y);

        if (cacheRenders)
            items.addRegionByWH(x, y, 16, 16, is.copy());
    }

    public void drawCustomRecipeList(GuiGraphicsExtractor render, Font f, List<Recipe<?>> lr, int x, int y, int x2, int y2) {
	    if (lr == null || lr.size() <= 0) {
	    	//ReikaJavaLibrary.pConsole("No recipes found for "+out);
	    	return;
	    }
	    //ReikaJavaLibrary.pConsole(lr.get(0).getRecipeOutput().toString());
	    int k = ((int)(System.nanoTime()/2000000000))%lr.size();
	    //ReikaJavaLibrary.pConsole(k);
	    Object ir = lr.get(k);
	    Recipe<?> ire = /*ir instanceof WrappedRecipe ? ((WrappedRecipe)ir).getRecipe() :*/ (Recipe<?>)ir;
	    ItemStack isout = ReikaRecipeHelper.getRecipeOutput(ire);
	    ItemStack[] in = ReikaRecipeHelper.getPermutedRecipeArray(ire);
	    if (in == null)
	    	return;
	    //ReikaJavaLibrary.pConsole(Arrays.toString(in)+" to "+isout);
	    boolean noshape = false;
	    if (ire instanceof ShapelessRecipe)
	    	noshape = true;
	    this.drawRecipe(render, f, x, y, in, x2, y2, isout, noshape);
	}

    /**
     * Draws a random recipe from the given list of recipes, using the given output items.
     * Args: render, font renderer, output items, recipe list, x in, y in, x out, y out
     */
    public void drawCustomRecipes(GuiGraphicsExtractor render, Font f, List<ItemStack> out, Collection<Recipe<?>> ir, int x, int y, int x2, int y2) { // Changed List to Collection, removed mouseX, mouseY
         ArrayList<Recipe<?>> lr = new ArrayList<Recipe<?>>();
		 for (ItemStack is : out) {
		 	lr.addAll(ReikaRecipeHelper.getAllRecipesByOutput(ir, is));
		 }
		 if (lr.size() <= 0) {
		 	//ReikaJavaLibrary.pConsole("No recipes found for "+out);
		 	return;
		 }
		 //ReikaJavaLibrary.pConsole(lr.get(13).getRecipeOutput());
		 Recipe ire = lr.get(((int)(System.nanoTime()/2000000000))%lr.size());
		 ItemStack isout = ReikaRecipeHelper.getRecipeOutput(ire);
		 ItemStack[] in = ReikaRecipeHelper.getPermutedRecipeArray(ire);
		 if (in == null)
		 	return;
		 boolean noshape = false;
		 if (ire instanceof ShapelessRecipe)
		 	noshape = true;
		 this.drawRecipe(render, f, x, y, in, x2, y2, isout, noshape);
    }

    /** Draw a crafting recipe in the GUI. Args: x in, y in; items of: top-left, top, top-right, left,
     * center, right, bottom-left, bottom, bottom right; x out, y out; output item, shapeless t/f.
     * Input items MUST be a size-9 array! */
    private void drawRecipe(GuiGraphicsExtractor render, Font f, int x, int y, ItemStack[] in, int x2, int y2, ItemStack out, boolean shapeless) { // Removed mouseX, mouseY
        if (in.length != 9)
            throw new MisuseException("DrawRecipe() requires 9 input items!");
        int j = this.getScreenXInset();
        int k = this.getScreenYInset();
        for (int ii = 0; ii < 3; ii++) {
            for (int jj = 0; jj < 3; jj++) {
                if (in[ii*3+jj] != null) {
                    in[ii*3+jj].setCount(1);
                    this.drawItemStackWithTooltip(render, f, in[ii*3+jj], x+j+18*jj, y+k+18*ii); // Removed mouseX, mouseY
                }
            }
        }
        if (out != null)
            this.drawItemStackWithTooltip(render, f, out, x2+4+j, y2+4+k); // Removed mouseX, mouseY
        if (shapeless)
            render.text(f, "Shapeless", x2+j-35, y2+k+27, 0x000000);
    }

    /** Draw a smelting recipe in the GUI. Args: output item, x in, y in, x out, y out */
    public void drawSmelting(GuiGraphicsExtractor render, Font f, ItemStack out, int x, int y, int x2, int y2) {
        int j = this.getScreenXInset();
        int k = this.getScreenYInset();

        ItemStack in = ReikaRecipeHelper.getFurnaceInput(out);

        if (in != null)
            this.drawItemStackWithTooltip(render, f, in, x+j, y+k);
        if (out != null)
            this.drawItemStackWithTooltip(render, f, out, x2+4+j, y2+4+k);
    }
 /*
 public void drawItemStackWithTooltip(GuiGraphicsExtractor renderer, ItemStack is, int x, int y) {
  this.drawItemStackWithTooltip(renderer, Minecraft.getMinecraft().fontRenderer, is, x, y);
 }
  */
    public void drawItemStackWithTooltip(GuiGraphicsExtractor GuiGraphicsExtractor, Font fr, ItemStack is, int x, int y) {
        this.drawItemStack(GuiGraphicsExtractor, fr, is, x, y);
        if (cacheRenders) {
            items.addRegionByWH(x, y, 16, 16, is.copy());
        }
    }

    public void drawItemStackWithTooltip(GuiGraphicsExtractor GuiGraphicsExtractor, Font fr, ItemStack is, int x, int y, double mouseX, double mouseY) {
        if (is == null || is.isEmpty())
            return;

        this.drawItemStack(GuiGraphicsExtractor, fr, is, x, y);

        if (this.isMouseInBox(x, x + 16, y, y + 16, mouseX, mouseY)) {
            GuiGraphicsExtractor.setTooltipForNextFrame(fr, is, (int)mouseX, (int)mouseY);
        }
    }

    public void drawMultilineTooltip(PoseStack stack, GuiGraphicsExtractor GuiGraphicsExtractor, List<String> li, int x, int y) {
        // In 1.21, pose is 2D; use zLevel tracking to control draw order rather than Z-translate on pose
        int dy = y;
        for (String s : li) {
            this.drawTooltipAt(GuiGraphicsExtractor, minecraft.font, s, x, dy);
            dy += 17;
        }
    }

    public void drawMultilineTooltip(PoseStack stack, GuiGraphicsExtractor graphics, ItemStack is, int x, int y, double mouseX, double mouseY) {
        if (this.isMouseInBox(x, x + 16, y, y + 16, mouseX, mouseY)) {
            List<String> li = new ArrayList<>();
            li.add(is.getDisplayName().getString());
            //todo is.getItem().addInformation(is, Minecraft.getInstance().player, li, true);
            // is.getItem().getDescription();
            this.drawMultilineTooltip(stack, graphics, li, x, y);
        }
    }

    public void drawTooltip(GuiGraphicsExtractor pose, Font f, String s, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) mouseX, (int) mouseY);
    }

    public void drawTooltip(GuiGraphicsExtractor pose, Font f, String s, int dx, int dy, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) (mouseX + dx), (int) (mouseY + dy));
    }

    public void drawTooltipAt(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, String s, int mx, int my) {
        if (s == null)
            s = "[null]";

        // RenderSystem.disableDepthTest();
        // RenderSystem.disableBlend();
//        RenderSystem.disableTexture();
        // RenderSystem.disableBlend();
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
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
        int k1 = 1347420415;
        int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        GuiGraphicsExtractor.fillGradient(j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);


//        RenderSystem.enableTexture();
        drawStringShadow(GuiGraphicsExtractor, f, s, j2, k2, 0xffffffff);

        if (cacheRenders)
            tooltips.addItem(s, mx, my + 8, f.width(s) + 24, f.lineHeight + 8);
    }

    public void drawSplitTooltipAt(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, List<String> li, int mx, int my) {
//        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // RenderSystem.disableDepthTest();
//        RenderSystem.disableTexture();
        // RenderSystem.disableBlend();
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
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
        GuiGraphicsExtractor.fillGradient(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
        int k1 = 1347420415;
        int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
        GuiGraphicsExtractor.fillGradient(j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
        GuiGraphicsExtractor.fillGradient(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);

//        RenderSystem.enableTexture();

        for (int i = 0; i < li.size(); i++) {
            String s = li.get(i);
            drawStringShadow(GuiGraphicsExtractor, f, s, j2, k2 + i * 10, 0xffffffff);
            if (cacheRenders)
                tooltips.addItem(s, mx, my + 8 + i * 10, f.width(s) + 24, f.lineHeight + 8);
        }

        GuiGraphicsExtractor.pose().popMatrix();
    }

    public void drawStringShadow(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, String s, int x, int y, int colour) {
        drawString(GuiGraphicsExtractor, f, s, x, y, colour, true);
    }

    public void drawString(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, String s, int x, int y, int colour) {
        drawString(GuiGraphicsExtractor, f, s, x, y, colour, false);
    }

    public void drawString(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, String s, int x, int y, int colour, boolean shadow) {
        GuiGraphicsExtractor.text(f, s, x, y, colour, shadow);
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





