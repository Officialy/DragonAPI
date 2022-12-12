package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.instantiable.data.immutable.InventorySlot;
import reika.dragonapi.instantiable.gui.Slot.SlotNoClick;
import reika.dragonapi.interfaces.blockentity.MultiPageInventory;
import reika.dragonapi.interfaces.blockentity.XPProducer;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;

public class CoreMenu<T extends BlockEntityBase> extends AbstractContainerMenu {

    private static final ChestBlockEntity fakeChest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
    public final T tile;
    protected final Container ii;
    private final ArrayList<InventorySlot> relaySlots = new ArrayList<>();
    protected Inventory epInv;
    protected ItemStack[] oldInv;
    int posX;
    int posY;
    int posZ;
    private boolean alwaysCan = false;

    public CoreMenu(MenuType<?> type, int id, final Inventory playerInv, T te, Container i) {
        super(type, id);
        tile = te;
        posX = tile.getBlockPos().getX();
        posY = tile.getBlockPos().getY();
        posZ = tile.getBlockPos().getZ();
        epInv = playerInv;
//        this.broadcastChanges();
        ii = i;
    }

    public CoreMenu<T> setAlwaysInteractable() {
        alwaysCan = true;
        return this;
    }

    public CoreMenu<T> addSlotRelay(Inventory inv, int slot) {
        relaySlots.add(new InventorySlot(slot, inv));
        return this;
    }

    public boolean hasInventoryChanged(ItemStack[] inv) {
        for (int i = 0; i < oldInv.length; i++)
            if (!ItemStack.matches(oldInv[i], inv[i]))
                return true;
        return false;
    }

    protected void updateInventory(ItemStack[] inv) {
        System.arraycopy(inv, 0, oldInv, 0, oldInv.length);
    }

    protected void addPlayerInventoryWithOffset(Inventory player, int dx, int dy) {
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(player, k + i * 9 + 9, dx + 8 + k * 18, dy + 84 + i * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(player, j, dx + 8 + j * 18, dy + 142));
        }
    }

    protected void addPlayerInventory(Inventory player) {
        this.addPlayerInventoryWithOffset(player, 0, 0);
    }

    @Override
    public void broadcastChanges() { //todo reika commented out code so its not even used, see what it did?
        super.broadcastChanges();
		/*
		for (int i = 0; i < slots.size(); ++i) {
			ItemStack itemstack = ((Slot)slots.get(i)).getStack();
			ItemStack itemstack1 = (ItemStack)inventoryItemStacks.get(i);
			//DragonAPI.LOGGER.info("TRY: "+itemstack+":"+itemstack1, itemstack != null && itemstack1 != null);
			if (!ItemStack.areItemStacksEqual(itemstack1, itemstack) || true) { //the true is to force a sync
				itemstack1 = itemstack == null ? null : itemstack.copy();
				inventoryItemStacks.set(i, itemstack1);
				for (int j = 0; j < crafters.size(); ++j) {
					//DragonAPI.LOGGER.info("SEND: "+i+":"+itemstack1);
					((ICrafting)crafters.get(j)).sendSlotContents(this, i, itemstack1);
				}
			}
		}
		 */

        //for (int i = 0; i < crafters.size(); i++) {
        //	ICrafting icrafting = (ICrafting)crafters.get(i);
        //}

    }

    @Override
    public void setItem(int pSlotId, int pStateId, ItemStack pStack) {
        //DragonAPI.LOGGER.info("RECEIVE: "+slot+":"+is);
        if (slots.size() == 0)
            return;
        super.setItem(pSlotId, pStateId, pStack);
    }

