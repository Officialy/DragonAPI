package reika.dragonapi.libraries.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
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

// 26.2: all GUI drawing goes through the vanilla GuiGraphicsExtractor (fill/blit/text). Tesselator
// and MultiBufferSource were removed, so the legacy PoseStack/immediate-mode overloads are gone;
// every helper now takes a GuiGraphicsExtractor.
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
     * Draws a solid color rectangle. Coordinates are (x0,y0)-(x1,y1).
     */
    public void drawRect(GuiGraphicsExtractor graphics, int x, int y, int x1, int y1, int color, boolean enableAlpha) {
        int c = enableAlpha ? color : (color | 0xff000000);
        graphics.fill(x, y, x1, y1, c);
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
     * 26.2 port: draws a circle outline as a fan of {@link #drawLine} segments (GuiGraphicsExtractor
     * has no line/circle primitive, so the outline is approximated by short straight segments).
     * Args: graphics, center x, y, radius, RGBA color
     */
    public void drawCircle(GuiGraphicsExtractor graphics, double x, double y, double radius, int color) {
        int col = color | (((color >>> 24) == 0) ? 0xff000000 : 0);
        int segments = 72;
        double prevX = x + radius, prevY = y;
        for (int i = 1; i <= segments; i++) {
            double ang = Math.toRadians(i * (360.0 / segments));
            double nx = x + radius * Math.cos(ang);
            double ny = y + radius * Math.sin(ang);
            this.drawLine(graphics, (int) Math.round(prevX), (int) Math.round(prevY), (int) Math.round(nx), (int) Math.round(ny), col);
            prevX = nx;
            prevY = ny;
        }
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

    public void renderStatic(GuiGraphicsExtractor graphics, int minx, int miny, int maxx, int maxy) {
        for (int i = minx; i <= maxx; i++) {
            for (int k = miny; k <= maxy; k++) {
                int br = ReikaRandomHelper.getRandomBetween(0, 255);
                int color = ReikaColorAPI.GStoHex(br);
                this.drawRect(graphics, i, k, i + 1, k + 1, 0xff000000 | color, false);
            }
        }
    }

    public void drawRectFrame(GuiGraphicsExtractor graphics, int minx, int miny, int w, int h, int color) {
        this.drawRectFrame(graphics, minx, miny, w, h, color, LineType.SOLID);
    }

    public void drawRectFrame(GuiGraphicsExtractor graphics, int minx, int miny, int w, int h, int color, LineType type) {
        int maxx = minx + w;
        int maxy = miny + h;
        this.drawLine(graphics, minx, miny, maxx, miny, color, type);
        this.drawLine(graphics, minx, maxy, maxx, maxy, color, type);
        this.drawLine(graphics, minx, miny, minx, maxy, color, type);
        this.drawLine(graphics, maxx, miny, maxx, maxy, color, type);
    }

    public float getZLevel() {
        return zLevel;
    }

    public void setZLevel(float z) {
        zLevel = z;
    }
}
