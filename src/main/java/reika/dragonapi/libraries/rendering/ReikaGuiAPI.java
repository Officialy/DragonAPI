package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
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

    private float zLevel = 0;

    private ReikaGuiAPI() {
        super(Component.empty());

        NeoForge.EVENT_BUS.register(this);
    }

    public static void setup() {
        // RenderSystem state is now managed by the RenderPipeline system.
    }

    /**
     * Draws a textured quad using the modern BufferBuilder API.
     * The caller must ensure the appropriate texture is set up via the RenderPipeline.
     * Args: matrixStack, buffer, x, y, w, h, color, u0, v0, u1, v1
     */
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
        // The 5-arg text() overload defaults dropShadow=true; pass false so this actually draws
        // without a shadow (the shadow made power-tab labels / titles look doubled and darker).
        graphics.text(par1FontRenderer, par2Str, par3 - par1FontRenderer.width(par2Str) / 2, par4, par5, false);
    }

    /**
     * Renders the specified text to the screen, center-aligned, using a direct in-batch draw.
     */
    public void drawCenteredStringNoShadow(PoseStack stack, Font par1FontRenderer, String par2Str, int par3, int par4, int par5, MultiBufferSource bufferSource) {
        par1FontRenderer.drawInBatch(par2Str, par3 - par1FontRenderer.width(par2Str) / 2, par4, par5, false, stack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    /**
     * Draws a textured rectangle at the stored z-value. 26.1 port: uses the modern BufferBuilder
     * with GuiGraphicsExtractor.blit() via the provided graphics context.
     * <p>
     * The original method had no rendering context parameter; that legacy overload is preserved but
     * forwarded through a new default PoseStack.  Prefer the GuiGraphicsExtractor overload whenever
     * possible.
     * Args: x, y, u, v, width, height, texture-scale
     */
    @Deprecated
    public void drawTexturedModalRectInvert(int x, int y, int u, int v, int w, int h, int scale) {
        // 26.1: Immediate-mode texturing is removed. Use the GuiGraphicsExtractor.blit() overload
        // that takes a RenderPipeline, texture Identifier, and UV coordinates.
        // For now, allocate a fresh PoseStack and use the modern BufferBuilder API.
        // This requires the caller to have bound a texture via RenderSystem.setShaderTexture()
        // (which is also deprecated). Prefer migrating call sites to the GuiGraphicsExtractor path.
        PoseStack stack = new PoseStack();
        RenderType type = RenderTypes.debugQuads();
        var builder = Tesselator.getInstance().begin(type.mode(), type.format());
        Matrix4f mat = stack.last().pose();
        float uScale = 1.0F / scale;
        float vScale = 1.0F / scale;
        float u0 = u * uScale;
        float v0 = v * vScale;
        float u1 = (u + w) * uScale;
        float v1 = (v + h) * vScale;
        // Inverted V: v1 first, then v0
        builder.addVertex(mat, x, y + h, 0).setColor(0xFFFFFFFF).setUv(u0, v1);
        builder.addVertex(mat, x + w, y + h, 0).setColor(0xFFFFFFFF).setUv(u1, v1);
        builder.addVertex(mat, x + w, y, 0).setColor(0xFFFFFFFF).setUv(u1, v0);
        builder.addVertex(mat, x, y, 0).setColor(0xFFFFFFFF).setUv(u0, v0);
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
    }

    /**
     * Draws a solid color rectangle with the specified coordinates and color.
     * <p>
     * Parameter names reflect the original (x, y, x1, y1) semantics — despite being labelled
     * "width" and "height", callers pass x2/y2 as the third and fourth coordinate arguments.
     * <p>
     * Prefer the {@link #drawRect(GuiGraphicsExtractor, int, int, int, int, int, boolean)}
     * overload when a GuiGraphicsExtractor is available.
     */
    public void drawRect(PoseStack matrixStack, int x, int y, int x1, int y1, int color, boolean enableAlpha) {
        int c = enableAlpha ? color : (color | 0xff000000);
        int a = (c >> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        // Normalise coordinates so x <= x1 and y <= y1 for consistent quad winding.
        int ix0 = Math.min(x, x1);
        int ix1 = Math.max(x, x1);
        int iy0 = Math.min(y, y1);
        int iy1 = Math.max(y, y1);
        if (ix0 == ix1 || iy0 == iy1) return;
        RenderType type = RenderTypes.debugQuads();
        var builder = Tesselator.getInstance().begin(type.mode(), type.format());
        Matrix4f mat = matrixStack.last().pose();
        builder.addVertex(mat, ix0, iy1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, ix1, iy1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, ix1, iy0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, ix0, iy0, 0).setColor(r, g, b, a);
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
    }

    public void drawRect(GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y, int x1, int y1, int color, boolean enableAlpha) {
        int c = enableAlpha ? color : (color | 0xff000000);
        GuiGraphicsExtractor.fill(x, y, x1, y1, c);
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

    /**
     * Draws a textured rectangle using the modern BufferBuilder pipeline.
     * <p>
     * The caller must ensure the appropriate texture is active via the RenderPipeline;
     * in most cases the {@link GuiGraphicsExtractor#blit} overloads should be preferred.
     */
    public void drawTexturedRect(PoseStack matrixStack, int x, int y, int w, int h, int color, float u0, float v0, float u1, float v1) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        RenderType type = RenderTypes.debugQuads();
        var builder = Tesselator.getInstance().begin(type.mode(), type.format());
        Matrix4f mat = matrixStack.last().pose();
        builder.addVertex(mat, x, y + h, 0).setColor(r, g, b, a).setUv(u0, v1);
        builder.addVertex(mat, x + w, y + h, 0).setColor(r, g, b, a).setUv(u1, v1);
        builder.addVertex(mat, x + w, y, 0).setColor(r, g, b, a).setUv(u1, v0);
        builder.addVertex(mat, x, y, 0).setColor(r, g, b, a).setUv(u0, v0);
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
    }

    /**
     * Draws a dotted line between two points using Bresenham's algorithm.
     * Args: poseStack, start x,y, end x,y, spacing, color
     */
    public void dottedLine(PoseStack poseStack, int x, int y, int x2, int y2, int t, int color) {
        if (x == x2 && y == y2)
            return;

        int dx = Math.abs(x2 - x);
        int dy = Math.abs(y2 - y);
        int sx = x < x2 ? 1 : -1;
        int sy = y < y2 ? 1 : -1;
        int err = dx - dy;
        int step = 0;
        int cx = x, cy = y;

        while (true) {
            if (step % t == 0) {
                this.drawRect(poseStack, cx, cy, cx + 1, cy + 1, color, false);
            }
            if (cx == x2 && cy == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 <  dx) { err += dx; cy += sy; }
            step++;
        }
    }

    /**
     * Draws a line between two points. Args: Start x,y, end x,y, color
     */
    public void drawLine(PoseStack matrixStack, int x, int y, int x2, int y2, int color) {
        this.drawLine(matrixStack, x, y, x2, y2, color, LineType.SOLID);
    }

    /**
     * 26.1: PoseStack-backed line drawing. Uses the BufferBuilder with LINES render type.
     * For solid lines the segment is drawn directly; for dashed/dotted patterns a Bresenham
     * walk emits individual 1x1 quads via the modern BufferBuilder API.
     */
    public void drawLine(PoseStack matrixStack, int x, int y, int x2, int y2, int color, LineType type) {
        int col = color | (((color >>> 24) == 0) ? 0xff000000 : 0);
        int a = (col >> 24) & 0xFF;
        int r = (col >> 16) & 0xFF;
        int g = (col >> 8) & 0xFF;
        int b = col & 0xFF;

        if (x == x2 && y == y2) {
            // Single pixel
            RenderType rt = RenderTypes.debugQuads();
            var builder = Tesselator.getInstance().begin(rt.mode(), rt.format());
            Matrix4f mat = matrixStack.last().pose();
            builder.addVertex(mat, x, y, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x + 1, y, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x + 1, y + 1, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x, y + 1, 0).setColor(r, g, b, a);
            MeshData mesh = builder.buildOrThrow();
            rt.draw(mesh);
            return;
        }

        // Axis-aligned lines: single quad
        if (y == y2) {
            int xa = Math.min(x, x2), xb = Math.max(x, x2);
            RenderType rt = RenderTypes.debugQuads();
            var builder = Tesselator.getInstance().begin(rt.mode(), rt.format());
            Matrix4f mat = matrixStack.last().pose();
            builder.addVertex(mat, xa, y, 0).setColor(r, g, b, a);
            builder.addVertex(mat, xb + 1, y, 0).setColor(r, g, b, a);
            builder.addVertex(mat, xb + 1, y + 1, 0).setColor(r, g, b, a);
            builder.addVertex(mat, xa, y + 1, 0).setColor(r, g, b, a);
            MeshData mesh = builder.buildOrThrow();
            rt.draw(mesh);
            return;
        }
        if (x == x2) {
            int ya = Math.min(y, y2), yb = Math.max(y, y2);
            RenderType rt = RenderTypes.debugQuads();
            var builder = Tesselator.getInstance().begin(rt.mode(), rt.format());
            Matrix4f mat = matrixStack.last().pose();
            builder.addVertex(mat, x, ya, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x + 1, ya, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x + 1, yb + 1, 0).setColor(r, g, b, a);
            builder.addVertex(mat, x, yb + 1, 0).setColor(r, g, b, a);
            MeshData mesh = builder.buildOrThrow();
            rt.draw(mesh);
            return;
        }

        // Bresenham diagonal — emit individual 1x1 quads batched into a single BufferBuilder.
        int dx = Math.abs(x2 - x), dy = Math.abs(y2 - y);
        int sx = x < x2 ? 1 : -1, sy = y < y2 ? 1 : -1;
        int err = dx - dy;
        int step = 0;
        int dashLen = type == LineType.DASHED ? 4 : (type == LineType.DOTTED ? 2 : Integer.MAX_VALUE);
        int gapLen  = type == LineType.DASHED ? 3 : (type == LineType.DOTTED ? 2 : 0);

        RenderType rt = RenderTypes.debugQuads();
        var bld = Tesselator.getInstance().begin(rt.mode(), rt.format());
        Matrix4f mat = matrixStack.last().pose();

        while (true) {
            int phase = step % (dashLen + gapLen);
            if (phase < dashLen) {
                bld.addVertex(mat, x, y, 0).setColor(r, g, b, a);
                bld.addVertex(mat, x + 1, y, 0).setColor(r, g, b, a);
                bld.addVertex(mat, x + 1, y + 1, 0).setColor(r, g, b, a);
                bld.addVertex(mat, x, y + 1, 0).setColor(r, g, b, a);
            }
            if (x == x2 && y == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
            step++;
        }

        MeshData mesh = bld.buildOrThrow();
        rt.draw(mesh);
    }

    /**
     * 26.1 port: draws a circle outline using the modern BufferBuilder (LINES mode).
     * Args: center x, y, radius, RGBA color
     */
    public void drawCircle(double x, double y, double radius, int color) {
        int alpha = ReikaColorAPI.getAlpha(color);
        if (alpha == 0)
            alpha = 255;
        int red = ReikaColorAPI.getRed(color);
        int green = ReikaColorAPI.getGreen(color);
        int blue = ReikaColorAPI.getBlue(color);

        RenderType type = RenderTypes.lines();
        var builder = Tesselator.getInstance().begin(type.mode(), type.format());
        PoseStack stack = new PoseStack();
        Matrix4f mat = stack.last().pose();
        // Emit line segments (pairs of vertices) around the circle.
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.toRadians(i * (360.0 / segments));
            double a2 = Math.toRadians((i + 1) * (360.0 / segments));
            builder.addVertex(mat, (float) (x + radius * Math.cos(a1)), (float) (y + radius * Math.sin(a1)), 0)
                    .setColor(red, green, blue, alpha);
            builder.addVertex(mat, (float) (x + radius * Math.cos(a2)), (float) (y + radius * Math.sin(a2)), 0)
                    .setColor(red, green, blue, alpha);
        }
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
    }

    /**
     * Draws a "fill bar" (rectangle from bottom up). 26.1 port: uses the BufferBuilder approach.
     * Args: left x, top y, width, bottom y, color, height, maxheight, alpha on/off
     */
    public void fillBar(PoseStack matrixStack, int x, int y, int w, int bottom, int c, int height, int maxHeight, boolean alpha) {
        if (maxHeight <= 0) return;
        int barH = (int) (((double) height / (double) maxHeight) * (bottom - y));
        if (barH <= 0) return;
        int topY = bottom - barH;
        int col = alpha ? c : (c | 0xff000000);
        this.drawRect(matrixStack, x, topY, x + w, bottom, col, false);
    }


    public void drawItemStack(GuiGraphicsExtractor GuiGraphicsExtractor, Font fr, ItemStack is, int x, int y) {
        if (is == null || is.isEmpty())
            return;

        GuiGraphicsExtractor.item(is, x, y);
        GuiGraphicsExtractor.itemDecorations(fr, is, x, y);

        if (cacheRenders)
            items.addRegionByWH(x, y, 16, 16, is.copy());
    }

    public void drawCustomRecipeList(GuiGraphicsExtractor render, Font f, List<Recipe<?>> lr, int x, int y, int x2, int y2) {
        if (lr == null || lr.size() <= 0) {
            return;
        }
        int k = ((int)(System.nanoTime()/2000000000))%lr.size();
        Object ir = lr.get(k);
        Recipe<?> ire = /*ir instanceof WrappedRecipe ? ((WrappedRecipe)ir).getRecipe() :*/ (Recipe<?>)ir;
        ItemStack isout = ReikaRecipeHelper.getRecipeOutput(ire);
        ItemStack[] in = ReikaRecipeHelper.getPermutedRecipeArray(ire);
        if (in == null)
            return;
        boolean noshape = false;
        if (ire instanceof ShapelessRecipe)
            noshape = true;
        this.drawRecipe(render, f, x, y, in, x2, y2, isout, noshape);
    }

    /**
     * Draws a random recipe from the given list of recipes, using the given output items.
     * Args: render, font renderer, output items, recipe list, x in, y in, x out, y out
     */
    public void drawCustomRecipes(GuiGraphicsExtractor render, Font f, List<ItemStack> out, Collection<Recipe<?>> ir, int x, int y, int x2, int y2) {
         ArrayList<Recipe<?>> lr = new ArrayList<Recipe<?>>();
         for (ItemStack is : out) {
             lr.addAll(ReikaRecipeHelper.getAllRecipesByOutput(ir, is));
         }
         if (lr.size() <= 0) {
             return;
         }
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
    private void drawRecipe(GuiGraphicsExtractor render, Font f, int x, int y, ItemStack[] in, int x2, int y2, ItemStack out, boolean shapeless) {
        if (in.length != 9)
            throw new MisuseException("DrawRecipe() requires 9 input items!");
        int j = this.getScreenXInset();
        int k = this.getScreenYInset();
        for (int ii = 0; ii < 3; ii++) {
            for (int jj = 0; jj < 3; jj++) {
                if (in[ii*3+jj] != null) {
                    in[ii*3+jj].setCount(1);
                    this.drawItemStackWithTooltip(render, f, in[ii*3+jj], x+j+18*jj, y+k+18*ii);
                }
            }
        }
        if (out != null)
            this.drawItemStackWithTooltip(render, f, out, x2+4+j, y2+4+k);
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
            this.drawMultilineTooltip(stack, graphics, li, x, y);
        }
    }

    public void drawTooltip(GuiGraphicsExtractor pose, Font f, String s, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) mouseX, (int) mouseY);
    }

    public void drawTooltip(GuiGraphicsExtractor pose, Font f, String s, int dx, int dy, double mouseX, double mouseY) {
        this.drawTooltipAt(pose, f, s, (int) (mouseX + dx), (int) (mouseY + dy));
    }

    /**
     * 26.1 port: draws a single-line tooltip background using modern GuiGraphicsExtractor.fill()/fillGradient()
     * and renders the text via drawStringShadow.
     */
    public void drawTooltipAt(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, String s, int mx, int my) {
        if (s == null)
            s = "[null]";

        int k = f.width(s);
        int j2 = mx + 12;
        int k2 = my - 12;
        int i1 = 8;

        if (j2 + k > width)
            j2 -= 28 + k;

        if (k2 + i1 + 6 > height)
            ;

        zLevel = 300.0F;

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

        drawStringShadow(GuiGraphicsExtractor, f, s, j2, k2, 0xffffffff);

        if (cacheRenders)
            tooltips.addItem(s, mx, my + 8, f.width(s) + 24, f.lineHeight + 8);
    }

    public void drawSplitTooltipAt(GuiGraphicsExtractor GuiGraphicsExtractor, Font f, List<String> li, int mx, int my) {
        int k = -1;
        for (String s : li) {
            k = Math.max(k, f.width(s));
        }
        int j2 = mx + 12;
        int k2 = my - 12;
        int i1 = 8 * li.size() + (2 * li.size() - 1) - 1;

        if (j2 + k > width)
            j2 -= 28 + k;

        if (k2 + i1 + 6 > height)
            ;

        zLevel = 300.0F;

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

        for (int i = 0; i < li.size(); i++) {
            String s = li.get(i);
            drawStringShadow(GuiGraphicsExtractor, f, s, j2, k2 + i * 10, 0xffffffff);
            if (cacheRenders)
                tooltips.addItem(s, mx, my + 8 + i * 10, f.width(s) + 24, f.lineHeight + 8);
        }
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
