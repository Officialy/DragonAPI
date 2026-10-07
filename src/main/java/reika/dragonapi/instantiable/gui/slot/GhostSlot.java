/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.gui.slot;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that needs no inventory. Use it for things like ghost items (like diamond pipes): the
 * owning menu's {@code clicked} reads the cursor stack and records it, and the screen draws the
 * recorded items itself. The slot never holds, accepts or yields a real item.
 *
 * <p>1.7.10 passed a null inventory and overrode the old getStack/putStack. 26.3's
 * {@link Slot#getItem()} and the menu's change broadcasting read the container directly, so a
 * null one crashed as soon as the menu opened; this one is backed by a permanently empty container.
 */
public final class GhostSlot extends Slot {

    private static final Container EMPTY = new SimpleContainer(0);

    public GhostSlot(Container ii, int id, int x, int y) {
        super(ii != null ? ii : EMPTY, id, x, y);
    }

    public GhostSlot(int idx, int x, int y) {
        this(null, idx, x, y);
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
}
