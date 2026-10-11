package reika.dragonapi.modregistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Translation of the retained source catalogue names into native common item tags. */
final class OreCatalogueTags {
    static List<TagKey<Item>> resolve(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("An ore tag name must not be empty");
        if (name.contains(":")) {
            var id = Identifier.tryParse(name);
            if (id == null) throw new IllegalArgumentException("Invalid item tag identifier: " + name);
            return List.of(tag(id));
        }
        var paths = new LinkedHashSet<String>();
        switch (name) {
            case "MonazitOre" -> paths.add("ores/monazit_ore");
            case "ForciciumItem" -> paths.add("items/forcicium_item");
            default -> {
                if (!name.matches("[a-z]+[A-Z][A-Za-z0-9]*"))
                    throw new IllegalArgumentException("Use a namespaced tag or a legacy category/material name: " + name);
                int prefix = 0;
                while (prefix < name.length() && Character.isLowerCase(name.charAt(prefix))) prefix++;
                if (prefix == 0 || prefix == name.length())
                    throw new IllegalArgumentException("Use a namespaced tag or a legacy category/material name: " + name);
                String category = name.substring(0, prefix) + "s/";
                String material = name.substring(prefix);
                paths.add(category + material.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                        .replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
                // Compatibility with the port's previously accepted flat translations.
                paths.add(category + material.toLowerCase(Locale.ROOT));
            }
        }
        if (name.equals("crystalCertusQuartz")) paths.add("gems/certus_quartz");
        if (name.equals("oreChargedCertusQuartz")) paths.add("ores/certus_quartz_charged");
        // Historical blue dye included lapis; the native gem tag preserves that association.
        if (name.equals("dyeBlue")) paths.add("gems/lapis");
        return paths.stream().map(path -> tag(Identifier.fromNamespaceAndPath("c", path))).toList();
    }

    private static TagKey<Item> tag(Identifier id) { return TagKey.create(Registries.ITEM, id); }
}
