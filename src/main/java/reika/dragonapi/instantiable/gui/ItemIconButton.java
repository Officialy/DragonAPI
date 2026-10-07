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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A 16x16 button drawn as an item icon, with no frame or label. */
public class ItemIconButton extends Button {

    private final int color;
    private final ItemStack iconItem;

    /** Args: id, x, y, color, itemstack, click action (1.7.10 routed the id to actionPerformed). */
    public ItemIconButton(int id, int x, int y, int color, ItemStack is, OnPress onPress) {
        super(new Builder(Component.empty(), onPress).pos(x, y).size(16, 16));
        this.color = color;
        this.iconItem = is == null ? ItemStack.EMPTY : is.copy();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.item(iconItem, getX(), getY());
    }
}
