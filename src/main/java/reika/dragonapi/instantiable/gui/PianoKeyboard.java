
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

import java.util.ArrayList;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
// Removed IForgeGuiGraphics and ScreenUtils - no longer exist in NeoForge

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Gui;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;


public class PianoKeyboard extends Gui {

    public final int guiX;
    public final int guiY;
    private final MusicGui guiInstance;

    private final ArrayList<PianoKey> keyList = new ArrayList<>();

    private static final KeyShape[] shapeList = {
            KeyShape.LEFT,
            KeyShape.BLACK,
            KeyShape.MIDDLE,
            KeyShape.BLACK,
            KeyShape.MIDDLE,
            KeyShape.BLACK,
            KeyShape.RIGHT,
            KeyShape.LEFT,
            KeyShape.BLACK,
            KeyShape.MIDDLE,
            KeyShape.BLACK,
            KeyShape.RIGHT
    };

    public PianoKeyboard(int x, int y, MusicGui gui) {
        super(Minecraft.getInstance());
        guiX = x;
        guiY = y;
        guiInstance = gui;

        int dx = 6;
        for (int m = 0; m < 4; m++) {
            for (int i = 0; i <= 4; i += 2) {
                int id = i + 1 + m * 12;
                keyList.add(new PianoKey(id, x + dx + i * 4, y + 1, this.getShapeFromIndex(id), guiInstance));
            }
            dx += 32;
            for (int i = 0; i <= 2; i += 2) {
                int id = i + 8 + m * 12;
                keyList.add(new PianoKey(id, x + dx + i * 4, y + 1, this.getShapeFromIndex(id), guiInstance));
            }
            dx += 24;
        }

        for (int i = 0; i <= 56; i += 2) {
            int id = i;
            if (id >= 8)
                id--;
            if (id >= 13)
                id--;
            if (id >= 20)
                id--;
            if (id >= 25)
                id--;
            if (id >= 32)
                id--;
            if (id >= 37)
                id--;
            if (id >= 44)
                id--;
            if (id >= 49)
                id--;

//            buttonList.add(new InvisibleButton(id, x, k+150, w, 37, ""));
            keyList.add(new PianoKey(id, x + i * 4, y + 1, this.getShapeFromIndex(id), guiInstance));
        }

    }

    private KeyShape getShapeFromIndex(int i) {
        if (i == 48)
            return KeyShape.WHITE;
        return shapeList[i % shapeList.length];
    }

    public void mouseClicked(double x, double y, int button) {
        // 1.21.5: AbstractWidget#mouseClicked now takes a MouseButtonEvent + double-click flag.
        net.minecraft.client.input.MouseButtonEvent event = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0));
        for (PianoKey key : keyList) {
            if (key.mouseClicked(event, false)) {
                guiInstance.onKeyPressed(key);
                return;
            }
        }
    }

    public void drawKeys(GuiGraphicsExtractor stack) {
        stack.blit(RenderPipelines.GUI_TEXTURED, guiInstance.bindKeyboardTexture(), guiX, guiY, 0, 64, 232, 37, 256, 256);

        Minecraft mc = Minecraft.getInstance();
        
        for (PianoKey key : keyList) {
            key.extractRenderState(stack, 0, 0, 0f);
        }
        

        stack.text(mc.font, "F", guiX - 6, guiY + 28, 0);
        stack.text(mc.font, "F", guiX + 233, guiY + 28, 0);
    }

    public static class PianoKey extends Button {

        public final KeyShape hitbox;
        private int alpha = 0;
        private final MusicGui guiInstance;

        public PianoKey(int note, int x, int y, KeyShape shape, MusicGui gui) {
            super(new Builder(Component.literal(""), b -> {}).pos(x, y).size(0, 0)); //todo i think note as an id of sorts
            hitbox = shape;
            guiInstance = gui;
        }

        @Override
        protected void extractContents(net.minecraft.client.gui.GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y, float p_93679_) {
            int c = guiInstance.getColorForChannel(guiInstance.getActiveChannel());
            int rgb = (c & 0xffffff) | (alpha << 24);
            if (alpha > 0) {
                switch (hitbox) {
                    case BLACK ->
                            ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 1, getY() + 1, getX() + 3, getY() + 20, rgb, true);
                    case LEFT -> {
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 1, getY(), getX() + 6, getY() + 35, rgb, true);
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 6, getY() + 21, getX() + 7, getY() + 35, rgb, true);
                    }
                    case MIDDLE -> {
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 2, getY(), getX() + 6, getY() + 21, rgb, true);
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 1, getY() + 21, getX() + 7, getY() + 35, rgb, true);
                    }
                    case RIGHT -> {
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 2, getY(), getX() + 7, getY() + 35, rgb, true);
                        ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 1, getY() + 21, getX() + 2, getY() + 35, rgb, true);
                    }
                    case WHITE ->
                            ReikaGuiAPI.instance.drawRect(GuiGraphicsExtractor, getX() + 1, getY(), getX() + 7, getY() + 35, rgb, true);
                    default -> {
                    }
                }
                alpha--;
            }
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            double x = event.x();
            double y = event.y();
            int button = event.button();
            ReikaGuiAPI api = ReikaGuiAPI.instance;
            boolean flag = false;
            switch (hitbox) {
                case BLACK -> {
                    if (api.isMouseInBox(getX(), getX() + 4, getY(), getY() + 21, x, y))
                        flag = true;
                }
                case LEFT -> {
                    if (api.isMouseInBox(getX() + 1, getX() + 6, getY(), getY() + 35, x, y))
                        flag = true;
                    if (api.isMouseInBox(getX() + 5, getX() + 7, getY() + 21, getY() + 35, x, y))
                        flag = true;
                }
                case MIDDLE -> {
                    if (api.isMouseInBox(getX() + 2, getX() + 6, getY(), getY() + 35, x, y))
                        flag = true;
                    if (api.isMouseInBox(getX() + 1, getX() + 7, getY() + 21, getY() + 35, x, y))
                        flag = true;
                }
                case RIGHT -> {
                    if (api.isMouseInBox(getX() + 2, getX() + 7, getY(), getY() + 35, x, y))
                        flag = true;
                    if (api.isMouseInBox(getX() + 1, getX() + 7, getY() + 21, getY() + 35, x, y))
                        flag = true;
                }
                case WHITE -> {
                    if (api.isMouseInBox(getX() + 1, getX() + 7, getY(), getY() + 35, x, y))
                        flag = true;
                }
                default -> {
                }
            }

            if (flag)
                alpha = 255;
            //ReikaJavaLibrary.pConsole(alpha);
            return flag;
        }

    }

    private enum KeyShape {
        WHITE(), //keyboard end
        BLACK(), //accidentals
        LEFT(), //C, F
        RIGHT(), //E, B
        MIDDLE(); //D, G, A

        KeyShape() {

        }
    }

    public interface MusicGui {

        int getActiveChannel();

        Identifier bindKeyboardTexture();

        void onKeyPressed(PianoKey key);

        int getColorForChannel(int channel);

    }
}


