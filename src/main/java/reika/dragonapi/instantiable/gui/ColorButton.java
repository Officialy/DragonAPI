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

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;

public class ColorButton extends Button {

    private final int color;
    private final int brighter;
    public boolean isSelected = false;

    public ColorButton(int par1, int par2, int par3, int par4, int par5, Color c) {
        this(par1, par2, par3, par4, par5, c.getRGB());
    }

    /** Draw a Gui Button with an image background. Args: id, x, y, width, height, color*/
    public ColorButton(int par1, int par2, int par3, int par4, int par5, int par9)
    {
        super(new Builder(Component.literal(""), Button::onPress).pos(par2, par3).size(par4, par5));
//        enable = true;
        visible = true;
//        id = par1;
        width = par4;
        height = par5;
//        displayString = null;

        color = par9;
        Color br = Color.decode(String.valueOf(color));
        int r = Math.min(255, br.getRed()+96);
        int g = Math.min(255, br.getGreen()+96);
        int b = Math.min(255, br.getBlue()+96);
        brighter = new Color(r, g, b).getRGB();
    }

    @Override
    public void renderButton(PoseStack stack, int p_93677_, int p_93678_, float p_93679_) {
        if (isSelected) {
            ReikaGuiAPI.instance.drawRect(stack, getX(), getY(), getX()+width, getY()+height, 0xff777777, false);
            ReikaGuiAPI.instance.drawRect(stack, getX(), getY(), getX()+width-1, getY()+height-1, 0xff333333, false);
            ReikaGuiAPI.instance.drawRect(stack, getX()+1, getY()+1, getX()+width-1, getY()+height-1, 0xff000000 | brighter, false);
            ReikaGuiAPI.instance.drawRect(stack, getX()+1, getY()+1, getX()+width-2, getY()+height-2, 0xff000000 | color, false);
        }
        else {
            ReikaGuiAPI.instance.drawRect(stack, getX(), getY(), getX()+width, getY()+height, 0xff333333, false);
            ReikaGuiAPI.instance.drawRect(stack, getX(), getY(), getX()+width-1, getY()+height-1, 0xff777777, false);
            ReikaGuiAPI.instance.drawRect(stack, getX()+1, getY()+1, getX()+width-1, getY()+height-1, 0xff000000 | brighter, false);
            ReikaGuiAPI.instance.drawRect(stack, getX()+2, getY()+2, getX()+width-1, getY()+height-1, 0xff000000 | color, false);
        }
    }
}
