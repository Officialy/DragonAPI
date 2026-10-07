package reika.dragonapi.libraries;

import net.neoforged.fml.loading.FMLEnvironment;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

public class ReikaEnchantmentHelper {
    public static final Comparator<Holder<Enchantment>> enchantmentNameSorter = new EnchantmentNameComparator();
    public static final Comparator<Holder<Enchantment>> enchantmentTypeSorter = new EnchantmentWeightComparator();

    /** Get a listing of all enchantments on an ItemStack. Args: ItemStack */
    public static HashMap<Holder<Enchantment>, Integer> getEnchantments(ItemStack is) {
        ItemEnchantments enchants = is.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty())
            return null;
        HashMap<Holder<Enchantment>, Integer> ench = new HashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            ench.put(entry.getKey(), entry.getIntValue());
        }
        return ench;
    }

    public static void applyEnchantment(ItemStack is, Holder<Enchantment> e, int level) {
        is.enchant(e, level);
    }

    /** Applies all enchantments to an ItemStack. Args: ItemStack, enchantment map */
    public static void applyEnchantments(ItemStack is, Map<Holder<Enchantment>, Integer> en) {
        if (en == null)
            return;
        for (Holder<Enchantment> e : en.keySet()) {
            int level = en.get(e);
            if (level > 0) {
                applyEnchantment(is, e, level);
            }
        }
    }

    /** Strips a single enchantment from a stack via the component system. Args: ItemStack, enchantment key */
    public static void removeEnchantment(ItemStack is, ResourceKey<Enchantment> key) {
        if (is == null || key == null)
            return;
        EnchantmentHelper.updateEnchantments(is, m -> m.removeIf(h -> h.is(key)));
    }

    /** Returns the enchantment level of an ItemStack. Args: Enchantment, ItemStack */
    public static int getEnchantmentLevel(Holder<Enchantment> e, ItemStack is) {
        if (is == null)
            return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(e, is);
    }

    /** Test whether an ItemStack has an enchantment. Args: Enchantment, ItemStack */
    public static boolean hasEnchantment(Holder<Enchantment> e, ItemStack is) {
        if (is == null)
            return false;
        return getEnchantmentLevel(e, is) > 0;
    }

    /** Resolve a ResourceKey<Enchantment> to a Holder<Enchantment> using the client/server registry access. Returns null if unavailable. */
    public static Holder<Enchantment> resolve(ResourceKey<Enchantment> key) {
        if (key == null) return null;
        HolderLookup.Provider provider = null;
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) provider = server.registryAccess();
        } catch (Throwable ignored) {}
        if (provider == null) {
            try {
                if (FMLEnvironment.getDist().isClient()
                        && reika.dragonapi.client.ClientEnvironment.level() != null)
                    provider = reika.dragonapi.client.ClientEnvironment.level().registryAccess();
            } catch (Throwable ignored) {}
        }
        if (provider == null) return null;
        return provider.lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
    }

    /** Convenience overloads that accept ResourceKey<Enchantment> (the new 1.21.5 vanilla Enchantments.X constants). */
    public static boolean hasEnchantment(ResourceKey<Enchantment> key, ItemStack is) {
        Holder<Enchantment> h = resolve(key);
        return h != null && hasEnchantment(h, is);
    }

    public static int getEnchantmentLevel(ResourceKey<Enchantment> key, ItemStack is) {
        Holder<Enchantment> h = resolve(key);
        return h == null ? 0 : getEnchantmentLevel(h, is);
    }

    public static int getEnchantmentLevel(ResourceKey<Enchantment> key, Entity entity) {
        if (entity instanceof LivingEntity le) {
            // Probe main hand for now; callers that need slot-specific lookups should call directly.
            return getEnchantmentLevel(key, le.getMainHandItem());
        }
        return 0;
    }

    public static void applyEnchantment(ItemStack is, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> h = resolve(key);
        if (h != null) applyEnchantment(is, h, level);
    }

    /** Returns the speed bonus that efficiency that gives. Args: Level */
    public static float getEfficiencyMultiplier(int level) {
        return (float)Math.pow(1.3, level);
    }

    /** Returns true iff all the enchantments are compatible with each other. */
    public static boolean areCompatible(Collection<Holder<Enchantment>> enchantments) {
        List<Holder<Enchantment>> list = new ArrayList<>(enchantments);
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (!areEnchantsCompatible(list.get(i), list.get(j)))
                    return false;
            }
        }
        return true;
    }

    /** Returns true iff the new enchantment is compatible with all the other enchantments. */
    public static boolean isCompatible(Collection<Holder<Enchantment>> enchantments, Holder<Enchantment> addition) {
        for (Holder<Enchantment> e : enchantments) {
            if (!areEnchantsCompatible(e, addition))
                return false;
        }
        return true;
    }

    public static boolean areEnchantsCompatible(Holder<Enchantment> e, Holder<Enchantment> e2) {
        return Enchantment.areCompatible(e, e2);
    }

    public static boolean hasEnchantments(ItemStack is) {
        return EnchantmentHelper.hasAnyEnchantments(is);
    }

    /** Comparator that sorts by enchantment weight (replaces old category-based sorting). */
    private static class EnchantmentWeightComparator implements Comparator<Holder<Enchantment>> {

        @Override
        public int compare(Holder<Enchantment> o1, Holder<Enchantment> o2) {
            return Integer.compare(o1.value().getWeight(), o2.value().getWeight());
        }

    }

    private static class EnchantmentNameComparator implements Comparator<Holder<Enchantment>> {

        @Override
        public int compare(Holder<Enchantment> o1, Holder<Enchantment> o2) {
            String name1 = o1.getRegisteredName();
            String name2 = o2.getRegisteredName();
            return name1.compareTo(name2);
        }

    }


    /**
     * 1.7.10 vanilla enchantment ids (the Enchantment.enchantmentsList index), read from the decompiled 1.7.10
     * Enchantment class, for scripts and data written against numeric ids. Enchantments are registry entries now.
     */
    private static final Map<Integer, ResourceKey<Enchantment>> LEGACY_IDS = Map.ofEntries(
            Map.entry(0, net.minecraft.world.item.enchantment.Enchantments.PROTECTION),
            Map.entry(1, net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION),
            Map.entry(2, net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING),
            Map.entry(3, net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION),
            Map.entry(4, net.minecraft.world.item.enchantment.Enchantments.PROJECTILE_PROTECTION),
            Map.entry(5, net.minecraft.world.item.enchantment.Enchantments.RESPIRATION),
            Map.entry(6, net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY),
            Map.entry(7, net.minecraft.world.item.enchantment.Enchantments.THORNS),
            Map.entry(16, net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),
            Map.entry(17, net.minecraft.world.item.enchantment.Enchantments.SMITE),
            Map.entry(18, net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS),
            Map.entry(19, net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK),
            Map.entry(20, net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT),
            Map.entry(21, net.minecraft.world.item.enchantment.Enchantments.LOOTING),
            Map.entry(32, net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY),
            Map.entry(33, net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
            Map.entry(34, net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),
            Map.entry(35, net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
            Map.entry(48, net.minecraft.world.item.enchantment.Enchantments.POWER),
            Map.entry(49, net.minecraft.world.item.enchantment.Enchantments.PUNCH),
            Map.entry(50, net.minecraft.world.item.enchantment.Enchantments.FLAME),
            Map.entry(51, net.minecraft.world.item.enchantment.Enchantments.INFINITY),
            Map.entry(61, net.minecraft.world.item.enchantment.Enchantments.LUCK_OF_THE_SEA),
            Map.entry(62, net.minecraft.world.item.enchantment.Enchantments.LURE));

    /** The enchantment a 1.7.10 numeric id named, or null if it named none (or a mod's long-gone one). */
    public static ResourceKey<Enchantment> getLegacyEnchantment(int id) {
        return LEGACY_IDS.get(id);
    }
}
