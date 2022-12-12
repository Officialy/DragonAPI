package reika.dragonapi.libraries;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import reika.dragonapi.instantiable.ItemMatch;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.item.ActivatedInventoryItem;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;

import static reika.dragonapi.DragonAPI.rand;

public class ReikaInventoryHelper {

    /**
     * Checks an itemstack array (eg an inventory) for an item of a specific id.
     * Returns true if found. Args: Item ID, Inventory
     */
    public static boolean checkForItem(Item id, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() == id) {
                    return true;
                } else if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItem(id, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean checkForItem(Block id, ItemStack[] inv) {
        return checkForItem(Item.BY_BLOCK.get(id), inv);
    }

    public static boolean checkForItem(Item id, Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack in = ii.getItem(i);
            if (in != null) {
                if (in.getItem() == id) {
                    return true;
                } else if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItem(id, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean checkForItem(Block id, Inventory ii) {
        return checkForItem(Item.BY_BLOCK.get(id), ii);
    }

    /**
     * Checks an itemstack array (eg an inventory) for a given itemstack.
     * Args: Check-for itemstack, Inventory, Match size T/F
     */
    public static boolean checkForItemStack(ItemStack is, ItemStack[] inv, boolean matchsize) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.tagMatches(is, in) && ReikaItemHelper.matchStacks(is, in))
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean checkForItemStack(KeyedItemStack is, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return true;
                }
                if (is.match(in))
                    return true;
            }
        }
        return false;
    }

