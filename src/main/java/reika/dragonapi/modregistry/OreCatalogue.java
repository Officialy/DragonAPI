package reika.dragonapi.modregistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** One immutable generation of ore and declared-product tag associations. No stacks are built at tag binding time. */
final class OreCatalogue {
    record Definition(ModOreList ore, List<String> names, String product) {
        Definition { names = List.copyOf(names); }
    }

    private final Map<ModOreList, List<Item>> blocks;
    private final Map<ModOreList, List<Item>> products;
    private final Map<Item, List<ModOreList>> byBlock;
    private final Map<Item, List<ModOreList>> byProduct;
    private final Map<String, List<ModOreList>> byName;

    private OreCatalogue(Map<ModOreList, List<Item>> blocks, Map<ModOreList, List<Item>> products,
            Map<Item, List<ModOreList>> byBlock, Map<Item, List<ModOreList>> byProduct,
            Map<String, List<ModOreList>> byName) {
        this.blocks = freeze(blocks); this.products = freeze(products);
        this.byBlock = freeze(byBlock); this.byProduct = freeze(byProduct); this.byName = freeze(byName);
    }

    static OreCatalogue build(List<Definition> definitions, Function<String, List<Item>> members) {
        var blocks = new EnumMap<ModOreList, List<Item>>(ModOreList.class);
        var products = new EnumMap<ModOreList, List<Item>>(ModOreList.class);
        var byBlock = new IdentityHashMap<Item, List<ModOreList>>();
        var byProduct = new IdentityHashMap<Item, List<ModOreList>>();
        var byName = new HashMap<String, List<ModOreList>>();
        // The builder owns ordering, even if a caller supplies definitions in another order.
        for (var definition : definitions.stream().sorted(java.util.Comparator.comparingInt(d -> d.ore().ordinal())).toList()) {
            var ore = definition.ore();
            var oreItems = new LinkedHashSet<Item>();
            for (var name : definition.names()) {
                add(byName, name, ore);
                members.apply(name).stream().filter(BlockItem.class::isInstance).forEach(oreItems::add);
            }
            blocks.put(ore, List.copyOf(oreItems));
            oreItems.forEach(item -> add(byBlock, item, ore));
            var productItems = List.copyOf(new LinkedHashSet<>(members.apply(definition.product())));
            products.put(ore, productItems);
            productItems.forEach(item -> add(byProduct, item, ore));
        }
        return new OreCatalogue(blocks, products, byBlock, byProduct, byName);
    }

    private static <K> void add(Map<K, List<ModOreList>> map, K key, ModOreList ore) {
        var entries = map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!entries.contains(ore)) entries.add(ore);
    }

    private static <K, V> Map<K, List<V>> freeze(Map<K, List<V>> map) {
        map.replaceAll((key, value) -> List.copyOf(value));
        return Collections.unmodifiableMap(map);
    }

    List<Item> blocks(ModOreList ore) { return blocks.getOrDefault(ore, List.of()); }
    List<Item> products(ModOreList ore) { return products.getOrDefault(ore, List.of()); }
    List<ModOreList> ores(Item item) { return byBlock.getOrDefault(item, List.of()); }
    List<ModOreList> names(String name) { return byName.getOrDefault(name, List.of()); }
    List<ModOreList> drops(Item item) {
        // Preserve the original ore-block lookup as well as restoring declared product lookup.
        var result = new LinkedHashSet<>(byProduct.getOrDefault(item, List.of()));
        result.addAll(ores(item));
        return result.stream().sorted(java.util.Comparator.comparingInt(ModOreList::ordinal)).toList();
    }
}
