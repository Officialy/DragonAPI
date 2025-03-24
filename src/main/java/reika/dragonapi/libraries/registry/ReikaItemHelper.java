package reika.dragonapi.libraries.registry;

import com.google.common.base.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.ItemFilter;
import reika.dragonapi.instantiable.ItemMatch;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.immutable.ImmutableItemStack;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;

import java.util.*;

import static reika.dragonapi.DragonAPI.rand;

public class ReikaItemHelper {



    public static final Comparator<ItemStack> comparator = new ItemComparator();
    public static final Comparator<Object> itemListComparator = new ItemListComparator();

    public static boolean matchStacks(ImmutableItemStack a, ImmutableItemStack b) {
        return matchStacks(a.getItemStack(), b.getItemStack());
    }

    public static boolean matchStacks(ItemStack a, ImmutableItemStack b) {
        return matchStacks(a, b.getItemStack());
    }

    public static boolean matchStacks(ItemStack a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (b instanceof ItemStack) {
            return matchStacks(a, (ItemStack) b);
        } else if (b instanceof BlockKey bk) {
            return matchStackWithBlock(a, bk.blockID);
        } else if (b instanceof Collection) {
            return ReikaItemHelper.listContainsItemStack((Collection<ItemStack>) b, a, false);
        } else if (b instanceof ItemFilter) {
            return ((ItemFilter) b).matches(a);
        } else if (b instanceof ItemMatch) {
            return ((ItemMatch) b).match(a);
        }
        //else if (b instanceof FlexibleIngredient) {
        //    return ((FlexibleIngredient)b).match(a);
        //}
        else {
            return false;
        }
    }

    private static class ItemComparator implements Comparator<ItemStack> {

        @Override
        public int compare(ItemStack o1, ItemStack o2) {
            if (o1.getItem() == o2.getItem()) {
                if (o1.getDamageValue() == o2.getDamageValue()) {
                    if (o1.getCount() == o2.getCount()) {
                        if (o1.getTag() == o2.getTag() || (o1.getTag() != null && o1.getTag().equals(o2.getTag()))) {
                            return 0;
                        }
                        else {
                            if (o1.getTag() == null && o2.getTag() != null) {
                                return -1;
                            }
                            else if (o2.getTag() == null && o1.getTag() != null) {
                                return 1;
                            }
                            else {
                                return ReikaNBTHelper.compareNBTTags(o1.getTag(), o2.getTag());
                            }
                        }
                    }
                    else {
                        return Integer.compare(o2.getCount(), o1.getCount());
                    }
                }
                else {
                    return Integer.compare(o1.getDamageValue(), o2.getDamageValue());
                }
            }
            else {
                return Integer.compare(Item.getId(o1.getItem()), Item.getId(o2.getItem()));
            }
        }

    }

