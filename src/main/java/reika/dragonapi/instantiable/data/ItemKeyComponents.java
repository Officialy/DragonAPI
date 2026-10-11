package reika.dragonapi.instantiable.data;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.ItemStack;

/** Explicit roles for components that replace source item subtypes; registration precedes use of the component. */
public final class ItemKeyComponents {
    private static final Map<DataComponentType<?>, Object> VARIANTS = new IdentityHashMap<>();

    /** Return the same type for use directly inside a deferred component registration supplier.
     * A null default means absence is distinct; otherwise absent/default values are the same subtype. */
    public static synchronized <T> DataComponentType<T> variant(DataComponentType<T> type, T defaultValue) {
        Objects.requireNonNull(type);
        if (type == DataComponents.DAMAGE || type == DataComponents.POTION_CONTENTS)
            throw new IllegalArgumentException("Durability and potion fields have dedicated matching roles");
        if (VARIANTS.containsKey(type) && !Objects.equals(VARIANTS.get(type), defaultValue))
            throw new IllegalArgumentException("A component's subtype default cannot change");
        VARIANTS.put(type, defaultValue);
        return type;
    }

    record Parts(DataComponentMap variants, DataComponentMap additional) {}

    static Parts split(ItemStack stack) {
        var parts = split(stack.getComponents());
        if (!(stack.getItem() instanceof reika.dragonapi.interfaces.ItemVariantProvider provider)) return parts;
        var owned = provider.getKeyVariantComponents(stack);
        return new Parts(DataComponentMap.builder().addAll(parts.variants()).addAll(owned).build(),
                DataComponentMap.builder().addAll(parts.additional().filter(type -> !owned.has(type))).build());
    }

    /** Source ItemMatch derives its extra-data policy from the template, not from subtype patches. */
    public static boolean hasAdditionalChanges(ItemStack stack) {
        return !split(stack).additional().equals(split(new ItemStack(stack.getItem())).additional());
    }

    static synchronized Parts split(DataComponentMap components) {
        var variants = DataComponentMap.builder();
        var additional = DataComponentMap.builder();
        for (var component : components) {
            var type = component.type();
            if (type == DataComponents.DAMAGE) continue;
            if (type == DataComponents.POTION_CONTENTS) {
                var potion = components.get(DataComponents.POTION_CONTENTS);
                variants.set(DataComponents.POTION_CONTENTS, new PotionContents(potion.potion(), Optional.empty(), java.util.List.of(), Optional.empty()));
                var extra = new PotionContents(Optional.empty(), potion.customColor(), potion.customEffects(), potion.customName());
                if (!extra.equals(PotionContents.EMPTY)) additional.set(DataComponents.POTION_CONTENTS, extra);
            } else if (type == DataComponents.MAP_ID || type == DataComponents.BLOCK_STATE || VARIANTS.containsKey(type)) {
                if (!VARIANTS.containsKey(type) || !Objects.equals(component.value(), VARIANTS.get(type)))
                    add(variants, component);
            } else add(additional, component);
        }
        return new Parts(variants.build(), additional.build());
    }

    private static <T> void add(DataComponentMap.Builder builder, TypedDataComponent<T> component) {
        builder.set(component.type(), component.value());
    }

    private ItemKeyComponents() {}
}
