package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

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
        return stack;
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
        int hash = stack.getItem().hashCode() * 31;
        if (!stack.isEmpty() && !stack.getComponentsPatch().isEmpty()) {
            // Use VanillaRegistries to get a HolderLookup.Provider for serialization
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, VanillaRegistries.createLookup());
            output.store("item", ItemStack.CODEC, stack);
            hash += output.buildResult().hashCode();
        }
        return hash;
    }
    
    @Override
    public String toString() {
        return "ImmutableItemStack{" + stack + "}";
    }
}