    /** Suitable for either raw RotaryItems or lists thereof, like what is found inside an OreRecipe. */
    private static class ItemListComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof ItemStack) {
                if (o2 instanceof ItemStack) {
                    return comparator.compare((ItemStack)o1, (ItemStack)o2);
                }
                else {
                    return Integer.MIN_VALUE;
                }
            }
            else if (o2 instanceof ItemStack) {
                return Integer.MAX_VALUE;
            }
            List<ItemStack> l1 = (List<ItemStack>)o1;
            List<ItemStack> l2 = (List<ItemStack>)o2;
            int fast = Integer.compare(l1.size(), l2.size());
            if (fast != 0)
                return fast;
            ArrayList<ItemStack> li1 = new ArrayList(l1);
            ArrayList<ItemStack> li2 = new ArrayList(l2);
            if (li1.size() > 1) {
                Collections.sort(li1, comparator);
                Collections.sort(li2, comparator);
            }
            for (int i = 0; i < li1.size(); i++) { //must be same size
                ItemStack is1 = li1.get(i);
                ItemStack is2 = li2.get(i);
                int get = comparator.compare(is1, is2);
                if (get != 0)
                    return get;
            }
            return 0;
        }

    }
    public static ItemStack lookupItem(String s) {
        if (Strings.isNullOrEmpty(s))
            throw new IllegalArgumentException("Invalid item string lookup os null or empty");
        String[] parts = s.split(":");
        /*if (parts.length == 3) {
            try {
                m = parts[2].equalsIgnoreCase("*") ? OreDictionary.WILDCARD_VALUE : Integer.parseInt(parts[2]);
            }
            catch (NumberFormatException e) {

            }
        }*/
        return lookupItem(parts[0], parts[1]);
    }

    public static ItemStack lookupItem(ModList mod, String s) {
        return lookupItem(mod.name(), s);
    }

    public static ItemStack lookupItem(String mod, String item) {
        Item i = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(mod, item));
        return i != null ? new ItemStack(i, 1) : null;
    }

    /**
     * Pass int.max to ignore stack limits
     */
    public static boolean areStacksCombinable(ItemStack is1, ItemStack is2, int limit) {
        if (is1 != null && limit != Integer.MAX_VALUE)
            limit = Math.min(limit, is1.getMaxStackSize());
        return is1 != null && is2 != null && matchStacks(is1, is2) && ItemStack.isSameItemSameTags(is1, is2) && is1.getCount() + is2.getCount() <= limit;
    }

    public static boolean verifyItemStack(ItemStack is, boolean fullCheck) {
        if (is == null)
            return true;
        if (is.getItem() == null)
            return false;
        try {
            is.toString();
            if (fullCheck)
                is.getDisplayName();
        } catch (Exception e) {
            if (ReikaObfuscationHelper.isDeObfEnvironment())
                e.printStackTrace();
            return false;
        }
        return true;
    }

    public static int getIndexOf(List<?> li, ItemStack is) {
        for (int i = 0; i < li.size(); i++) {
            Object o = li.get(i);
            if (o instanceof ItemStack && matchStacks((ItemStack) o, is))
                return i;
        }
        return -1;
    }

    public static ItemStack parseItem(Object o) {
        if (o instanceof ItemStack) {
            return ((ItemStack) o).copy();
        }
//        else if (o instanceof String) {
//            return lookupItem((String)o);
//        }
        else if (o instanceof BlockKey) {
            return ((BlockKey) o).asItemStack();
        }
        return null;
    }

    public static ArrayList<ItemStack> collateItemList(Collection<ItemStack> c) {
        if (c.size() <= 1)
            return new ArrayList<>(c);
        ArrayList<ItemStack> li = new ArrayList<>();
        HashMap<KeyedItemStack, Integer> vals = new HashMap<>();
        for (ItemStack is : c) {
            KeyedItemStack ks = new KeyedItemStack(is).setSimpleHash(true).setIgnoreNBT(false);
            Integer get = vals.get(ks);
            int val = get != null ? get : 0;
            vals.put(ks, val + is.getCount());
        }
        for (Map.Entry<KeyedItemStack, Integer> e : vals.entrySet()) {
            KeyedItemStack is = e.getKey();
            Integer val = e.getValue();
            if (val == null) {
                DragonAPI.LOGGER.error("Item " + is + " was mapped to null!");
                continue;
            }
            while (val > 0) {
                int amt = Math.min(val, is.getItemStack().getMaxStackSize());
                ItemStack copy = getSizedItemStack(is.getItemStack(), amt);
                li.add(copy);
                val -= amt;
            }
        }
        return li;
    }

    public static boolean matchStackCollections(Collection<ItemStack> c1, Collection<ItemStack> c2) {
        if (c1.size() != c2.size())
            return false;
        ArrayList<ItemStack> li = new ArrayList<>(c1);
        ArrayList<ItemStack> li2 = new ArrayList<>(c2);
        for (int i = 0; i < li.size(); i++) {
            ItemStack o1 = li.get(i);
            ItemStack o2 = li2.get(i);
            if (!matchStacks(o1, o2))
                return false;
        }
        return true;
    }
    public static boolean matchStackCollections(NonNullList<Ingredient> c1, NonNullList<Ingredient> c2) {
        if (c1.size() != c2.size())
            return false;
        ArrayList<Ingredient> li = new ArrayList<>(c1);
        ArrayList<Ingredient> li2 = new ArrayList<>(c2);
        for (int i = 0; i < li.size(); i++) {
            Ingredient o1 = li.get(i);
            Ingredient o2 = li2.get(i);
            if (!matchStacks(o1.getItems()[i], o2.getItems()[i]))
                return false;
        }
        return true;
    }
