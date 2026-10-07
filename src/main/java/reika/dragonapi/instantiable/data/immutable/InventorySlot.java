package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public record InventorySlot(Container inventory, int slot) {

    public InventorySlot(int slot, Container inv) {
        this(inv, slot);
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
        if (is == null || is.isEmpty() || amt <= 0)
            return 0;
        int ret = Math.min(amt, is.getCount());
        is.shrink(ret);
        if (is.getCount() <= 0)
            inventory.setItem(slot, ItemStack.EMPTY);
        else
            inventory.setChanged();
        return ret;
    }

    public int increment(int amt) {
        ItemStack stack = this.getStack();
        if (stack.isEmpty() || amt <= 0) return 0;
        int max = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize());
        int added = Math.min(amt, Math.max(0, max - stack.getCount()));
        if (added > 0) {
            stack.grow(added);
            inventory.setChanged();
        }
        return added;
    }

    public ItemStack setSlot(ItemStack is) {
        ItemStack prev = this.getStack();
        inventory.setItem(slot, is != null ? is : ItemStack.EMPTY);
        return prev;
    }

    public boolean isEmpty() {
        ItemStack is = this.getStack();
        return is == null || is.isEmpty();
    }

    @Override
    public String toString() {
        return "Slot " + slot + " of " + inventory;
    }

    public Slot toSlot(int x, int y) {
        return new Slot(inventory, slot, x, y);
    }
}
