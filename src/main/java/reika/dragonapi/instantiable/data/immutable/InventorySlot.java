package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InventorySlot {

    public final Container inventory;
    public final int slot;

    public InventorySlot(int slot, Container inv) {
        inventory = inv;
        this.slot = slot;
    }

    public ItemStack getStack() {
        return inventory.getItem(slot);
    }

    public int getStackSize() {
        ItemStack is = this.getStack();
        return is != null ? is.getCount() : 0;
    }

    public int decrement(int amt) {
        ItemStack is = this.getStack();
        int ret = Math.min(amt, is.getCount());
        is.setCount(is.getCount() - amt);
        if (is.getCount() <= 0)
            inventory.setItem(slot, ItemStack.EMPTY);
        return ret;
    }

    public int increment(int amt) {
        ItemStack is = this.getStack();
        int max = Math.min(is.getMaxStackSize(), inventory.getMaxStackSize());
        int ret = Math.min(amt, max - is.getCount());
        is.setCount(is.getCount() + ret);
        if (is.getCount() <= 0)
            inventory.setItem(slot, ItemStack.EMPTY);
        return ret;
    }

    public ItemStack setSlot(ItemStack is) {
        ItemStack prev = this.getStack();
        inventory.setItem(slot, is);
        return prev;
    }

    public boolean isEmpty() {
        return this.getStack() == null;
    }

    @Override
    public String toString() {
        return "Slot " + slot + " of " + inventory;
    }

    public Slot toSlot(int x, int y) {
        return new Slot(inventory, slot, x, y);
    }
}
