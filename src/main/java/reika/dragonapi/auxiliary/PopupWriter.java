package reika.dragonapi.auxiliary;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.joml.Matrix4f;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;

import java.util.ArrayList;
import java.util.Collection;

import static reika.dragonapi.DragonAPI.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PopupWriter extends Screen {

    public static final PopupWriter instance = new PopupWriter(Component.literal("PopupWriterScreen"));
    public static final ArrayList<Warning> list = new ArrayList<>();
    private final ArrayList<Warning> serverMessages = new ArrayList<>();
    private final PlayerMap<Collection<Warning>> alreadySent = new PlayerMap<>();
    public boolean ungrabbed = false;
    private static double buttonX;
    private static double buttonY;
    private static int buttonSize;

    protected PopupWriter(Component pTitle) {
        super(pTitle);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(PopupWriter.instance);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (Screen.hasControlDown() && !PopupWriter.instance.ungrabbed) {
            //ReikaJavaLibrary.pConsole("Press");
            Minecraft.getInstance().setScreen(this);
            Minecraft.getInstance().mouseHandler.releaseMouse();
            PopupWriter.instance.ungrabbed = true;
            return true;
        } else if (PopupWriter.instance.ungrabbed) {
            //ReikaJavaLibrary.pConsole("Release");
            Minecraft.getInstance().mouseHandler.grabMouse();
            PopupWriter.instance.ungrabbed = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        if (!Screen.hasControlDown()) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void addMessage(String w) {
        this.addMessage(new Warning(w));
    }

    public void addMessage(Warning w) {
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            serverMessages.add(w);
        } else {
            //sb.append(" CTRL-ALT-click to close this message.");
            String sg = w.text + " Hold CTRL to be able to click this message.";
            list.add(new Warning(sg, w.width));
        }
    }

    public void sendServerMessages(ServerPlayer ep) {
        PacketTarget pt = new PacketTarget.PlayerTarget(ep);
        Collection<Warning> c = alreadySent.get(ep);
        if (c == null) {
            c = new ArrayList<>();
        }
        for (Warning s : serverMessages) {
            if (c.contains(s))
                continue;
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.POPUP.ordinal(), pt, s.text, s.width);
            c.add(s);
        }
        alreadySent.put(ep, c);
    }

    @SubscribeEvent
    public static void drawOverlay(RenderGuiOverlayEvent event) {
        if (!list.isEmpty() && event.getOverlay() == VanillaGuiOverlay.TITLE_TEXT.type()) {
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            PoseStack matrixStack = event.getGuiGraphics().pose();
            Matrix4f matrix = matrixStack.last().pose();

            Warning s = list.get(0);
            //Font renderer
            Font fr = Minecraft.getInstance().font;
            int x = 2;
            int y = 2;
            int w = s.width;
            int sw = w - 25;
            //How many lines of text can fit in the box
            int lines = fr.split(FormattedText.of(s.text), sw).size();
            int h = 7 + (lines) * (fr.lineHeight); //FONT_HEIGHT

            int sz = 24;
            int dx = x + w - sz;
            int dy = y;

            //Draw the main rectangle and the border
            ReikaGuiAPI.instance.drawRect(matrixStack, x, y, x + w, y + h, 0xff4a4a4a, false);
            ReikaGuiAPI.instance.drawRectFrame(matrixStack, x, y, w, h, 0xb0b0b0);
            ReikaGuiAPI.instance.drawRectFrame(matrixStack, x + 2, y + 2, w - 4, h - 4, 0xcfcfcf);

            //Draw the text
//todo            fr.drawWordWrap(matrixStack, FormattedText.of(s.text), x + 4, y + 4, sw, 0xffffff);

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            //Apply the texture to the warning icon
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, (ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/warning.png")));
            //Draw the warning icon
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, dx, dy + sz, 0).uv(0, 1).endVertex();
            buffer.vertex(matrix, dx + sz, dy + sz, 0).uv(1, 1).endVertex();
            buffer.vertex(matrix, dx + sz, dy, 0).uv(1, 0).endVertex();
            buffer.vertex(matrix, dx, dy, 0).uv(0, 0).endVertex();
            tesselator.end();

            sz = 16;
            dx = x + w - sz - 4;
            dy = y + h - sz - 4;

            buttonX = dx;
            buttonY = dy;
            buttonSize = sz;

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            //Apply the texture to the button
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/buttons.png"));
            //Draw the close button
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(dx, dy + sz, 0).uv(0.5f, 0.25f).endVertex();
            buffer.vertex(dx + sz, dy + sz, 0).uv(0.75f, 0.25f).endVertex();
            buffer.vertex(dx + sz, dy, 0).uv(0.75f, 0).endVertex();
            buffer.vertex(dx, dy, 0).uv(0.5f, 0).endVertex();
            tesselator.end();

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed evt) {
//        ReikaJavaLibrary.pConsole(buttonX + "," + buttonY);

        if (!list.isEmpty() && buttonX > 0 && buttonY > 0) {
//            ReikaJavaLibrary.pConsole(evt.getMouseX() + "," + evt.getMouseY());

            if (evt.getMouseX() >= buttonX && evt.getMouseX() <= buttonX + buttonSize && evt.getMouseY() >= buttonY && evt.getMouseY() <= buttonY + buttonSize) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER)));
                list.remove(0);
            }
        }
    }

    //    @SubscribeEvent
//    public void keyHandle(InputEvent.KeyInputEvent evt) {
//        if (!PopupWriter.instance.list.isEmpty() || PopupWriter.instance.ungrabbed) {
//            if (Screen.hasControlDown() && !PopupWriter.instance.ungrabbed) {
//                //ReikaJavaLibrary.pConsole("Press");
//                Minecraft.getInstance().setScreen(this);
//                Minecraft.getInstance().mouseHandler.releaseMouse();
//                PopupWriter.instance.ungrabbed = true;
//            } else if (PopupWriter.instance.ungrabbed) {
//                //ReikaJavaLibrary.pConsole("Release");
//                Minecraft.getInstance().mouseHandler.grabMouse();
//                PopupWriter.instance.ungrabbed = false;
//            }
//        }
//    }

    @SubscribeEvent
    public static void keyHandle(InputEvent.Key evt) {
        if (!list.isEmpty()) {
            if (Screen.hasControlDown()) {
                open();
            }
        }
    }

    public static class Warning {

        public final String text;
        public final int width;

        public Warning(String s) {
            this(s, Math.max(calcMinSizeForText(s), 192));
        }

        public Warning(String s, int w) {
            text = s;
            width = Math.min(300, w);
        }

        private static int calcMinSizeForText(String s) { //at w=192, 74 chars becomes 4 lines, or about 18 chars a line (1 char = 11px); ideally keep line count <= 6
            int w = 192;
            int c = 18;
            int lines = s.length() / c;
            while (lines > 6) {
                w += 16;
                c += 2;
                lines = s.length() / c;
            }
            return w;
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Warning && text.equals(((Warning) o).text);
        }

    }

}
