package reika.dragonapi.libraries;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ReikaEnchantmentHelper {
    public static final Comparator<Enchantment> enchantmentNameSorter = new EnchantmentNameComparator();
    public static final Comparator<Enchantment> enchantmentTypeSorter = new EnchantmentTypeComparator();

    /** Get a listing of all enchantments on an ItemStack. Args: ItemStack */
    public static HashMap<Enchantment,Integer> getEnchantments(ItemStack is) {
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(is);
        if (enchants == null)
            return null;
        HashMap<Enchantment, Integer> ench = new HashMap<>();
        for (Enchantment id : enchants.keySet()) {
            Enchantment e = Enchantment.byId(id.category.ordinal()); //was enchantmentsList todo this is for sure wrong lol
            int level = enchants.get(id);
            ench.put(e, level);
        }
        return ench;
    }

    public static void applyEnchantment(ItemStack is, Enchantment e, int level) {
        if (is.getItem() == Items.ENCHANTED_BOOK) {
            //Items.ENCHANTED_BOOK.addEnchantment(is, new EnchantmentData(e, level));
        }
        else {
            is.enchant(e, level);
        }
    }

    /** Applies all enchantments to an ItemStack. Args: ItemStack, enchantment map */
    public static void applyEnchantments(ItemStack is, Map<Enchantment,Integer> en) {
        if (en == null)
            return;
        for (Enchantment e : en.keySet()) {
            int level = en.get(e);
            if (level > 0) {
                applyEnchantment(is, e, level);
            }
        }
    }

    /** Returns the enchantment level of an ItemStack. Args: Enchantment, ItemStack */
    public static int getEnchantmentLevel(Enchantment e, ItemStack is) {
        if (is == null)
            return 0;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(is);
        if (enchants == null)
            return 0;
        if (enchants.containsKey(e)) {
            int level = enchants.get(e);
            return level;
        }
        return 0;
    }

    /** Test whether an ItemStack has an enchantment. Args: Enchantment, ItemStack */
    public static boolean hasEnchantment(Enchantment e, ItemStack is) {
        if (is == null)
            return false;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(is);
        if (enchants == null)
            return false;
        return enchants.containsKey(e); //.getId()
    }

    /** Returns the speed bonus that efficiency that gives. Args: Level */
    public static float getEfficiencyMultiplier(int level) {
        return (float)Math.pow(1.3, level);
    }

    /** Returns true iff all the enchantments are compatible with each other. */
    public static boolean areCompatible(Collection<Enchantment> enchantments) {
        Iterator<Enchantment> it = enchantments.iterator();
        Iterator<Enchantment> it2 = enchantments.iterator();
        while (it.hasNext()) {
            Enchantment e = it.next();
            while (it2.hasNext()) {
                Enchantment e2 = it2.next();
                if (!areEnchantsCompatible(e, e2))
                    return false;
            }
        }
        return true;
    }

    /** Returns true iff the new enchantment is compatible with all the other enchantments. */
    public static boolean isCompatible(Collection<Enchantment> enchantments, Enchantment addition) {
        Iterator<Enchantment> it = enchantments.iterator();
        Iterator<Enchantment> it2 = enchantments.iterator();
        while (it.hasNext()) {
            Enchantment e = it.next();
            if (!areEnchantsCompatible(e, addition))
                return false;
        }
        return true;
    }

    public static boolean areEnchantsCompatible(Enchantment e, Enchantment e2) {
        return e.isCompatibleWith(e2);
    }

    public static boolean hasEnchantments(ItemStack is) {
        Map map = EnchantmentHelper.getEnchantments(is);
        return map != null && !map.isEmpty();
    }

    private static class EnchantmentTypeComparator implements Comparator<Enchantment> {

        @Override
        public int compare(Enchantment o1, Enchantment o2) {
            return o1.category.ordinal()-o2.category.ordinal(); //todo was type, i bet this is wrong
        }

    }

    private static class EnchantmentNameComparator implements Comparator<Enchantment> {

        @Override
        public int compare(Enchantment o1, Enchantment o2) {
            return ForgeRegistries.ENCHANTMENTS.getKey(o1).getNamespace().compareTo(ForgeRegistries.ENCHANTMENTS.getKey(o2).getNamespace());
        }

    }

}
