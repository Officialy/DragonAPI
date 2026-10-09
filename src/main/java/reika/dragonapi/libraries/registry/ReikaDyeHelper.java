package reika.dragonapi.libraries.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import reika.dragonapi.DragonAPI;

/**
 * Helper enum/class for the 16 vanilla dye colors.
 * Each constant carries its 24-bit RGB and matching DyeColor for cross-API use.
 */
public enum ReikaDyeHelper {
    BLACK(0x191919, DyeColor.BLACK),
    RED(0x993333, DyeColor.RED),
    GREEN(0x667F33, DyeColor.GREEN),
    BROWN(0x664C33, DyeColor.BROWN),
    BLUE(0x334CB2, DyeColor.BLUE),
    PURPLE(0x7F3FB2, DyeColor.PURPLE),
    CYAN(0x4C7F99, DyeColor.CYAN),
    LIGHTGRAY(0x999999, DyeColor.LIGHT_GRAY),
    GRAY(0x4C4C4C, DyeColor.GRAY),
    PINK(0xF27FA5, DyeColor.PINK),
    LIME(0x7FCC19, DyeColor.LIME),
    YELLOW(0xE5E533, DyeColor.YELLOW),
    LIGHTBLUE(0x6699D8, DyeColor.LIGHT_BLUE),
    MAGENTA(0xB24CD8, DyeColor.MAGENTA),
    ORANGE(0xD87F33, DyeColor.ORANGE),
    WHITE(0xFFFFFF, DyeColor.WHITE);

    public static final ReikaDyeHelper[] dyes = values();

    public final int color;
    public final DyeColor dye;
    public final String colorName;
    public final String colorNameNoSpaces;
    private static volatile Map<Item, List<ReikaDyeHelper>> itemColors;

    ReikaDyeHelper(int rgb, DyeColor dye) {
        this.color = rgb;
        this.dye = dye;
        colorName = switch (dye) {
            case LIGHT_GRAY -> "Light Gray";
            case LIGHT_BLUE -> "Light Blue";
            default -> name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
        };
        colorNameNoSpaces = colorName.replace(" ", "");
    }

    public int getColor() {
        return color;
    }

    /**
     * V33a {@code getJavaColor}. Dye-coloured blocks (dye leaves, saplings, flowers, vines) tint with
     * {@code getJavaColor().brighter()}, AWT's brightening, so this has to be an AWT colour.
     */
    public java.awt.Color getJavaColor() {
        return new java.awt.Color(color);
    }

    public DyeColor getDye() {
        return dye;
    }

    public static ReikaDyeHelper getFromDye(DyeColor dye) {
        for (ReikaDyeHelper r : dyes) {
            if (r.dye == dye) return r;
        }
        return null;
    }

    public static ReikaDyeHelper getByName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT).replace(" ", ""));
    }

    /** Original dye damage order; native DyeColor IDs run in the opposite direction. */
    public static ReikaDyeHelper getColorFromDamage(int damage) {
        return damage >= 0 && damage < dyes.length ? dyes[damage] : BLACK;
    }

    public int getDamage() { return ordinal(); }
    public int getWoolMeta() { return 15 - ordinal(); }
    public ItemStack getStackOf() { return new ItemStack(Items.DYE.pick(dye)); }
    public ItemStack getWoolStack() { return new ItemStack(Blocks.WOOL.pick(dye)); }
    public String getOreDictName() { return "dye" + colorNameNoSpaces; }

    public static boolean isDyeItem(ItemStack stack) {
        return getColorsFromItem(stack) != null;
    }

    public static ReikaDyeHelper getColorFromItem(ItemStack stack) {
        var colors = getColorsFromItem(stack);
        return colors == null ? null : colors.iterator().next();
    }

    /** Typed DYE replaces legacy metadata; unrelated components and stack count do not affect tag identity. */
    public static Collection<ReikaDyeHelper> getColorsFromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var cache = itemColors;
        if (cache == null) {
            buildItemCache();
            cache = itemColors;
        }
        List<ReikaDyeHelper> tagged = cache.getOrDefault(stack.getItem(), List.of());
        DyeColor component = stack.get(DataComponents.DYE);
        if (component == null) return tagged.isEmpty() ? null : tagged;
        ReikaDyeHelper primary = getFromDye(component);
        var result = new ArrayList<ReikaDyeHelper>();
        result.add(primary);
        for (var color : tagged) if (color != primary) result.add(color);
        return List.copyOf(result);
    }

    /** Manual initialization remains supported; tag events supply the authoritative registry lookup. */
    public static void buildItemCache() {
        buildItemCache(BuiltInRegistries.ITEM);
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData())
            buildItemCache(event.getRegistries().lookupOrThrow(Registries.ITEM));
    }

    private static synchronized void buildItemCache(HolderLookup.RegistryLookup<Item> registry) {
        var mutable = new IdentityHashMap<Item, List<ReikaDyeHelper>>();
        for (var color : dyes) {
            registry.get(color.dye.getTag()).ifPresent(tag -> {
                for (var item : tag)
                    mutable.computeIfAbsent(item.value(), ignored -> new ArrayList<>()).add(color);
            });
        }
        var immutable = new IdentityHashMap<Item, List<ReikaDyeHelper>>();
        mutable.forEach((item, colors) -> immutable.put(item, List.copyOf(colors)));
        itemColors = java.util.Collections.unmodifiableMap(immutable);
        DragonAPI.LOGGER.debug("Built dye item cache for {} tagged items", itemColors.size());
    }
}
