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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class InvisibleButton extends Button {


    protected InvisibleButton(int p_259075_, int p_259271_, int p_260232_, int p_260028_, Component p_259351_, OnPress p_260152_, CreateNarration p_259552_) {
        super(p_259075_, p_259271_, p_260232_, p_260028_, p_259351_, p_260152_, p_259552_);
    }

    @Override
    public void renderButton(PoseStack p_93746_, int p_93747_, int p_93748_, float p_93749_) {
        //super.drawButton(par1Minecraft, par2, par3);
    }

}