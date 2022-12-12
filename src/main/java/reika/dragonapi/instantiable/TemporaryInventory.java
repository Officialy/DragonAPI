/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import net.minecraft.world.item.ItemStack;

public class TemporaryInventory extends BasicInventory {

    public TemporaryInventory(int size) {
        super("temp", size);
    }

    public TemporaryInventory(int size, int stack) {
        super("temp", size, stack);
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ItemStack removeItem(int p_18942_, int p_18943_) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_18951_) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearContent() {
        // TODO Auto-generated method stub

    }

}
