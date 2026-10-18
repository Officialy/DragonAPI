package reika.dragonapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;

/** Client presentation only. Each opened screen owns its displayed message and input lifetime. */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class PopupClient {
    public static final Identifier LAYER = Identifier.parse("dragonapi:popup_warnings");
    private static final Identifier ICON = Identifier.parse("dragonapi:textures/gui/warning.png");
    private PopupClient() {}

    @SubscribeEvent public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.TITLE, LAYER, (graphics, delta) -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.gui.hud.isHidden() || minecraft.gui.screen() instanceof PopupScreen) return;
            draw(graphics, PopupWriter.instance().currentMessage());
        });
    }

    @SubscribeEvent public static void key(InputEvent.Key event) {
        var minecraft = Minecraft.getInstance();
        if (event.getAction() == com.mojang.blaze3d.platform.InputConstants.PRESS
                && (event.getKey() == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LCTRL
                || event.getKey() == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RCTRL)
                && minecraft.hasControlDown()) open();
    }

    public static void open() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.gui.screen() == null && PopupWriter.instance().hasMessages())
            minecraft.gui.setScreen(new PopupScreen());
    }

    public static void clearSession() {
        PopupWriter.instance().clearMessages();
        var minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof PopupScreen screen) screen.onClose();
    }

    private record Layout(PopupWriter.Message message, int width, int height) {
        int buttonX() { return 2 + width - 20; }
        int buttonY() { return 2 + height - 20; }
        boolean contains(double x, double y) {
            return x >= buttonX() && x <= buttonX() + 16 && y >= buttonY() && y <= buttonY() + 16;
        }
    }

    private static Layout draw(GuiGraphicsExtractor graphics, PopupWriter.Message message) {
        if (message == null) return null;
        var font = Minecraft.getInstance().font;
        var warning = message.warning();
        int width = warning.width();
        var lines = font.split(FormattedText.of(warning.text()), width - 25);
        int height = Math.max(48, 7 + lines.size() * font.lineHeight);
        var layout = new Layout(message, width, height);
        var api = ReikaGuiAPI.instance;
        api.drawRect(graphics, 2, 2, 2 + width, 2 + height, 0xff4a4a4a, false);
        api.drawRectFrame(graphics, 2, 2, width, height, 0xb0b0b0);
        api.drawRectFrame(graphics, 4, 4, width - 4, height - 4, 0xcfcfcf);
        int y = 6;
        for (var line : lines) {
            graphics.text(font, line, 6, y, 0xffffff, false);
            y += font.lineHeight;
        }
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICON,
                2 + width - 24, 2, 0, 0, 24, 24, 48, 48, 48, 48);
        int x = layout.buttonX(); y = layout.buttonY();
        api.drawRect(graphics, x, y, x + 16, y + 16, 0xff707070, false);
        api.drawRectFrame(graphics, x, y, 16, 16, 0xffcfcfcf);
        graphics.text(font, "x", x + 4, y + 4, 0xffffffff, false);
        return layout;
    }

    public static final class PopupScreen extends Screen {
        private Layout layout;

        private PopupScreen() { super(Component.literal("PopupWriterScreen")); }

        @Override public boolean isPauseScreen() { return false; }
        @Override public boolean isInGameUi() { return true; }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            layout = minecraft.gui.hud.isHidden() ? null : draw(graphics, PopupWriter.instance().currentMessage());
        }

        @Override protected void init() { layout = null; }
        @Override public void removed() { layout = null; }

        @Override public void tick() {
            if (!minecraft.hasControlDown() || !PopupWriter.instance().hasMessages()) onClose();
        }

        @Override public boolean keyReleased(KeyEvent event) {
            if (!minecraft.hasControlDown()) { onClose(); return true; }
            return super.keyReleased(event);
        }

        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                    || !minecraft.hasControlDown() || minecraft.gui.hud.isHidden()
                    || layout == null || !layout.contains(event.x(), event.y())) return false;
            if (!PopupWriter.instance().dismiss(layout.message())) { layout = null; return false; }
            layout = null;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1));
            if (!PopupWriter.instance().hasMessages()) onClose();
            return true;
        }
    }
}
