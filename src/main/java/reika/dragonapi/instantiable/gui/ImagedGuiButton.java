/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Faithful 26.1 port of the legacy 1.7.10 {@code ImagedGuiButton}.
 * <p>
 * The legacy class extended {@code GuiButton} and rendered a region of a texture
 * (the "buttons.png" sheet) instead of the vanilla button background, optionally
 * drawing a text overlay or a hover tooltip. Hover state was used to modify the
 * texture UV (so subclasses could swap to a "lit" variant) and to drive a fade
 * value ({@link #getHoverFade()}).
 * <p>
 * In 26.1 the render pipeline changed completely: widgets no longer draw directly
 * with GL but instead emit blits/text into a {@link GuiGraphicsExtractor} via
 * {@code extractContents}. Click handling is delegated to the vanilla
 * {@link Button.OnPress} callback rather than a screen-wide {@code actionPerformed}.
 * This port preserves the public surface (multiple constructors, hover-UV hooks,
 * {@link TextAlign}, {@code textureSize}, {@code invisible}, {@code icon}) while
 * translating the rendering to the new pipeline.
 */
public class ImagedGuiButton extends Button {

    private final int baseU;
    private final int baseV;
    private final int color;
    private final boolean shadow;
    private final Identifier texture;
    protected final boolean hasToolTip;

    /** The legacy numeric button id; preserved for callers that branch on it. */
    public final int id;

    protected int u;
    protected int v;

    /** Optional text overlay (drawn on top of the image when {@link #hasToolTip} is false). */
    private final String overlay;

    public TextAlign alignment = TextAlign.CENTER;
    public int textOffset = 0;
    public Font renderer = Minecraft.getInstance().font;

    public int textureSize = 256;

    public boolean invisible = false;

    private boolean lastHover;
    protected int hoverTicks;
    private float hoverFade;
    public float hoverFadeSpeedUp = 0.08F;
    public float hoverFadeSpeedDown = 0.15F;
    private int ticks = 0;

    /** Optional atlas sprite drawn centred over the button (legacy {@code IIcon}). */
    public TextureAtlasSprite icon = null;
    public int iconWidth;
    public int iconHeight;

    /**
     * Draw a Gui Button with an image background.
     * Args: id, x, y, width, height, u, v, texture, onPress
     */
    public ImagedGuiButton(int id, int x, int y, int width, int height, int u, int v, Identifier texture, Button.OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.id = id;
        this.u = u;
        this.v = v;
        this.baseU = u;
        this.baseV = v;
        this.texture = texture;
        this.color = 0xffffff;
        this.shadow = false;
        this.overlay = null;
        this.hasToolTip = false;
        this.iconWidth = width;
        this.iconHeight = height;
    }

    /**
     * Draw a Gui Button with an image background and text overlay.
     * Args: id, x, y, width, height, u, v, text overlay, text color, shadow, texture, onPress
     */
    public ImagedGuiButton(int id, int x, int y, int width, int height, int u, int v, String overlay, int color, boolean shadow, Identifier texture, Button.OnPress onPress) {
        super(x, y, width, height, overlay == null ? Component.empty() : Component.literal(overlay), onPress, DEFAULT_NARRATION);
        this.id = id;
        this.u = u;
        this.v = v;
        this.baseU = u;
        this.baseV = v;
        this.color = color;
        this.shadow = shadow;
        this.texture = texture;
        this.overlay = overlay;
        this.hasToolTip = false;
        this.iconWidth = width;
        this.iconHeight = height;
    }

    /**
     * Draw a Gui Button with an image background and text tooltip.
     * Args: id, x, y, width, height, u, v, texture, text tooltip, text color, shadow, onPress
     */
    public ImagedGuiButton(int id, int x, int y, int width, int height, int u, int v, Identifier texture, String tooltip, int color, boolean shadow, Button.OnPress onPress) {
        super(x, y, width, height, tooltip == null ? Component.empty() : Component.literal(tooltip), onPress, DEFAULT_NARRATION);
        this.id = id;
        this.u = u;
        this.v = v;
        this.baseU = u;
        this.baseV = v;
        this.color = color;
        this.shadow = shadow;
        this.texture = texture;
        this.overlay = null;
        this.hasToolTip = true;
        this.iconWidth = width;
        this.iconHeight = height;
        if (tooltip != null && !tooltip.isEmpty())
            this.setTooltip(Tooltip.create(Component.literal(tooltip)));
    }

    public ImagedGuiButton setTextAlign(TextAlign ta) {
        alignment = ta;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.updateVisibility();
        if (!this.visible || this.invisible)
            return;

        boolean hover = this.isHoveredOrFocused();

        // Recompute the texture UV every frame, allowing subclasses to override it
        // (and to apply a "hovered" variant) just as the legacy getHoverState did.
        u = baseU;
        v = baseV;
        this.modifyTextureUV();
        if (hover)
            this.getHoveredTextureCoordinates();

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.getX(), this.getY(), u, v, this.width, this.height, textureSize, textureSize);

        if (overlay != null && !hasToolTip) {
            graphics.text(renderer, overlay, this.getLabelX() + alignment.getDX(renderer, overlay), this.getLabelY(), this.getLabelColor(), shadow);
        }

        if (icon != null) {
            int dx = (this.width - iconWidth) / 2;
            int dy = (this.height - iconHeight) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, this.getX() + dx, this.getY() + dy, iconWidth, iconHeight);
        }

        if (!lastHover && hover && ticks > 1)
            this.onHoverTo();

        lastHover = hover;
        hoverTicks = lastHover ? hoverTicks + 1 : 0;
        if (lastHover)
            hoverFade = Math.min(1, hoverFade + hoverFadeSpeedUp);
        else
            hoverFade = Math.max(0, hoverFade - hoverFadeSpeedDown);
        ticks++;
    }

    protected void updateVisibility() {

    }

    protected void modifyTextureUV() {

    }

    protected void getHoveredTextureCoordinates() {

    }

    protected void onHoverTo() {

    }

    protected int getLabelX() {
        int base = textOffset + this.getX();
        return switch (alignment) {
            case CENTER -> base + this.width / 2 - renderer.width(overlay) + 1;
            case LEFT -> base + 2;
            case RIGHT -> base + this.width - 4 - renderer.width(overlay) * 2;
        };
    }

    protected int getLabelY() {
        return this.getY() + (this.height - 8) / 2;
    }

    public int getLabelColor() {
        return color;
    }

    public final float getHoverFade() {
        return hoverFade;
    }

    public enum TextAlign {
        LEFT(),
        CENTER(),
        RIGHT();

        public int getDX(Font f, String s) {
            return switch (this) {
                case CENTER -> f.width(s) / 2;
                case LEFT -> 0;
                case RIGHT -> f.width(s);
            };
        }
    }

}