/*
    public static ItemStack lookupItem(String s) {
        if (Strings.isNullOrEmpty(s))
            throw new IllegalArgumentException("Invalid item string lookup os null or empty");
        String[] parts = s.split(":");

        return lookupItem(parts[0], parts[1]);
    }

    public static ItemStack lookupItem(String mod, String item) {
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(mod, item);
        return i != null ? new ItemStack(i, 1) : null;
    }

    public static ItemStack lookupBlock(String mod, String s) {
        Block b = Block.getBlockFromName(mod+":"+s);
        return b != null ? new ItemStack(b, 1) : null;
    }*/

    /**
     * Like .equals for comparing ItemStacks, but does not care about size or NBT tags.
     * Returns true if the ids match (or both are null).
     * Args: ItemStacks a, b
     */
    public static boolean matchStacks(ItemStack a, ItemStack b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        if (a.getItem() == null || b.getItem() == null)
            return false;
        if (a.getItem() != b.getItem())
            return false;
        if (a.getItem() == b.getItem())
            return false;
        return false;
    }

    public static boolean isFireworkIngredient(Item id) {
        if (id == Items.DIAMOND)
            return true;
        if (id instanceof DyeItem)
            return true;
        if (id == Items.GLOWSTONE_DUST)
            return true;
        if (id == Items.FEATHER)
            return true;
        if (id == Items.GOLD_NUGGET)
            return true;
        if (id == Items.FIRE_CHARGE)
            return true;
        if (id == Items.CREEPER_HEAD)
            return true;
        if (id == Items.FIREWORK_STAR)
            return true;
        if (id == Items.PAPER)
            return true;
        return id == Items.GUNPOWDER;
    }

    public static ItemStack getSizedItemStack(ItemStack is, int num) {/*
		if (is == null)
			return null;
		if (is.getItem() == null)
			return null;
		if (num <= 0)
			return null;
		ItemStack is2 = new ItemStack(is.getItem(), num, is.getItemDamage());
		if (is.getTag() != null)
			is2.getTag() = (CompoundTag)is.getTag().copy();
		return is2;*/
        if (is == null || is.getItem() == null || num <= 0)
            return null;
        ItemStack is2 = is.copy();
        is2.setCount(num);
        return is2;
    }

    public static void dropInventory(Player ep) {
        Inventory ii = ep.getInventory();
        Random par5Random = new Random();
        if (ii != null) {
            label0:
            for (int i = 0; i < ii.getContainerSize(); i++) {
                ItemStack itemstack = ii.getItem(i);
                if (itemstack == null)
                    continue;
                float f = par5Random.nextFloat() * 0.8F + 0.1F;
                float f1 = par5Random.nextFloat() * 0.8F + 0.1F;
                float f2 = par5Random.nextFloat() * 0.8F + 0.1F;
                do {
                    if (itemstack.getCount() <= 0)
                        continue label0;
                    int j = par5Random.nextInt(21) + 10;
                    if (j > itemstack.getCount())
                        j = itemstack.getCount();
                    //itemstack.getCount() -= j;
                    int count = itemstack.getCount() - 1;
                    itemstack.setCount(count);
                    ItemEntity ei = new ItemEntity(ep.level(), ep.getX() + f, ep.getY() + 0.25 + f1, ep.getZ() + f2, new ItemStack(itemstack.getItem(), j));
                    if (itemstack.hasTag())
                        ei.getItem().save(itemstack.getTag().copy());
                    float f3 = 0.05F;
                    ei.setDeltaMovement((float) par5Random.nextGaussian() * f3, (float) par5Random.nextGaussian() * f3 + 0.2F, (float) par5Random.nextGaussian() * f3);
                    ei.setPickUpDelay(10);
                    ep.level().addFreshEntity(ei);
                }
                while (true);
            }
        }
    }

    public static void dropInventory(Level world, BlockPos pos) {
        Container ii = (Container) world.getBlockEntity(pos);
        Random par5Random = new Random();
        if (ii != null) {
            label0:
            for (int i = 0; i < ii.getContainerSize(); i++) {
                ItemStack itemstack = ii.getItem(i);
                if (itemstack == null)
                    continue;
                float f = par5Random.nextFloat() * 0.8F + 0.1F;
                float f1 = par5Random.nextFloat() * 0.8F + 0.1F;
                float f2 = par5Random.nextFloat() * 0.8F + 0.1F;
                do {
                    if (itemstack.getCount() <= 0)
                        continue label0;
                    int j = par5Random.nextInt(21) + 10;
                    if (j > itemstack.getCount())
                        j = itemstack.getCount();
                    int count = itemstack.getCount();
                    itemstack.setCount(count - j);
                    ItemEntity ei = new ItemEntity(world, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, new ItemStack(itemstack.getItem(), j));
                    if (itemstack.hasTag())
                        ei.getItem().save(itemstack.getTag().copy());
                    float f3 = 0.05F;
                    ei.setDeltaMovement((float) par5Random.nextGaussian() * f3, (float) par5Random.nextGaussian() * f3 + 0.2F, (float) par5Random.nextGaussian() * f3);
                    ei.setPickUpDelay(10);
                    world.addFreshEntity(ei);
                }
                while (true);
            }
        }
    }

    public static ItemEntity dropItem(Entity e, ItemStack is) {
        return dropItem(e.level(), e.blockPosition().getX(), e.blockPosition().getY(), e.blockPosition().getZ(), is);
    }

    public static ItemEntity dropItem(Level world, double x, double y, double z, ItemStack is) {
        return dropItem(world, x, y, z, is, 1);
    }

    public static ItemEntity dropItem(Level world, double x, double y, double z, ItemStack is, double vscale) {
        if (is == null)
            return null;
        ItemEntity ei = new ItemEntity(world, x, y, z, is.copy());
        ei.setPickUpDelay(10);
        ei.setDeltaMovement((-0.1 + 0.2 * rand.nextDouble()) * vscale, (0.2 * rand.nextDouble()) * vscale, (-0.1 + 0.2 * rand.nextDouble()) * vscale);
        if (!world.isClientSide()) {
            world.addFreshEntity(ei);
        }
        return ei;
    }

    public static void dropItems(Level world, double x, double y, double z, Collection<ItemStack> li) {
        for (ItemStack is : li)
            dropItem(world, x, y, z, is);
    }

    public static boolean isBlock(ItemStack is) {
        Block b = Block.byItem(is.getItem());
        return b != Blocks.AIR;
    }

    public static boolean collectionContainsItemStack(Collection<ItemStack> li, ItemStack is) {
        return listContainsItemStack(li, is, false);
    }

    public static boolean listContainsItemStack(Collection<ItemStack> li, ItemStack is, boolean NBT) {
        for (ItemStack is2 : li) {
            if (matchStacks(is, is2) && (!NBT || ItemStack.isSameItem(is, is2)))
                return true;
        }
        return false;
    }

    public static boolean matchStackWithBlock(ItemStack is, BlockState b) {
        return is.getItem() == Item.BY_BLOCK.get(b.getBlock());
    }

    public static BlockKey getWorldBlockFromItem(ItemStack is) {
        if (is == null)
            return new BlockKey(Blocks.AIR);
        if (!(is.getItem() instanceof BlockItem))
            return new BlockKey(Blocks.AIR);

//        if (matchStackWithBlock(is, Blocks.PISTON.defaultBlockState()) || matchStackWithBlock(is, Blocks.STICKY_PISTON.defaultBlockState()))

        return new BlockKey(Block.byItem(is.getItem()));
    }

}
