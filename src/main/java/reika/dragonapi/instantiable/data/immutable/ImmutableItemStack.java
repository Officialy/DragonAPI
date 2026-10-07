package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.world.item.ItemStack;

/**
 * Immutable wrapper around ItemStack for use as map keys and in collections.
 */
public final class ImmutableItemStack {
    private final ItemStack stack;
    
    public ImmutableItemStack(ItemStack stack) {
        this.stack = stack != null ? stack.copy() : ItemStack.EMPTY;
    }
    
    public ItemStack getItemStack() {
        return stack.copy();
    }
    
    public ItemStack getItemStackReference() {
        return stack.copy();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImmutableItemStack that = (ImmutableItemStack) obj;
        return ItemStack.isSameItemSameComponents(stack, that.stack);
    }
    
    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
    
    @Override
    public String toString() {
        return "ImmutableItemStack{" + stack + "}";
    }
}

