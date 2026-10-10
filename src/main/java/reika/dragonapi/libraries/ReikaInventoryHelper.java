package reika.dragonapi.libraries;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.instantiable.storage.ManagedItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import reika.dragonapi.instantiable.ItemMatch;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.item.ActivatedInventoryItem;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;

import static reika.dragonapi.DragonAPI.rand;

public class ReikaInventoryHelper {

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    private static int handlerLimit(ManagedItemHandler handler, int slot, ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), handler.getSlotLimit(slot, stack));
    }

    private static void spill(Level world, int x, int y, int z, ItemStack stack, boolean delayPickup) {
        ItemEntity entity = new ItemEntity(world, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), stack);
        ReikaEntityHelper.addRandomDirVelocity(entity, 0.2);
        if (delayPickup) entity.setPickUpDelay(10);
        world.addFreshEntity(entity);
    }

    /** 26.3 armor is defined by its Equippable component. */
    private static boolean isArmorItem(ItemStack is) {
        Equippable eq = is.get(DataComponents.EQUIPPABLE);
        return eq != null && eq.slot().isArmor();
    }

    /**
     * Checks an itemstack array (eg an inventory) for an item of a specific id.
     * Returns true if found. Args: Item ID, Inventory
     */
    public static boolean checkForItem(Item id, ItemStack[] inv) {
        for (ItemStack in : inv) {
            if (!empty(in)) {
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
            if (!empty(in)) {
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
        if (empty(is)) return false;
        for (ItemStack in : inv) {
            if (!empty(in)) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.isSameItemSameComponents(is, in) && ReikaItemHelper.matchStacks(is, in))
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean checkForItemStack(KeyedItemStack is, ItemStack[] inv) {
        for (ItemStack in : inv) {
            if (!empty(in)) {
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
        for (ItemStack in : inv) {
            if (!empty(in)) {
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
        if (empty(is)) return false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (!empty(in)) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.isSameItemSameComponents(is, in) && ReikaItemHelper.matchStacks(is, in))
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
        if (empty(is)) return false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (!empty(in)) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return true;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in))
                        return true;
                } else {
                    if (ItemStack.isSameItemSameComponents(is, in) && ReikaItemHelper.matchStacks(is, in))
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
            if (!empty(is))
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
            if (empty(inv[i]))
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

    public static boolean hasNEmptyStacks(ManagedItemHandler ii, int n) {
        int e = 0;
        for (int i = 0; i < ii.getSlots(); i++) {
            if (ii.getStackInSlot(i).isEmpty())
                e++;
        }
        return e == n;
    }

    public static boolean hasNEmptyStacks(ItemStack[] inv, int n) {
        int e = 0;
        for (ItemStack itemStack : inv) {
            if (empty(itemStack))
                e++;
        }
        return e == n;
    }

    /**
     * Returns true if the inventory has space for more of a specific Items. Args: ItemStack, inventory
     */
    public static boolean canAcceptMoreOf(ItemStack is, Inventory inv) {
        return hasSpaceFor(is, inv, false, 0, inv.getContainerSize());
    }

    /**
     * Returns the number of unique itemstacks in the inventory after sorting and cleaning. Args: Inventory
     */
    public static int getTotalUniqueStacks(ItemStack[] inv) {
        List<ItemStack> unique = new ArrayList<>();
        for (ItemStack stack : inv) {
            if (!empty(stack) && unique.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack)))
                unique.add(stack);
        }
        return unique.size();
    }

    public static boolean addToIInv(Block b, ManagedItemHandler ii) {
        return addToIInv(new ItemStack(b), ii);
    }

    public static boolean addToIInv(Item is, ManagedItemHandler ii) {
        return addToIInv(new ItemStack(is), ii);
    }
    public static boolean addToIInv(Item is, Container ii) {
        return addToIInv(new ItemStack(is), ii);
    }
    public static boolean addToIInv(ItemStack is, ManagedItemHandler ii) {
        return addToIInv(is, ii, false);
    }

    public static boolean addToIInv(ItemStack is, ManagedItemHandler ii, boolean overrideValid) {
        return addToIInv(is, ii, overrideValid, 0, ii.getSlots());
    }

    public static boolean addToIInv(ItemStack is, ManagedItemHandler ii, int first, int last) {
        return addToIInv(is, ii, false, first, last);
    }

    /**
     * Returns true if succeeded; adds if you can fit the whole stack
     */
    public static boolean addToIInv(ItemStack is, ManagedItemHandler ii, boolean overrideValid, int firstSlot, int maxSlot) {
        if (empty(is)) return true;
        int first = Math.max(0, firstSlot);
        int end = Math.min(maxSlot, ii.getSlots());
        if (!overrideValid) {
            // One root transaction makes a failed full-fit insertion leave every slot unchanged.
            ItemResource resource = ItemResource.of(is);
            int left = is.getCount();
            try (Transaction tx = Transaction.openRoot()) {
                for (int slot = first; slot < end && left > 0; slot++)
                    left -= ii.insert(slot, resource, left, tx);
                if (left > 0) return false;
                tx.commit();
                return true;
            }
        }
        if (!hasSpaceFor(is, ii, true, first, end)) return false;
        ItemStack remaining = is.copy();
        for (int slot = first; slot < end && !remaining.isEmpty(); slot++) {
            ItemStack present = ii.getStackInSlot(slot);
            int limit = handlerLimit(ii, slot, remaining);
            if (present.isEmpty() || ItemStack.isSameItemSameComponents(present, remaining)) {
                int added = Math.min(remaining.getCount(), Math.max(0, limit - present.getCount()));
                if (added > 0) {
                    ii.setStackInSlot(slot, remaining.copyWithCount(present.getCount() + added));
                    remaining.shrink(added);
                }
            }
        }
        return remaining.isEmpty();
    }


    public static boolean addToIInv(Block b, Container ii) {
        return addToIInv(new ItemStack(b), ii);
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
    public static boolean addToIInv(ItemStack stack, Container inventory, boolean overrideValid, int firstSlot, int maxSlot) {
        if (empty(stack)) return true;
        ItemStack remaining = stack.copy();
        int end = Math.min(maxSlot, inventory.getContainerSize());
        if (!hasSpaceFor(remaining, inventory, overrideValid, firstSlot, end))
            return false;
        boolean changed = false;
        for (int slot = Math.max(0, firstSlot); slot < end && !remaining.isEmpty(); slot++) {
            if (!overrideValid && !inventory.canPlaceItem(slot, remaining))
                continue;
            ItemStack present = inventory.getItem(slot);
            int slotLimit = Math.min(inventory.getMaxStackSize(), remaining.getMaxStackSize());
            if (empty(present)) {
                int inserted = Math.min(remaining.getCount(), slotLimit);
                inventory.setItem(slot, remaining.copyWithCount(inserted));
                remaining.shrink(inserted);
                changed = true;
            }
            else if (ItemStack.isSameItemSameComponents(remaining, present)) {
                int inserted = Math.min(remaining.getCount(), Math.max(0, slotLimit - present.getCount()));
                if (inserted > 0) {
                    inventory.setItem(slot, remaining.copyWithCount(present.getCount() + inserted));
                    remaining.shrink(inserted);
                    changed = true;
                }
            }
        }
        if (changed)
            inventory.setChanged();
        return remaining.isEmpty();
    }

    private static int addToInventoryWithLeftover(Item id, int size, ItemStack[] inventory) {
        return size <= 0 ? 0 : addToInventoryWithLeftover(new ItemStack(id, size), inventory);
    }

    public static int addToInventoryWithLeftover(Item id, int size, ManagedItemHandler inventory) {
        return size <= 0 ? 0 : addToInventoryWithLeftover(new ItemStack(id, size), inventory);
    }

    /**
     * Adds as much of the specified item stack as it can and
     * returns the number of "leftover" items that did not fit.
     * Args: Itemstack, inventory
     */
    public static int addToInventoryWithLeftover(ItemStack stack, ItemStack[] inventory) {
        if (empty(stack)) return 0;
        int left = stack.getCount();
        for (int slot = 0; slot < inventory.length && left > 0; slot++) {
            ItemStack present = inventory[slot];
            if (!empty(present) && !ItemStack.isSameItemSameComponents(stack, present)) continue;
            int count = empty(present) ? 0 : present.getCount();
            int added = Math.min(left, Math.max(0, stack.getMaxStackSize() - count));
            if (added > 0) {
                inventory[slot] = stack.copyWithCount(count + added);
                left -= added;
            }
        }
        return left;
    }

    /**
     * Adds as much of the specified item stack as it can and
     * returns the number of "leftover" items that did not fit.
     * Args: Itemstack, ManagedItemHandler
     */
    public static int addToInventoryWithLeftover(ItemStack stack, ManagedItemHandler inventory) {
        if (empty(stack)) return 0;
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getSlots() && !remaining.isEmpty(); slot++)
            remaining = inventory.insertItem(slot, remaining, false);
        return remaining.getCount();
    }

    public static int addToInventoryWithLeftover(ItemStack stack, Inventory inventory, boolean simulate) {
        if (empty(stack)) return 0;
        int left = stack.getCount();
        int limit = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            if (!inventory.canPlaceItem(slot, stack)) continue;
            ItemStack present = inventory.getItem(slot);
            if (!empty(present) && !ItemStack.isSameItemSameComponents(stack, present)) continue;
            int count = empty(present) ? 0 : present.getCount();
            int added = Math.min(left, Math.max(0, limit - count));
            if (added > 0) {
                if (!simulate) inventory.setItem(slot, stack.copyWithCount(count + added));
                left -= added;
            }
        }
        if (!simulate && left < stack.getCount()) inventory.setChanged();
        return left;
    }

    /**
     * Returns the location (array index) of an itemstack in the specified inventory.
     * Returns -1 if not present. Args: Itemstack to check, Inventory, Match size T/F
     */
    public static int locateInInventory(ItemStack is, ManagedItemHandler inv, boolean matchsize) {
        if (empty(is)) return -1;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack in = inv.getStackInSlot(i);
            if (!empty(in)) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return i;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in)) {
                        return i;
                    }
                } else {
                    if (ItemStack.isSameItemSameComponents(is, in) && ReikaItemHelper.matchStacks(is, in)) {
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
            if (!empty(in)) {
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
            if (!empty(in)) {
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
    public static int locateInInventory(ItemStack is, Container inv, boolean matchsize) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (!empty(in)) {
                if (in.getItem() instanceof ActivatedInventoryItem) {
                    if (checkForItemStack(is, ((ActivatedInventoryItem) in.getItem()).getInventory(in), matchsize))
                        return i;
                }
                if (matchsize) {
                    if (ItemStack.matches(is, in)) {
                        return i;
                    }
                } else {
                    if (ItemStack.isSameItemSameComponents(is, in) && ReikaItemHelper.matchStacks(is, in)) {
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
            if (!empty(in)) {
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
     * Returns the location (array index) of an item in the specified inventory.
     * Returns -1 if not present. Args: Item ID, ManagedItemHandler
     */
    public static int locateInInventory(Item id, ManagedItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack in = inv.getStackInSlot(i);
            if (!empty(in)) {
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
        ItemStack stack = ii.getItem(slot);
        if (empty(stack)) return ItemStack.EMPTY;
        ii.setItem(slot, ItemStack.EMPTY);
        ii.setChanged();
        return stack;
    }

    /**
     * Returns the location of an empty slot in an inventory. Returns -1 if none.
     * Args: Inventory
     */
    public static int findEmptySlot(ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; i++)
            if (empty(inventory[i])) return i;
        return -1;
    }

    /**
     * Returns the location of an empty slot in an inventory. Returns -1 if none.
     * Args: ManagedItemHandler
     */
    public static int findEmptySlot(ManagedItemHandler inventory) {
        for (int i = 0; i < inventory.getSlots(); i++)
            if (empty(inventory.getStackInSlot(i))) return i;
        return -1;
    }
    /**
     * Returns the location of an empty slot in an inventory. Returns -1 if none.
     * Args: Inventory
     */
    public static int findEmptySlot(Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++)
            if (empty(ii.getItem(i))) return i;
        return -1;
    }

    public static ArrayList<Integer> findEmptySlots(ItemStack[] inventory) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.length; i++)
            if (empty(inventory[i])) slots.add(i);
        return slots;
    }

    public static ArrayList<Integer> findEmptySlots(Inventory inventory) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (empty(inventory.getItem(i))) slots.add(i);
        return slots;
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
            if (!empty(inventory[i]))
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
        int amount = min + rand.nextInt(max - min + 1);
        return empty(init) ? amount : Math.min(amount, Math.max(0, init.getMaxStackSize() - init.getCount()));
    }

    /**
     * Intelligently decrements a stack in an inventory, setting it to EMPTY if necessary.
     * Also performs sanity checks. Args: Inventory, Slot
     */
    public static void decrStack(int slot, ItemStack[] inv) {
        decrStack(slot, inv, 1);
    }

    public static void decrStack(int slot, ManagedItemHandler inv) {
        decrStack(slot, inv, 1);
    }

    public static void decrStack(int slot, ManagedItemHandler inv, int amount) {
        if (amount <= 0) return;
        if (slot >= inv.getSlots()) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is larger than the inventory.");
            return;
        }
        if (slot < 0) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is < 0.");
            return;
        }
        ItemStack in = inv.getStackInSlot(slot);
        if (empty(in)) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is empty.");
            return;
        }
        // 26.1 fix: {@code ManagedItemHandler#getStackInSlot} returns a NEW {@link ItemStack}
        // copy each call — mutating that copy (e.g. {@code in.setCount(...)}) has no effect on
        // the underlying storage. Previously this method called {@code in.setCount(count - amount)}
        // which silently did nothing, leaving engines with infinite fuel, grinders with infinite
        // input, and every other caller broken. Route through {@code extractItem} which goes
        // through the resource-handler transaction system and commits a real change.
        inv.extractItem(slot, amount, false);
    }

    public static void decrStack(int slot, ItemStack[] inv, int amount) {
        if (amount <= 0) return;
        if (slot >= inv.length) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is larger than the inventory.");
            return;
        }
        if (slot < 0) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is < 0.");
            return;
        }
        ItemStack in = inv[slot];
        if (empty(in)) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is empty.");
            return;
        }
        int count = in.getCount();
        if (in.getCount() > amount)
            in.setCount(count -= amount);
        else
            inv[slot] = ItemStack.EMPTY;
    }

    /**
     * Intelligently decrements a stack in an inventory, setting it to EMPTY if necessary.
     * Also performs sanity checks. Args: Slot, Container, Amount
     */
    public static void decrStack(int slot, Container inv, int amount) {
        if (amount <= 0) return;
        if (slot >= inv.getContainerSize()) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is larger than the inventory.");
            return;
        }
        if (slot < 0) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is < 0.");
            return;
        }
        ItemStack in = inv.getItem(slot);
        if (empty(in)) {
            ReikaChatHelper.write("Tried to access Slot " + slot + ", which is empty.");
            return;
        }
        //ReikaJavaLibrary.pConsole("pre: "+inv.getItem(slot)+" w "+amount);
        inv.setItem(slot, amount < in.getCount() ? in.copyWithCount(in.getCount() - amount) : ItemStack.EMPTY);
        inv.setChanged();

    }

    /**
     * Fill-in so one does not need to constantly rewrite the Inventory method
     */
    public static ItemStack decrStackSize(ManagedItemHandler ii, int slot, int decr) {
        return ii.extractItem(slot, decr, false);
    }

    public static int locateIDInInventory(Item id, Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            if (!ii.getItem(i).isEmpty()) {
                if (ii.getItem(i).getItem() == id)
                    return i;
            }
        }
        return -1;
    }

    public static boolean hasSpaceFor(ItemStack is, ManagedItemHandler ii, boolean overrideValid) {
        return hasSpaceFor(is, ii, overrideValid, 0, ii.getSlots());
    }

    public static boolean hasSpaceFor(ItemStack is, ManagedItemHandler ii, boolean overrideValid, int firstSlot, int maxSlot) {
        if (empty(is)) return true;
        if (!overrideValid) {
            ItemResource resource = ItemResource.of(is);
            int left = is.getCount();
            try (Transaction tx = Transaction.openRoot()) {
                for (int slot = Math.max(0, firstSlot); slot < Math.min(maxSlot, ii.getSlots()) && left > 0; slot++)
                    left -= ii.insert(slot, resource, left, tx);
                return left == 0;
            }
        }
        int left = is.getCount();
        for (int slot = Math.max(0, firstSlot); slot < Math.min(maxSlot, ii.getSlots()) && left > 0; slot++) {
            ItemStack present = ii.getStackInSlot(slot);
            if (present.isEmpty() || ItemStack.isSameItemSameComponents(is, present))
                left -= Math.max(0, handlerLimit(ii, slot, is) - present.getCount());
        }
        return left <= 0;
    }
    public static boolean hasSpaceFor(ItemStack is, Container ii, boolean overrideValid, int firstSlot, int maxSlot) {
        if (empty(is)) return true;
        int left = is.getCount();
        int limit = Math.min(ii.getMaxStackSize(), is.getMaxStackSize());
        for (int slot = Math.max(0, firstSlot); slot < Math.min(maxSlot, ii.getContainerSize()) && left > 0; slot++) {
            if (!overrideValid && !ii.canPlaceItem(slot, is)) continue;
            ItemStack present = ii.getItem(slot);
            if (empty(present) || ItemStack.isSameItemSameComponents(is, present))
                left -= Math.max(0, limit - (empty(present) ? 0 : present.getCount()));
        }
        return left <= 0;
    }

    public static boolean addOrSetStack(ItemStack is, Inventory inv, int slot) {
        return !empty(is) && addToIInv(is, inv, true, slot, slot + 1);
    }

    public static boolean addOrSetStack(ItemStack is, ManagedItemHandler inv, int slot) {
        return !empty(is) && addToIInv(is, inv, true, slot, slot + 1);
    }

    /**
     * Adds a certain amount of a specified ID to an inventory slot, creating the itemstack if necessary.
     * Returns true if the whole stack fit and was added. Args: ID, number, metadata (-1 for any), inventory, slot
     */
    public static boolean addOrSetStack(Item id, int size, ManagedItemHandler inv, int slot) {
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
            ii.setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * Add multiple items to an inventory. Args: Inventory, Items. Returns the ones that could not be added.
     */
    public static List<ItemStack> addMultipleItems(ManagedItemHandler ii, List<ItemStack> items) {
        List<ItemStack> extra = new ArrayList<ItemStack>();
        for (ItemStack item : items) {
            if (!addToIInv(item, ii))
                extra.add(item);
        }
        return extra;
    }

    /**
     * Gets the first block in an inventory, optionally consuming one. Args: Inventory, Decr yes/no
     */
    public static ItemStack getNextBlockInInventory(ItemStack[] inv, boolean decr) {
        for (int i = 0; i < inv.length; i++) {
            ItemStack stack = inv[i];
            if (!empty(stack) && stack.getItem() instanceof BlockItem) {
                ItemStack selected = stack.copyWithCount(1);
                if (decr) decrStack(i, inv);
                return selected;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Gets the first block in an inventory, optionally consuming one. Args: Inventory, Decr yes/no
     */
    public static ItemStack getNextBlockInInventory(ManagedItemHandler inv, boolean decr) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!empty(stack) && stack.getItem() instanceof BlockItem) {
                ItemStack selected = stack.copyWithCount(1);
                if (decr) decrStack(i, inv);
                return selected;
            }
        }
        return ItemStack.EMPTY;
    }
    /**
     * Returns whether an inventory is full. Args: Inventory
     */
    public static boolean isFull(Inventory ii) {
        for (int i = 0; i < ii.getContainerSize(); i++) {
            ItemStack is = ii.getItem(i);
            if (is.isEmpty())
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
            if (is.isEmpty())
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
        if (world.isClientSide()) return;
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack stack = inventory[slot];
            inventory[slot] = ItemStack.EMPTY;
            if (!empty(stack)) spill(world, x, y, z, stack.copy(), false);
        }
    }

    /**
     * Spills the entire inventory of an Inventory at the specified coordinates with a 1-block spread.
     * Args: Level, x, y, z, Inventory
     */
    public static void spillAndEmptyInventory(Level world, int x, int y, int z, ManagedItemHandler ii) {
        if (world.isClientSide()) return;
        for (int slot = 0; slot < ii.getSlots(); slot++) {
            ItemStack stack = ii.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                // Administrative inventory destruction must also clear slots disallowing automation extraction.
                ii.setStackInSlot(slot, ItemStack.EMPTY);
                spill(world, x, y, z, stack, true);
            }
        }
    }

    public static int getFirstNonEmptySlot(ManagedItemHandler ii) {
        for (int i = 0; i < ii.getSlots(); i++) {
            if (!ii.getStackInSlot(i).isEmpty())
                return i;
        }
        return -1;
    }

    /**
     * Adds an ItemStack to an inventory and returns how many items were successfully added.
     */
    public static int addStackAndReturnCount(ItemStack stack, ManagedItemHandler ii) {
        return addStackAndReturnCount(stack, ii, 0, ii.getSlots() - 1);
    }

    public static int addStackAndReturnCount(ItemStack stack, ManagedItemHandler ii, int slotMin, int slotMax) {
        int transferred = 0;
        int first = Math.max(0, slotMin);
        int last = Math.min(slotMax, ii.getSlots()-1);
        for (int idx = first; idx <= last && !stack.isEmpty(); idx++) {
            int before = stack.getCount();
            ItemStack remainder = ii.insertItem(idx, stack, false);
            transferred += before-remainder.getCount();
            stack.setCount(remainder.getCount());
        }
        return transferred;
    }

    public static ArrayList<ItemStack> getAllTransferrables(ManagedItemHandler source) {
        return new ArrayList<>(getLocatedTransferrables(source).values());
    }

    public static HashMap<Integer, ItemStack> getLocatedTransferrables(ManagedItemHandler source) {
        HashMap<Integer, ItemStack> stacks = new LinkedHashMap<>();
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ItemStack extracted = source.extractItem(slot, stack.getCount(), true);
                if (!extracted.isEmpty()) stacks.put(slot, extracted);
            }
        }
        return stacks;
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

    public static HashSet<Integer> getSlotsBetweenWithItemStack(ItemStack is, ManagedItemHandler ii, int min, int max, boolean matchSize) {
        HashSet<Integer> li = new HashSet<>();
        for (int i = min; i <= max; i++) {
            ItemStack in = ii.getStackInSlot(i);
            if (ReikaItemHelper.matchStacks(is, in)) {
                if (!matchSize || in.getCount() == is.getCount()) {
                    li.add(i);
                }
            }
        }
        return li;
    }

    public static boolean inventoryContains(ItemHashMap<Integer> map, ManagedItemHandler ii) {
        ItemHashMap<Integer> inv = ItemHashMap.getFromInventory(ii);
        for (ItemStack is : map.keySet()) {
            int need = map.get(is);
            Integer has = inv.get(is);
            if (has == null || need > has.intValue())
                return false;
        }
        return true;
    }

    public static void removeFromInventory(ItemHashMap<Integer> map, ManagedItemHandler ii) {
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
//                if (!empty(in)) {
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

    public static void addItems(ManagedItemHandler ii, ArrayList<ItemStack> li) {
        for (ItemStack is : li) {
            addToIInv(is, ii);
        }
    }

    /**
     * Returns the number successfully removed.
     */
    public static int drawFromInventory(ItemStack is, int max, Inventory inventory) {
        if (empty(is) || max <= 0) return 0;
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
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                    max -= rem;
                    amt += rem;
                    if (max <= 0)
                        break;
                }
            }
        }
        if (amt > 0) inventory.setChanged();
        return amt;
    }

    /**
     * Returns whether an inventory is empty. Args: Inventory
     */
    public static boolean isEmpty(Inventory ii) {
        return isEmptyFrom(ii, 0, ii.getContainerSize() - 1);
    }

    /**
     * Returns whether an inventory is empty. Args: ManagedItemHandler
     */
    public static boolean isEmpty(ManagedItemHandler ii) {
        return isEmptyFrom(ii, 0, ii.getSlots() - 1);
    }

    public static boolean isEmptyFrom(ManagedItemHandler ii, int from, int to) {
        for (int i = from; i <= to; i++) {
            ItemStack is = ii.getStackInSlot(i);
            if (!is.isEmpty())
                return false;
        }
        return true;
    }

    public static boolean isEmptyFrom(Container ii, int from, int to) {
        for (int i = from; i <= to; i++) {
            ItemStack is = ii.getItem(i);
            if (!is.isEmpty())
                return false;
        }
        return true;
    }

    public static ItemStack getSmallestStack(ItemStack[] inv) {
        return getSmallestStack(inv, 0, inv.length - 1);
    }

    public static ItemStack getSmallestStack(ItemStack[] inv, int min, int max) {
        ItemStack smallest = ItemStack.EMPTY;
        for (int i = min; i <= max; i++) {
            ItemStack in = inv[i];
            if (!empty(in)) {
                if (smallest.isEmpty() || smallest.getCount() > in.getCount())
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