//    @Override
//    public boolean canInteractWith(Player player) {
//        return alwaysCan || this.isStandard8mReach(player);
//    }

    public final boolean isStandard8mReach(Player player) {
        double dist = ReikaMathLibrary.py3d(tile.getBlockPos().getX() + 0.5 - player.getX(), tile.getBlockPos().getY() + 0.5 - player.getY(), tile.getBlockPos().getZ() + 0.5 - player.getZ());
        return (dist <= 8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        Slot islot = slots.get(slot);
        if (!this.allowShiftClicking(player, slot, islot.getItem()))
            return null;
        ItemStack ret = this.onShiftClickSlot(player, slot, islot.getItem());
        if (ret != null)
            return ret;
        ItemStack is = null;
        Slot fromSlot = islot;
        if (!(tile instanceof Container))
            return null;
        int invsize = ((Container) tile).getContainerSize();
        int base = 0;
        if (tile instanceof MultiPageInventory) {
            MultiPageInventory mp = (MultiPageInventory) tile;
            invsize = mp.getSlotsOnPage(mp.getCurrentPage());
            int cur = mp.getCurrentPage();
            for (int i = 0; i < cur; i++) {
                base += mp.getSlotsOnPage(i);
            }
        }

        if (fromSlot != null && fromSlot.hasItem()) {
            ItemStack inslot = fromSlot.getItem();
            is = inslot.copy();
            boolean toPlayer = slot < invsize + base;

            if (toPlayer) {
                for (int i = invsize + base; i < slots.size() && is.getCount() > 0; i++) {
                    //DragonAPI.LOGGER.info(i);
                    Slot toSlot = slots.get(i);
                    if (toSlot.mayPlace(is) && this.canAdd(is, toSlot.getItem())) {
                        if (!toSlot.hasItem()) {
                            toSlot.set(is.copy());
                            is.setCount(0);
                        } else {
                            ItemStack inToSlot = toSlot.getItem();
                            int add = inToSlot.getMaxStackSize() - inToSlot.getCount();
                            if (add > is.getCount())
                                add = is.getCount();
                            ItemStack toAdd = ReikaItemHelper.getSizedItemStack(is, inToSlot.getCount() + add);
                            //DragonAPI.LOGGER.info(is+" to "+inToSlot+" for "+toAdd+", by "+add);
                            toSlot.set(toAdd);
                            is.setCount(is.getCount() - add);
                        }
                        if (tile instanceof XPProducer) {
                            float xp = ((XPProducer) tile).getXP();
                            epInv.player.giveExperiencePoints((int) xp); //todo might be levels
                            ((XPProducer) tile).clearXP();
                        }
                    } else {

                    }
                }
                if (is.getCount() <= 0) {
                    fromSlot.set(ItemStack.EMPTY);
                } else {
                    fromSlot.set(is.copy());
                }
                is = null;
                return is;
            } else {
                List<Slot> list = this.getOrderedSlotList();
                for (int i = base; i < ((Container) tile).getContainerSize() && i < list.size() && is.getCount() > 0; i++) {
                    Slot toSlot = list.get(i);
                    int lim = ((Container) tile).getMaxStackSize();
                    //DragonAPI.LOGGER.info(i+" "+toSlot+":"+toSlot.getSlotIndex()+" E ["+base+", "+((Container)tile).getSizeInventory()+") > "+toSlot.isItemValid(is), Dist.DEDICATED_SERVER);
                    if (toSlot.mayPlace(is) && (((Container) tile).canPlaceItem(i, is)) && this.canAdd(is, toSlot.getItem())) {
                        if (!toSlot.hasItem()) {
                            if (is.getCount() <= lim) {
                                toSlot.set(is.copy());
                                //DragonAPI.LOGGER.info(toSlot.getSlotIndex());
                                is.setCount(0);
                            } else {
                                toSlot.set(ReikaItemHelper.getSizedItemStack(is, lim));
                                is.setCount(is.getCount() - lim);
                            }
                        } else {
                            ItemStack inToSlot = toSlot.getItem();
                            int add = Math.min(inToSlot.getMaxStackSize() - inToSlot.getCount(), lim - inToSlot.getCount());
                            if (add > is.getCount())
                                add = is.getCount();
                            toSlot.set(ReikaItemHelper.getSizedItemStack(is, inToSlot.getCount() + add));
                            is.setCount(is.getCount() - add);
                        }
                    } else {

                    }
                }
                if (is.getCount() <= 0) {
                    fromSlot.set(ItemStack.EMPTY);
                } else {
                    fromSlot.set(is.copy());
                }
                is = null;
                return is;
            }
        }

        return null;
    }

    public boolean allowShiftClicking(Player player, int slot, ItemStack stack) {
        return true;
    }

    /**
     * Return non-null here to stop all normal shift-click behavior
     */
    protected ItemStack onShiftClickSlot(Player player, int slot, ItemStack is) {
        return null;
    }

    private boolean canAdd(ItemStack is, ItemStack inslot) {
        if (inslot == null)
            return true;
        return ReikaItemHelper.matchStacks(is, inslot) && ItemStack.tagMatches(is, inslot);
    }

    @Override //To avoid a couple crashes with some mods (or vanilla packet system) not checking array bounds
    public Slot getSlot(int index) {
        if (index >= slots.size() || index < 0) {
            String o = "A mod tried to access an invalid slot " + index + " for BlockEntity " + tile + ".";
            String o2 = "It is likely assuming the BlockEntity has an inventory when it does not.";
            String o3 = "Check for any inventory-modifying mods and items you are carrying.";
            String o4 = "Slot List = " + slots.size() + ": " + slots;
            DragonAPI.LOGGER.info(o);
            DragonAPI.LOGGER.info(o2);
            DragonAPI.LOGGER.info(o3);
            DragonAPI.LOGGER.info(o4);
            DragonAPI.LOGGER.info("Stack Trace:");
            Thread.dumpStack();
            if (DragonOptions.CHATERRORS.getState()) {
                ReikaChatHelper.write(o);
                ReikaChatHelper.write(o2);
                ReikaChatHelper.write(o3);
                ReikaChatHelper.write(o4);
            }
            //Thread.dumpStack();
            return new Slot(fakeChest, 0, -20, -20); //create new slot off screen; hacky fix, but should work
        }
        return slots.get(index);
    }

    @Override
    public boolean stillValid(Player player) {
        return !tile.isRemoved();
    }

    protected void addSlot(int i, int x, int y) {
        if (ii == null)
            return;
        this.addSlot(new Slot(ii, i, x, y));
    }

    protected void addSlotNoClick(int i, int x, int y) {
        this.addSlotNoClick(i, x, y, false, true);
    }

    protected void addSlotNoClick(int i, int x, int y, boolean add, boolean take) {
        if (ii == null)
            return;
        this.addSlot(new SlotNoClick(ii, i, x, y, add, take));
    }

    @Override
    public void clicked(int id, int button, ClickType type, Player ep) {
        //DragonAPI.LOGGER.info(ID, Dist.DEDICATED_SERVER);
//        ItemStack is = super.slotClick(id, button, type, ep);
        if (ii != null && tile instanceof XPProducer) {
            if (id < ii.getContainerSize()) {
                float xp = ((XPProducer) tile).getXP();
                if (xp > 0) {
                    ep.giveExperiencePoints((int) xp); //todo might be levels
                    ((XPProducer) tile).clearXP();
                    ep.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, 1);
                }
            }
        }
//        return is;
        super.clicked(id, button, type, ep);
    }

    private List<Slot> getOrderedSlotList() {
        List<Slot> copy = new ArrayList<>(slots);
        copy.sort(new SlotComparator());
        Iterator<Slot> it = copy.iterator();
        while (it.hasNext()) {
            Slot s = it.next();
            if (s.container instanceof Inventory)
                it.remove();
        }

//		StringBuilder sb = new StringBuilder();
//		sb.append("[");
//		for (int i = 0; i < copy.size(); i++) {
//			Slot slot = copy.get(i);
//			sb.append(slot.getSlotIndex()+":"+slot.getClass().getSimpleName()+":"+slot.container);
//			sb.append(", ");
//		}
//		sb.append("]");
//		DragonAPI.LOGGER.info(sb.toString());

        return copy;
    }

    @Override
    public OptionalInt findSlot(Container container, int slot) {
        OptionalInt s = super.findSlot(container, slot);
        if (s == null) {
            for (InventorySlot is : relaySlots) {
                if (is.inventory == ii && is.slot == slot) {
//                    return is.toSlot(-20, -20).getContainerSlot();
                    return OptionalInt.of(is.toSlot(-20, -20).getSlotIndex());
                }
            }
        }
        return OptionalInt.empty();
    }

    private static class SlotComparator implements Comparator<Slot> {

        @Override
        public int compare(Slot o1, Slot o2) {
            return o1.getSlotIndex() - o2.getSlotIndex();
        }

    }

}
