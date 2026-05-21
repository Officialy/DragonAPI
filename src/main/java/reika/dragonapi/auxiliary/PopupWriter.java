package reika.dragonapi.auxiliary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import static reika.dragonapi.DragonAPI.MODID;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
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
    public boolean keyPressed(KeyEvent keyEvent) {
        if (Minecraft.getInstance().hasControlDown() && !PopupWriter.instance.ungrabbed) {
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
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (!Minecraft.getInstance().hasControlDown()) {
            this.onClose();
            return true;
        }
        return super.keyReleased(keyEvent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void addMessage(String w) {
        this.addMessage(new Warning(w));
    }

    public void addMessage(Warning w) {
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
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
    public static void drawOverlay(RenderGuiLayerEvent.Post event) {
        if (!list.isEmpty() && event.getName().equals(VanillaGuiLayers.TITLE)) {
            GuiGraphics gui = event.getGuiGraphics();
            PoseStack matrixStack = new PoseStack();
            Warning s = list.get(0);
            Font fr = Minecraft.getInstance().font;

            int x = 2;
            int y = 2;
            int w = s.width;
            int sw = w - 25;

            List<FormattedCharSequence> linesList = fr.split(FormattedText.of(s.text), sw);
            int h = 7 + linesList.size() * fr.lineHeight;

            int sz = 24;
            int dx = x + w - sz;
            int dy = y;

            // Draw rectangles
            ReikaGuiAPI.instance.drawRect(matrixStack, x, y, x + w, y + h, 0xff4a4a4a, false);
            ReikaGuiAPI.instance.drawRectFrame(matrixStack, x, y, w, h, 0xb0b0b0);
            ReikaGuiAPI.instance.drawRectFrame(matrixStack, x + 2, y + 2, w - 4, h - 4, 0xcfcfcf);

            // Draw wrapped text
            int textY = y + 4;
            for (FormattedCharSequence line : linesList) {
                gui.drawString(fr, line, x + 4, textY, 0xFFFFFF, false);
                textY += fr.lineHeight;
            }

            // TODO: Update to modern rendering API - use GuiGraphics.blit() for textures
            // Old immediate mode OpenGL rendering removed in 1.21
            // For now, commenting out texture rendering to get compilation working
            // Draw warning icon (stubbed)
            // gui.blit(WARNING_ICON_TEXTURE, dx, dy, 0, 0, 0, sz, sz, 256, 256);

            // Draw close button
            sz = 16;
            dx = x + w - sz - 4;
            dy = y + h - sz - 4;

            buttonX = dx;
            buttonY = dy;
            buttonSize = sz;
            
            // TODO: Draw close button texture using GuiGraphics.blit()
            // gui.blit(CLOSE_BUTTON_TEXTURE, dx, dy, 0, 0, 0, sz, sz, 256, 256);
        }
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed evt) {
//        ReikaJavaLibrary.pConsole(buttonX + "," + buttonY);

        if (!list.isEmpty() && buttonX > 0 && buttonY > 0) {
//            ReikaJavaLibrary.pConsole(evt.getMouseX() + "," + evt.getMouseY());

            if (evt.getMouseX() >= buttonX && evt.getMouseX() <= buttonX + buttonSize && evt.getMouseY() >= buttonY && evt.getMouseY() <= buttonY + buttonSize) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER)));
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
            if (Minecraft.getInstance().hasControlDown()) {
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

