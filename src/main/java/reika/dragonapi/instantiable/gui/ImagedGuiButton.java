//package reika.dragonapi.instantiable.gui;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.Font;
//import net.minecraft.client.gui.components.Button;
//import net.minecraft.client.resources.sounds.SimpleSoundInstance;
//import net.minecraft.client.sounds.SoundManager;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.sounds.SoundEvent;
//import reika.dragonapi.libraries.rendering.ReikaGuiAPI;
//
//public class ImagedButton extends Button {
//
//    protected final boolean hasToolTip;
//    protected final Class<?> modClass;
//    private final int baseU;
//    private final int baseV;
//    public TextAlign alignment = TextAlign.CENTER;
//    public int textOffset = 0;
//    public Font renderer = Minecraft.getInstance().font;
//    public int textureSize = 256;
//    public boolean invisible = false;
//    public float hoverFadeSpeedUp = 0.08F;
//    public float hoverFadeSpeedDown = 0.15F;
//    public int iconWidth = width;
//    public int iconHeight = height;
//    protected int u;
//    protected int v;
//    protected int hoverTicks;
//    SoundEvent sound = new SoundEvent(ResourceLocation.parse("gui.button.press"));
//    private int color;
//    private boolean shadow = true;
//    private String filepath;
//    private boolean lastHover;
//    private float hoverFade;
//    private int ticks = 0;
//
//    public ImagedButton(int par1, int par2, int par3, String par4Str, Class mod) {
//        super(par1, par2, par3, 200, 20, par4Str);
//
//        hasToolTip = false;
//        modClass = mod;
//        baseU = u = 0;
//        baseV = v = 0;
//    }
//
//    /**
//     * Draw a Gui Button with an image background. Args: id, x, y, width, height, u, v, filepath, class root
//     */
//    public ImagedButton(int par1, int par2, int par3, int par4, int par5, int par7, int par8, String file, Class<?> mod) {
//        super(par1, par2, par3, 200, 20, null);
//        enabled = true;
//        visible = true;
//        id = par1;
//        x = par2;
//        y = par3;
//        width = par4;
//        height = par5;
//        displayString = null;
//
//        u = par7;
//        v = par8;
//        baseU = u;
//        baseV = v;
//        filepath = file;
//
//        hasToolTip = false;
//        modClass = mod;
//    }
//
//    /**
//     * Draw a Gui Button with an image background and text overlay.
//     * Args: id, x, y, width, height, u, v, text overlay, text color, shadow, filepath, class root
//     */
//    public ImagedButton(int par1, int par2, int par3, int par4, int par5, int par7, int par8, String par6Str, int par9, boolean par10, String file, Class<?> mod) {
//        super(par1, par2, par3, 200, 20, par6Str);
//        enabled = true;
//        visible = true;
//        id = par1;
//        x = par2;
//        y = par3;
//        width = par4;
//        height = par5;
//        displayString = par6Str;
//
//        u = par7;
//        v = par8;
//        baseU = u;
//        baseV = v;
//        color = par9;
//        shadow = par10;
//        filepath = file;
//
//        hasToolTip = false;
//        modClass = mod;
//    }
//
//    /**
//     * Draw a Gui Button with an image background and text tooltip. Args: id, x, y, width, height, u, v, filepath, text tooltip, text color, shadow
//     */
//    public ImagedButton(int par1, int par2, int par3, int par4, int par5, int par7, int par8, String file, String par6Str, int par9, boolean par10, Class<?> mod) {
//        super(par1, par2, par3, 200, 20, par6Str);
//        enabled = true;
//        visible = true;
//        id = par1;
//        x = par2;
//        y = par3;
//        width = par4;
//        height = par5;
//        displayString = par6Str;
//
//        u = par7;
//        v = par8;
//        baseU = u;
//        baseV = v;
//        color = par9;
//        shadow = par10;
//        filepath = file;
//
//        hasToolTip = true;
//        modClass = mod;
//    }
//
//    public ImagedButton setTextAlign(TextAlign ta) {
//        alignment = ta;
//        return this;
//    }
//
//    protected final String getButtonTexture() {
//        return filepath;
//    }
//
//    @Override
//    public final void drawButton(Minecraft mc, int mx, int my) {
//        this.updateVisibility();
//
//        if (visible && !invisible) {
//            field_146123_n = this.isPositionWithin(mx, my);
//            int k = this.getHoverState(field_146123_n);
//
//            this.renderButton();
//
////            this.mouseDragged(mx, my);
//            if (displayString != null && !hasToolTip) {
//                //ReikaTextureHelper.bindFontTexture();
//                //GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
//                renderer.draw(displayString, this.getLabelX() + alignment.getDX(renderer, displayString), this.getLabelY(), this.getLabelColor(), shadow);
//            } else if (k == 2 && displayString != null && hasToolTip) {
//                this.drawToolTip(mc, mx, my);
//            }
//            RenderSystem.setShaderColor(1, 1, 1, 1);
//
//            if (icon != null) {
////                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
////                GL11.glDisable(GL11.GL_LIGHTING);
//                RenderSystem.enableBlend();
//                RenderSystem.defaultBlendFunc();
////                        ReikaTextureHelper.bindTerrainTexture();
//                int dx = (width - iconWidth) / 2;
//                int dy = (height - iconHeight) / 2;
//                ReikaGuiAPI.instance.drawTexturedModelRectFromIcon(x + dx, y + dy, icon, iconWidth, iconHeight);
////                GL11.glPopAttrib();
//            }
//
//            if (!lastHover && field_146123_n && ticks > 1) {
//                this.onHoverTo();
//            }
//
//            lastHover = field_146123_n;
//            hoverTicks = lastHover ? hoverTicks + 1 : 0;
//            if (lastHover) {
//                hoverFade = Math.min(1, hoverFade + hoverFadeSpeedUp);
//            } else {
//                hoverFade = Math.max(0, hoverFade - hoverFadeSpeedDown);
//            }
//            ticks++;
//        }
//    }
//
//    protected void updateVisibility() {
//
//    }
//
//    protected void renderButton() {
//        //int tex = GL11.GL_TEXTURE_BINDING_2D;
//        ReikaTextureHelper.bindTexture(modClass, this.getButtonTexture());
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        this.drawTexturedModalRect(new PoseStack(), x, y, u, v, width, height);
//    }
//
//    @Override
//    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
//        return enabled && visible && this.isPositionWithin(pMouseX, pMouseY);
//    }
//
//    protected boolean isPositionWithin(double mx, double my) {
//        return mx >= x && my >= y && mx < x + width && my < y + height;
//    }
//
//    @Override
//    public final int getHoverState(boolean flag) {
//        int ret = super.getHoverState(flag);
//        u = baseU;
//        v = baseV;
//        this.modifyTextureUV();
//        if (ret == 2) {
//            this.getHoveredTextureCoordinates();
//        }
//        return ret;
//    }
//
//    protected void modifyTextureUV() {
//
//    }
//
//    protected void getHoveredTextureCoordinates() {
//
//    }
//
//    protected void onHoverTo() {
//
//    }
//
//    public void drawTexturedModalRect(PoseStack poseStack, int x, int y, int u, int v, int w, int h) {
//        float f = 1F / textureSize;
//        Tesselator tesselator = Tesselator.getInstance();
//        BufferBuilder builder = tesselator.getBuilder();
//        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        builder.vertex(x, y + h, zLevel).uv((u + 0) * f, (v + h) * f).endVertex();
//        builder.vertex(x + w, y + h, zLevel).uv((u + w) * f, (v + h) * f).endVertex();
//        builder.vertex(x + w, y + 0, zLevel).uv((u + w) * f, (v + 0) * f).endVertex();
//        builder.vertex(x, y + 0, zLevel).uv((u + 0) * f, (v + 0) * f).endVertex();
//        tesselator.end();
////        builder.draw();
//    }
//
//
////    public void func_146113_a(SoundHandler sh) {
////        sh.playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation(sound), 1.0F));
////    }
//
//    @Override
//    public void playDownSound(SoundManager pHandler) {
//        pHandler.play(SimpleSoundInstance.forUI(sound, 1.0F));
//    }
//
//    protected int getLabelX() {
//        int base = textOffset + x;
//        return switch (alignment) {
//            case CENTER -> base + width / 2 - renderer.width(displayString) + 1;
//            case LEFT -> base + 2;
//            case RIGHT -> base + width - 4 - renderer.width(displayString) * 2;
//            default -> base;
//        };
//    }
//
//    protected int getLabelY() {
//        return y + (height - 8) / 2;
//    }
//
//    public int getLabelColor() {
//        return color;
//    }
//
//    protected void drawToolTip(Minecraft mc, int mx, int my) {
//        ReikaGuiAPI.instance.drawTooltip(mc.font, displayString);
//        ReikaTextureHelper.bindFontTexture();
//    }
//
//    public float getHoverFade() {
//        return hoverFade;
//    }
//
//    public enum TextAlign {
//        LEFT(),
//        CENTER(),
//        RIGHT();
//
//        public int getDX(Font f, String s) {
//            return switch (this) {
//                case CENTER -> f.width(s) / 2;
//                case LEFT -> 0;
//                case RIGHT -> f.width(s);
//                default -> 0;
//            };
//        }
//    }
//
//}