    public static boolean checkForItemStack(ItemMatch is, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return true;
                }
                if (is.match(in))
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks an itemstack array (eg an inventory) for a given itemstack.
     * Args: Check-for itemstack, Inventory, Match size T/F
     */
    public static boolean checkForItemStack(ItemStack is, Inventory inv, boolean matchsize) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.tagMatches(is, in) && ReikaItemHelper.matchStacks(is, in))
                        return true;
                }
            }
        }
        return false;
    }
    /**
     * Checks an itemstack array (eg an inventory) for a given itemstack.
     * Args: Check-for itemstack, Inventory, Match size T/F
     */
    public static boolean checkForItemStack(ItemStack is, Container inv, boolean matchsize) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.tagMatches(is, in) && ReikaItemHelper.matchStacks(is, in))
                        return true;
                }
            }
        }
        return false;
    }
    /**
     * Converts a crafting inventory to a standard ItemStack array. Args: CraftingContainer
     */
    public static ItemStack[] convertCraftToItemStacks(CraftingContainer ic) {
        ItemStack[] slots = new ItemStack[ic.getContainerSize()];
        for (int i = 0; i < slots.length; i++)
            slots[i] = ic.getItem(i);
        return slots;
    }

    /**
     * Converts a crafting inventory to a standard ItemStack array. Args: CraftingContainer
     */
    public static ArrayList<ItemStack> convertCraftToItemList(CraftingContainer ic) {
        ArrayList<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < ic.getContainerSize(); i++) {
            ItemStack is = ic.getItem(i);
            if (is != null)
                slots.add(is);
        }
        return slots;
    }

    /**
     * Counts the number of empty slots in an inventory. Args: Inventory
     */
    public static int countEmptySlots(ItemStack[] inv) {
        int num = 0;
        for (int i = 0; i < inv.length; i++)
            if (inv[i] == null)
                num++;
        return num;
    }

    /**
     * Returns true if the inventory is full. Args: Inventory
     */
    public static boolean isInventoryFull(ItemStack[] inv) {
        if (countEmptySlots(inv) > 0)
            return false;
        for (int i = 0; i < inv.length; i++) {
            if (inv[i].getMaxStackSize() > inv[i].getCount())
                return false;
        }
        return true;
    }

    public static boolean canAcceptMoreOf(Item item, int amt, Inventory inv) {
        return canAcceptMoreOf(new ItemStack(item, amt), inv);
    }
    public static boolean hasNEmptyStacks(Container ii, int n) {
        int e = 0;
        for (int i = 0; i < ii.getContainerSize(); i++) {
            if (ii.getItem(i) == null)
                e++;
        }
        return e == n;
    }

    public static boolean hasNEmptyStacks(ItemStack[] inv, int n) {
        int e = 0;
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] == null)
                e++;
        }
        return e == n;
    }

    /**
     * Returns true if the inventory has space for more of a specific Items. Args: ItemStack, inventory
     */
    public static boolean canAcceptMoreOf(ItemStack is, Inventory inv) {
		/*
		if (countEmptySlots(inv) > 0)
			return true;
		if (locateInInventory(id, meta, inv) == -1)
			return false;
		int num = 0;
		int maxnum = new ItemStack(id, 1, meta).getMaxStackSize()*countNumStacks(id, meta, inv);
		for (int i = 0; i < inv.length; i++) {
			if (inv[i].getItem() == id && inv[i].getItemDamage() == meta)
				num += inv[i].getCount();
		}
		return (num < maxnum);*/
        int space = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.canPlaceItem(i, is)) {
                ItemStack in = inv.getItem(i);
                if (in == null)
                    return true;
                else {
                    if (ReikaItemHelper.matchStacks(in, is) && ItemStack.tagMatches(is, in)) {
                        int max = Math.min(in.getMaxStackSize(), inv.getMaxStackSize());
                        space += max - in.getCount();
                    }
                }
            }
        }
        return space >= is.getCount();
    }

    /**
     * Returns the number of unique itemstacks in the inventory after sorting and cleaning. Args: Inventory
     */
    public static int getTotalUniqueStacks(ItemStack[] inv) {
        ItemStack[] cp = new ItemStack[inv.length];
        for (int i = 0; i < cp.length; i++)
            cp[i] = inv[i];
        return 0;
    }

    public static boolean addToIInv(Block b, Container ii) {
        return addToIInv(new ItemStack(b), ii);
    }

    public static boolean addToIInv(Item is, Container ii) {
        return addToIInv(new ItemStack(is), ii);
    }

    public static boolean addToIInv(ItemStack is, Container ii) {
        return addToIInv(is, ii, false);
    }

    public static boolean addToIInv(ItemStack is, Container ii, boolean overrideValid) {
        return addToIInv(is, ii, overrideValid, 0, ii.getContainerSize());
    }

    public static boolean addToIInv(ItemStack is, Container ii, int first, int last) {
        return addToIInv(is, ii, false, first, last);
    }

    /**
     * Returns true if succeeded; adds if you can fit the whole stack
     */
    public static boolean addToIInv(ItemStack is, Container ii, boolean overrideValid, int firstSlot, int maxSlot) {
//        if (InterfaceCache.DSU.instanceOf(ii))
//            return addToDSU((IDeepStorageUnit)ii, is, false);
        is = is.copy();
        if (!hasSpaceFor(is, ii, overrideValid, firstSlot, maxSlot)) {
            return false;
        }
        int max = Math.min(ii.getContainerSize(), is.getCount());
        for (int i = firstSlot; i < maxSlot; i++) {
            if (overrideValid || ii.canPlaceItem(i, is)) {
                if (!(is.getItem() instanceof ArmorItem)) {
                    if (i >= (ii).getContainerSize())
                        continue;
                }
                ItemStack in = ii.getItem(i);
                if (in == null) {
                    int added = Math.min(is.getCount(), max);
                    int currentCount = is.getCount();
                    is.setCount(currentCount -= added);
                    ii.setItem(i, ReikaItemHelper.getSizedItemStack(is, added));
                    return true;
                } else {
                    if (ReikaItemHelper.areStacksCombinable(is, in, max)) {
                        int space = max - in.getCount();
                        int added = Math.min(is.getCount(), space);
                        int currentCount = is.getCount();
                        is.setCount(currentCount -= added);
                        int iicount = ii.getItem(i).getCount();
                        ii.getItem(i).setCount(iicount += added);
                        if (is.getCount() <= 0)
                            return true;
                    }
                }
            }
        }
        return is.getCount() == 0;
    }

    private static int addToInventoryWithLeftover(Item id, int size, ItemStack[] inventory) {
        int slot = locateInInventory(id, inventory);
        int empty = findEmptySlot(inventory);
        if (slot == -1) {
            if (empty == -1)
                return size;
            inventory[empty] = new ItemStack(id, size);
            return 0;
        }
        int space = inventory[slot].getMaxStackSize() - inventory[slot].getCount();
        if (space >= size) {
            int count = inventory[slot].getCount();
            inventory[slot].setCount(count += size);
            return 0;
        }
        int count = inventory[slot].getCount();
        inventory[slot].setCount(count += space);
        size -= space;
        return size;
    }

    /**
     * Adds as much of the specified item stack as it can and
     * returns the number of "leftover" items that did not fit.
     * Args: Itemstack, inventory
     */
    public static int addToInventoryWithLeftover(ItemStack stack, ItemStack[] inventory) {
        int leftover = addToInventoryWithLeftover(stack.getItem(), stack.getCount(), inventory);
        return leftover;
    }

    public static int addToInventoryWithLeftover(ItemStack stack, Inventory inventory, boolean simulate) {
        int left = stack.getCount();
        int max = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack in = inventory.getItem(i);
            if (in == null) {
                int add = Math.min(max, left);
                if (!simulate)
                    inventory.setItem(i, ReikaItemHelper.getSizedItemStack(stack, add));
                left -= add;
                if (left <= 0)
                    return 0;
            } else {
                if (ReikaItemHelper.matchStacks(stack, in) && ItemStack.tagMatches(stack, in)) {
                    int space = max - in.getCount();
                    int add = Math.min(space, stack.getCount());
                    if (add > 0) {
                        int count = in.getCount();
                        if (!simulate)
                            in.setCount(count += add);
                        left -= add;
                        if (left <= 0)
                            return 0;
                    }
                }
            }
        }
        return left;
    }

    /**
     * Returns the location (array index) of an itemstack in the specified inventory.
     * Returns -1 if not present. Args: Itemstack to check, Inventory, Match size T/F
     */
    public static int locateInInventory(ItemStack is, ItemStack[] inv, boolean matchsize) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return i;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in)) {
                        return i;
                    }
                } else {
                    if (ItemStack.tagMatches(is, in) && ReikaItemHelper.matchStacks(is, in)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static int locateInInventory(KeyedItemStack is, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return i;
                }
                if (is.match(in)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int locateInInventory(ItemMatch is, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in)))
                        return i;
                }
                if (is.match(in)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the location (array index) of an itemstack in the specified inventory.
     * Returns -1 if not present. Args: Itemstack to check, Inventory, Match size T/F
     */
    public static int locateInInventory(ItemStack is, IItemHandler inv, boolean matchsize) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack in = inv.getStackInSlot(i);
            if (in != null) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return i;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in)) {
                        return i;
                    }
                } else {
                    if (ItemStack.tagMatches(is, in) && ReikaItemHelper.matchStacks(is, in)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns the location (array index) of an item in the specified inventory.
     * Returns -1 if not present. Args: Item ID, Inventory
     */
    public static int locateInInventory(Item id, ItemStack[] inv) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (in.getItem() == id) {
                    return i;
                } else if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (locateInInventory(id, ((ActivatedInventoryItem) in.getItem()).getInventory(in)) >= 0)
                        return i;
                }
            }
        }
        return -1;
    }

    /**
     * Fill-in so one does not need to constantly rewrite the Inventory method
     */
    public static ItemStack getItemOnClosing(Inventory ii, int slot) {
        if (ii.getItem(slot) != null) {
            ItemStack itemstack = ii.getItem(slot);
            ii.setItem(slot, null);
            return itemstack;
        } else
            return null;
    }

    /**
     * Returns the location of an empty slot in an inventory. Returns -1 if none.
     * Args: Inventory
     */
    public static int findEmptySlot(ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null)
                return i;
            if (inventory[i].getCount() <= 0) {
                inventory[i] = null;
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the location of an empty slot in an inventory. Returns -1 if none.
     * Args: Inventory
     */
    public static int findEmptySlot(Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack is = ii.getItem(i);
            if (is == null)
                return i;
            if (is.getCount() <= 0) {
                is = null;
                return i;
            }
        }
        return -1;
    }

    public static ArrayList<Integer> findEmptySlots(ItemStack[] inventory) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null)
                li.add(i);
            if (inventory[i].getCount() <= 0) {
                inventory[i] = null;
                li.add(i);
            }
        }
        return li;
    }

    public static ArrayList<Integer> findEmptySlots(Inventory inventory) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack is = inventory.getItem(i);
            if (is == null)
                li.add(i);
            if (is.getCount() <= 0) {
                inventory.setItem(i, null);
                li.add(i);
            }
        }
        return li;
    }

    /**
     * Returns true if the inventory is empty. Args: Inventory
     */
    public static boolean isEmpty(ItemStack[] inventory) {
        return isEmpty(inventory, 0, inventory.length);
    }

    /**
     * Returns true if the inventory is empty in the slot range specified. Args: Inventory, int min, int max
     */
    public static boolean isEmpty(ItemStack[] inventory, int min, int max) {
        for (int i = min; i < max; i++) {
            if (inventory[i] != null)
                return false;
        }
        return true;
    }

    /**
     * Returns the number of items to add to a stack when adding a random number
     * between min and max, but taking care to stay within getCount() limits.
     * Args: Intial slot contents, min items, max items
     */
    public static int addUpToStack(ItemStack init, int min, int max) {
        int num = min + rand.nextInt(max - min + 1);
        if (init == null)
            return (num);
        while (num + init.getCount() > init.getMaxStackSize())
            num--;
        return num;
    }

    /**
     * Intelligently decrements a stack in an inventory, setting it to null if necessary.
     * Also performs sanity checks. Args: Inventory, Slot
     */
    public static void decrStack(int slot, ItemStack[] inv) {
        decrStack(slot, inv, 1);
    }

    public static void decrStack(int slot, ItemStack[] inv, int amount) {
        if (slot >= inv.length) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is larger than the inventory.");
            return;
        }
        if (slot < 0) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is < 0.");
            return;
        }
        ItemStack in = inv[slot];
        if (in == null) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is empty.");
            return;
        }
        int count = in.getCount();
        if (in.getCount() > amount)
            in.setCount(count -= amount);
        else
            inv[slot] = null;
    }

    /**
     * Intelligently decrements a stack in an inventory, setting it to null if necessary.
     * Also performs sanity checks. Args: Inventory, Slot, Amount
     */
    public static void decrStack(int slot, IItemHandler inv, int amount) {
        if (slot >= inv.getSlots()) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is larger than the inventory.");
            return;
        }
        if (slot < 0) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is < 0.");
            return;
        }
        ItemStack in = inv.getStackInSlot(slot);
        if (in == null) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is empty.");
            return;
        }
        //ReikaJavaLibrary.pConsole("pre: "+inv.getItem(slot)+" w "+amount);
        int count = in.getCount();
        if (in.getCount() > amount)
            in.setCount(count -= amount);
        else
            inv.insertItem(slot, null, false);
        //ReikaJavaLibrary.pConsole("post: "+inv.getItem(slot)+" w "+amount);

    }

    /**
     * Fill-in so one does not need to constantly rewrite the Inventory method
     */
    public static ItemStack decrStackSize(Container ii, int slot, int decr) {
        if (ii.getItem(slot) != null) {
            if (ii.getItem(slot).getCount() <= decr) {
                ItemStack itemstack = ii.getItem(slot);
                ii.setItem(slot, null);
                return itemstack;
            }
            ItemStack itemstack1 = ii.getItem(slot).split(decr);
            if (ii.getItem(slot).getCount() == 0)
                ii.setItem(slot, null);
            return itemstack1;
        } else
            return null;
    }

    public static int locateIDInInventory(Item id, Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            if (ii.getItem(i) != null) {
                if (ii.getItem(i).getItem() == id)
                    return i;
            }
        }
        return -1;
    }

    public static boolean hasSpaceFor(ItemStack is, Container ii, boolean overrideValid) {
        return hasSpaceFor(is, ii, overrideValid, 0, ii.getContainerSize());
    }

    public static boolean hasSpaceFor(ItemStack is, Container ii, boolean overrideValid, int firstSlot, int maxSlot) {
        int size = is.getCount();
        int max = Math.min(ii.getMaxStackSize(), is.getMaxStackSize());
        for (int i = firstSlot; i < maxSlot && size > 0; i++) {
            if (overrideValid || ii.canPlaceItem(i, is)) {
                ItemStack in = ii.getItem(i);
                if (in == null) {
                    size -= max;
                } else {
                    if (ReikaItemHelper.matchStacks(is, in) && ItemStack.tagMatches(is, in)) {
                        int space = max - in.getCount();
                        size -= space;
                    }
                }
            }
        }
        return size <= 0;
    }


    public static boolean addOrSetStack(ItemStack is, Inventory inv, int slot) {
        if (is == null)
            return false;
        ItemStack at = inv.getItem(slot);
        if (at == null) {
            inv.setItem(slot, is.copy());
            return true;
        }
        int max = at.getMaxStackSize();
        if (!(ReikaItemHelper.matchStacks(is, at) && ItemStack.tagMatches(is, at)) || at.getCount() + is.getCount() > max)
            return false;
        int count = at.getCount();
        at.setCount(count += is.getCount());
        return true;
    }

    public static boolean addOrSetStack(ItemStack is, ItemStack[] inv, int slot) {
        if (is == null)
            return false;
        if (inv[slot] == null) {
            inv[slot] = is.copy();
            return true;
        }
        int max = inv[slot].getMaxStackSize();
        if (!(ReikaItemHelper.matchStacks(is, inv[slot]) && ItemStack.tagMatches(is, inv[slot])) || inv[slot].getCount() + is.getCount() > max)
            return false;

        int count = inv[slot].getCount();
        inv[slot].setCount(count += is.getCount());
        return true;
    }

    /**
     * Adds a certain amount of a specified ID and metadata to an inventory slot, creating the itemstack if necessary.
     * Returns true if the whole stack fit and was added. Args: ID, number, metadata (-1 for any), inventory, slot
     */
    public static boolean addOrSetStack(Item id, int size, ItemStack[] inv, int slot) {
        return addOrSetStack(new ItemStack(id, size), inv, slot);
    }

    public static ArrayList<ItemStack> getWholeInventory(Container ii) {
        ArrayList<ItemStack> li = new ArrayList<>();
        for (int i = 0; i < ii.getContainerSize(); i++) {
            li.add(ii.getItem(i));
        }
        return li;
    }

    public static void clearInventory(Container ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ii.setItem(i, null);
        }
    }

    /**
     * Add multiple items to an inventory. Args: Inventory, Items. Returns the ones that could not be added.
     */
    public static List<ItemStack> addMultipleItems(Container ii, List<ItemStack> items) {
        List<ItemStack> extra = new ArrayList<ItemStack>();
        for (int i = 0; i < items.size(); i++) {
            if (!addToIInv(items.get(i), ii))
                extra.add(items.get(i));
        }
        return extra;
    }

    /**
     * Gets the first block in an inventory, optionally consuming one. Args: Inventory, Decr yes/no
     */
    public static ItemStack getNextBlockInInventory(ItemStack[] inv, boolean decr) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack is = inv[i];
            if (is != null) {
                Item item = is.getItem();
                if (item instanceof BlockItem) {
                    if (decr)
                        decrStack(i, inv);
                    return inv[i];
                }
            }
        }
        return null;
    }

    /**
     * Returns whether an inventory is full. Args: Inventory
     */
    public static boolean isFull(Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack is = ii.getItem(i);
            if (is == null)
                return false;
            int max = Math.min(is.getMaxStackSize(), ii.getMaxStackSize());
            if (is.getCount() < max)
                return false;
        }
        return true;
    }

    /**
     * Returns whether an Container is full. Args: Container
     */
    public static boolean isFull(Container ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack is = ii.getItem(i);
            if (is == null)
                return false;
            int max = Math.min(is.getMaxStackSize(), ii.getMaxStackSize());
            if (is.getCount() < max)
                return false;
        }
        return true;
    }

    /**
     * Spills the entire inventory of an ItemStack[] at the specified coordinates with a 1-block spread.
     * Args: Level, x, y, z, inventory
     */
    public static void spillAndEmptyInventory(Level world, int x, int y, int z, ItemStack[] inventory) {
        ItemEntity ei;
        ItemStack is;
        for (int i = 0; i < inventory.length; i++) {
            is = inventory[i];
            inventory[i] = null;
            if (is != null && !world.isClientSide()) {
                ei = new ItemEntity(world, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), is);
                ReikaEntityHelper.addRandomDirVelocity(ei, 0.2);
                world.addFreshEntity(ei);
            }
        }
    }

    /**
     * Spills the entire inventory of an Inventory at the specified coordinates with a 1-block spread.
     * Args: Level, x, y, z, Inventory
     */
    public static void spillAndEmptyInventory(Level world, int x, int y, int z, IItemHandler ii) {
        int size = ii.getSlots();
        for (int i = 0; i < size; i++) {
            ItemStack s = ii.getStackInSlot(i);
            if (s != null) {
                ii.insertItem(i, null, false);
                ItemEntity ei = new ItemEntity(world, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), s);
                ReikaEntityHelper.addRandomDirVelocity(ei, 0.2);
                ei.setPickUpDelay(10);
                if (!world.isClientSide())
                    world.addFreshEntity(ei);
            }
        }
    }

    public static int getFirstNonEmptySlot(IItemHandler ii) {
        for (int i = 0; i < ii.getSlots(); i++) {
            if (ii.getStackInSlot(i) != null)
                return i;
        }
        return -1;
    }

    /**
     * Adds an ItemStack to an inventory and returns how many items were successfully added.
     */
    public static int addStackAndReturnCount(ItemStack stack, IItemHandler ii) {
        return addStackAndReturnCount(stack, ii, 0, ii.getSlots() - 1);
    }

    public static int addStackAndReturnCount(ItemStack stack, IItemHandler ii, int slotMin, int slotMax) {
        int slots = ii.getSlots(); //todo ii instanceof IItemHandler ? ((IItemHandler)ii).getAccessibleSlotsFromSide(side.ordinal()) : ReikaArrayHelper.getLinearArray(slotMin, slotMax);
        int transferred = 0;
        for (int idx = 0; idx < slots && stack.getCount() > 0; idx++) {
            ItemStack is = ii.getStackInSlot(slots);
            if (is == null) {
                ii.insertItem(slots, stack.copy(), false);
                transferred += stack.getCount();
                stack.setCount(0);
            } else {
                if (ReikaItemHelper.areStacksCombinable(stack, is, ii.getSlots())) {
                    int max = Math.min(stack.getMaxStackSize(), ii.getSlots());
                    int space = max - is.getCount();
                    if (space > 0) {
                        int added = Math.min(space, stack.getCount());
                        transferred += added;
                        is.setCount(is.getCount() + added);
                        stack.setCount(stack.getCount() - added);
                    }
                }
            }
        }
        return transferred;
    }

    public static ArrayList<ItemStack> getAllTransferrables(IItemHandler source) {
        ArrayList<ItemStack> li = new ArrayList<>();
        if (source instanceof IItemHandler) {
            IItemHandler ii = source;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack is = ii.getStackInSlot(slot);
                if (is != null) {
                    if (ii.isItemValid(slot, is)) {
                        li.add(is);
                    }
                }
            }
        } else if (source instanceof IItemHandler) {
            IItemHandler ii = source;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack is = ii.getStackInSlot(slot);
                if (is != null) {
                    li.add(is);
                }
            }
        }
        return li;
    }

    public static HashMap<Integer, ItemStack> getLocatedTransferrables(IItemHandler source) {
        HashMap<Integer, ItemStack> li = new HashMap<>();
        if (source instanceof IItemHandler) {
            IItemHandler ii = source;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack is = ii.getStackInSlot(slot);
                if (is != null) {
                    if (ii.isItemValid(slot, is)) {
                        li.put(slot, is);
                    }
                }
            }
        } else if (source instanceof IItemHandler) {
            IItemHandler ii = source;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack is = ii.getStackInSlot(slot);
                if (is != null) {
                    li.put(slot, is);
                }
            }
        }
        return li;
    }

    public static ArrayList<Integer> getSlotsWithItemStack(ItemStack is, Inventory ii, boolean matchSize) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack in = ii.getItem(i);
            if (ReikaItemHelper.matchStacks(is, in)) {
                if (!matchSize || in.getCount() == is.getCount()) {
                    li.add(i);
                }
            }
        }
        return li;
    }

    public static ArrayList<Integer> getSlotsWithItemStack(ItemStack is, ItemStack[] inv, boolean matchSize) {
        ArrayList<Integer> li = new ArrayList<>();
        for (int i = 0; i < inv.length; i++) {
            ItemStack in = inv[i];
            if (ReikaItemHelper.matchStacks(is, in)) {
                if (!matchSize || in.getCount() == is.getCount()) {
                    li.add(i);
                }
            }
        }
        return li;
    }

    public static HashSet<Integer> getSlotsBetweenWithItemStack(ItemStack is, Inventory ii, int min, int max, boolean matchSize) {
        HashSet<Integer> li = new HashSet<>();
        for (int i = min; i <= max; i++) {
            ItemStack in = ii.getItem(i);
            if (ReikaItemHelper.matchStacks(is, in)) {
                if (!matchSize || in.getCount() == is.getCount()) {
                    li.add(i);
                }
            }
        }
        return li;
    }

    public static boolean inventoryContains(ItemHashMap<Integer> map, Inventory ii) {
        ItemHashMap<Integer> inv = ItemHashMap.getFromInventory(ii);
        for (ItemStack is : map.keySet()) {
            int need = map.get(is);
            Integer has = inv.get(is);
            if (has == null || need > has.intValue())
                return false;
        }
        return true;
    }

    public static void removeFromInventory(ItemHashMap<Integer> map, IItemHandler ii) {
        for (ItemStack is : map.keySet()) {
            int need = map.get(is);
            int loc = locateInInventory(is, ii, false);
            while (loc >= 0 && need > 0) {
                ItemStack in = ii.getStackInSlot(loc);
                int max = Math.min(need, in.getCount());
                decrStack(loc, ii, max);
                need -= max;
                loc = locateInInventory(is, ii, false);
            }
        }
    }

