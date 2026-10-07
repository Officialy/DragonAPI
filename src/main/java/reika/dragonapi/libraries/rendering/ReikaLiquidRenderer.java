/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import reika.dragonapi.instantiable.rendering.GuiShapes;

/**
 * The GUI half of V33a's ReikaLiquidRenderer: a fluid's still icon and colour, and the
 * {@code drawTexturedModelRectFromIcon} tank fill every machine screen built on them.
 */
public final class ReikaLiquidRenderer {

    private ReikaLiquidRenderer() {}

    /** The fluid's still texture; water's for a null or empty fluid, as the 1.7.10 "safe" lookup fell back. */
    public static TextureAtlasSprite getFluidIconSafe(Fluid f) {
        if (f == null || f == Fluids.EMPTY)
            f = Fluids.WATER;
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState())
                .stillMaterial().sprite();
    }

    /**
     * The fluid's colour, opaque. 1.7.10 icons carried their colour; 26.3 tints the grey still texture
     * (water, for one) through the model's tint source, using the local biome when a world is loaded.
     */
    public static int getFluidColor(Fluid f) {
        if (f == null || f == Fluids.EMPTY)
            return 0xffffffff;
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState());
        var tint = model.fluidTintSource();
        if (tint == null)
            return 0xffffffff;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null)
            return 0xff000000 | tint.colorInWorld(f.defaultFluidState(), f.defaultFluidState().createLegacyBlock(),
                    mc.level, mc.player.blockPosition());
        return 0xff000000 | tint.color(f.defaultFluidState());
    }

    /**
     * V33a {@code drawTexturedModelRectFromIcon(x, y, getFluidIconSafe(f), w, h)}: the whole icon stretched
     * over the rectangle, as a tank's fill level was drawn.
     */
    public static void drawFluidIcon(GuiGraphicsExtractor g, Fluid f, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0)
            return;
        GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, getFluidIconSafe(f), x, y, x + w, y + h, getFluidColor(f));
    }
}
