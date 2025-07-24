package reika.dragonapi.libraries;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities to deeply compare Ingredient collections with full component awareness.
 */
public class ReikaIngredientHelper {

    // Hard cap to prevent pathological explosions when enumerating variants
    private static final int HARD_CAP_PER_INGREDIENT = 4096;

    // Cache to avoid re-expanding the same ingredient repeatedly
    private static final Map<Ingredient, List<ItemStack>> EXPANSION_CACHE = new ConcurrentHashMap<>();

    // Register your enumerators here (Potion variants, trims, your custom component variants, …)
    private static final List<ComponentEnumerator> ENUMERATORS = new ArrayList<>();

    static {
        // Minimal default enumerator: just give back the stack you pass in
        ENUMERATORS.add((item, base) -> List.of(base));

        // Example (disabled): if you later want to enumerate Potion variants, register one here.
        // ENUMERATORS.add(new PotionEnumerator());
    }

    private ReikaIngredientHelper() {}

    public static boolean matchStackCollections(NonNullList<Ingredient> c1,
                                                NonNullList<Ingredient> c2,
                                                RegistryAccess registries) {
        if (c1.size() != c2.size())
            return false;

        for (int i = 0; i < c1.size(); i++) {
            if (!ingredientsEquivalentDeep(c1.get(i), c2.get(i), registries)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deeply compares two Ingredients, considering **components**.
     *
     * Algorithm (bounded but precise enough in practice):
     *  1) Expand each Ingredient into a (capped) Set of representative ItemStacks (including component variants).
     *  2) Compare those sets with ItemStack.isSameItemSameComponents.
     */
    public static boolean ingredientsEquivalentDeep(Ingredient a,
                                                    Ingredient b,
                                                    RegistryAccess registries) {
        List<ItemStack> la = expandIngredient(a, registries);
        List<ItemStack> lb = expandIngredient(b, registries);

        if (la.size() != lb.size())
            return false;

        // Convert to canonical multiset using a stable fingerprint
        Map<Fingerprint, Long> ma = la.stream()
                .collect(Collectors.groupingBy(Fingerprint::of, Collectors.counting()));
        Map<Fingerprint, Long> mb = lb.stream()
                .collect(Collectors.groupingBy(Fingerprint::of, Collectors.counting()));

        return ma.equals(mb);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Expansion
    // ─────────────────────────────────────────────────────────────────────────────

    private static List<ItemStack> expandIngredient(Ingredient ing, RegistryAccess registries) {
        return EXPANSION_CACHE.computeIfAbsent(ing, i -> {
            Set<Fingerprint> seen = new HashSet<>();
            List<ItemStack> out = new ArrayList<>();

            ICustomIngredient custom = ing.getCustomIngredient();
            if (custom != null) {
                // We only get Items, no pre-built stacks -> make default stacks and fan out through enumerators.
                custom.items().forEach(holder -> {
                    Item item = holder.value();
                    addExpandedVariants(ing, item, out, seen);
                });
            } else {
                // Vanilla: we need to brute-force through the registry and pick everything that passes test()
                for (Item item : BuiltInRegistries.ITEM) {
                    if (out.size() >= HARD_CAP_PER_INGREDIENT)
                        break;
                    addExpandedVariants(ing, item, out, seen);
                }
            }

            // Final safety cap
            if (out.size() > HARD_CAP_PER_INGREDIENT) {
                return out.subList(0, HARD_CAP_PER_INGREDIENT);
            }
            return List.copyOf(out);
        });
    }

    private static void addExpandedVariants(Ingredient ing, Item item, List<ItemStack> out, Set<Fingerprint> seen) {
        ItemStack base = new ItemStack(item);

        // Fast reject: if the base stack already fails, variants might still pass (components!),
        // so we still enumerate variants. If you *know* your ingredients never filter by components,
        // you can early-return here to speed up.
        for (ItemStack variant : enumerateVariants(item, base)) {
            if (ing.test(variant)) {
                Fingerprint fp = Fingerprint.of(variant);
                if (seen.add(fp)) {
                    out.add(variant);
                    if (out.size() >= HARD_CAP_PER_INGREDIENT)
                        return;
                }
            }
        }
    }

    /**
     * Enumerate "interesting" variants for a given item using all registered enumerators.
     * Start with the passed base stack, let enumerators fan out.
     */
    private static List<ItemStack> enumerateVariants(Item item, ItemStack base) {
        List<ItemStack> current = List.of(base);
        for (ComponentEnumerator e : ENUMERATORS) {
            // Expand each current into variants
            List<ItemStack> next = new ArrayList<>();
            for (ItemStack s : current) {
                next.addAll(e.enumerate(item, s));
            }
            current = next;
            if (current.size() >= HARD_CAP_PER_INGREDIENT) {
                // Hard stop; we’ll trim later
                break;
            }
        }
        return current.size() > HARD_CAP_PER_INGREDIENT
                ? current.subList(0, HARD_CAP_PER_INGREDIENT)
                : current;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Stable key over (item + full component map). Uses the same equality semantics
     * as {@link ItemStack#isSameItemSameComponents(ItemStack, ItemStack)}.
     */
    private record Fingerprint(ResourceLocation itemId, DataComponentMap components) {
        static Fingerprint of(ItemStack s) {
            return new Fingerprint(BuiltInRegistries.ITEM.getKey(s.getItem()), s.getComponents());
        }
    }

    /**
     * Plug points to enumerate “component-rich” variants for a given item.
     * For example, potions, trimmed armor, custom-component-carrying stacks, etc.
     *
     * Implementations should:
     * - Always include the passed-in stack if it is valid.
     * - Return a *small* list (keep it bounded).
     */
    @FunctionalInterface
    public interface ComponentEnumerator {
        List<ItemStack> enumerate(Item item, ItemStack base);
    }
}