//    public static void generateMultipliedLoot(int bonus, Random r, String s, Inventory te) {
//        for (int n = 0; n < bonus; n++) {
//            TemporaryInventory ii = new TemporaryInventory(te.getContainerSize());
//            WeightedRandomChestContent[] loot = ChestGenHooks.getItems(s, r);
//            WeightedRandomChestContent.generateChestContents(r, loot, ii, ChestGenHooks.getCount(s, r));
//            for (int i = 0; i < ii.getContainerSize(); i++) {
//                ItemStack in = ii.getItem(i);
//                if (in != null) {
//                    int tg = r.nextInt(ii.getContainerSize());
//                    int tries = 0;
//                    while (te.getItem(tg) != null && tries < 10) {
//                        tg = r.nextInt(ii.getContainerSize());
//                        tries++;
//                    }
//                    if (te.getItem(tg) == null) {
//                        te.setItem(tg, in);
//                    }
//                }
//            }
//        }
//    }

    public static void addItems(Container ii, ArrayList<ItemStack> li) {
        for (ItemStack is : li) {
            addToIInv(is, ii);
        }
    }

    /**
     * Returns the number successfully removed.
     */
    public static int drawFromInventory(ItemStack is, int max, Inventory inventory) {
        int amt = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack in = inventory.getItem(i);
            if (ReikaItemHelper.matchStacks(is, in)) {
                int rem = Math.min(max, in.getCount());
                if (rem > 0) {
                    if (in.getCount() > rem) {
                        int count = in.getCount();
                        in.setCount(count -= rem);
                    } else {
                        inventory.setItem(i, null);
                    }
                    max -= rem;
                    amt += rem;
                    if (max <= 0)
                        break;
                }
            }
        }
        return amt;
    }

    /**
     * Returns whether an inventory is empty. Args: Inventory
     */
    public static boolean isEmpty(Inventory ii) {
        return isEmptyFrom(ii, 0, ii.getContainerSize() - 1);
    }

    public static boolean isEmptyFrom(Container ii, int from, int to) {
        for (int i = from; i <= to; i++) {
            ItemStack is = ii.getItem(i);
            if (is != null)
                return false;
        }
        return true;
    }

    public static ItemStack getSmallestStack(ItemStack[] inv) {
        return getSmallestStack(inv, 0, inv.length - 1);
    }

    public static ItemStack getSmallestStack(ItemStack[] inv, int min, int max) {
        ItemStack smallest = null;
        for (int i = min; i <= max; i++) {
            ItemStack in = inv[i];
            if (in != null) {
                if (smallest == null || smallest.getCount() < in.getCount())
                    smallest = in;
            }
        }
        return smallest;
    }

    public static void sortInventory(Container te, Comparator<ItemStack> c) {
        ArrayList<ItemStack> li = getWholeInventory(te);
        Collections.sort(li, c);
        clearInventory(te);
        for (int i = 0; i < li.size(); i++) {
            te.setItem(i, li.get(i));
        }
    }
}
