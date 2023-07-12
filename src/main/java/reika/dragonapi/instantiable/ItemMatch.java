package reika.dragonapi.instantiable;

import net.minecraft.nbt.Tag;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class ItemMatch {

    private final HashSet<KeyedItemStack> items = new HashSet();

    private final ArrayList<ItemStack> displayList = new ArrayList<>();

    private ItemMatch() {

    }

    private ItemMatch(HashSet<KeyedItemStack> set, ArrayList<ItemStack> li) {
        items.addAll(set);
        displayList.addAll(li);
    }

    public ItemMatch(Block b) {
        this.addItem(new KeyedItemStack(b));
    }

    public ItemMatch(Item i) {
        this.addItem(new KeyedItemStack(i));
    }

    public ItemMatch(ItemStack is) {
        this.addItem(new KeyedItemStack(is));
    }

    public ItemMatch(Collection<ItemStack> c) {
        this.addAll(c);
        if (items.isEmpty())
            throw new RegistrationException(DragonAPI.instance, "This recipe uses an list with no items!");
    }

    public static ItemMatch createFromObject(Object o) {
        if (o instanceof Block) {
            return new ItemMatch((Block) o);
        } else if (o instanceof Item) {
            return new ItemMatch((Item) o);
        } else if (o instanceof ItemStack) {
            return new ItemMatch((ItemStack) o);
        } else {
            throw new IllegalArgumentException("Invalid item matching type!");
        }
    }

    private void addAll(Collection<ItemStack> li) {
        for (ItemStack is : li) {
            this.addItem(new KeyedItemStack(is));
        }
    }

    public ItemMatch copy() {
        ItemMatch m = new ItemMatch();
        m.items.addAll(items);
        m.displayList.addAll(displayList);
        return m;
    }

    public ItemMatch addItem(KeyedItemStack ks) {
        ks = ks.setSimpleHash(true).setIgnoreNBT(ks.getItemStack().getTag() == null).lock();
        items.add(ks);
        //if (FMLLoader.getDist() == Dist.CLIENT)
        ItemStack is2 = ks.getItemStack();
        displayList.add(is2);
        return this;
    }

    public boolean match(ItemStack is) {
		/*
		for (KeyedItemStack in : items) {
			if (ReikaItemHelper.matchStacks(in, is) && (in.stackTagCompound == null || ItemStack.isSameItemSameTags(in, is))) {
				return true;
			}
		}
		return false;
		 */
        return is != null && items.contains(new KeyedItemStack(is).setSimpleHash(true));
    }

    public Set<KeyedItemStack> getItemList() {
        return Collections.unmodifiableSet(items);
    }

    @Override
    public String toString() {
        return items.toString();
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ItemMatch m) {
            /*
			if (m.items.size() == items.size()) {
				for (ItemStack is : items) {
					if (ReikaItemHelper.listContainsItemStack(m.items, is, false)) {
					}
					else {
						return false;
					}
				}
				return true;
			}
			else {
				return false;
			}
			 */
            return m.items.equals(items);
        } else {
            return false;
        }
    }

    public void saveAdditional(CompoundTag NBT) {
        ListTag li = new ListTag();
        for (KeyedItemStack ks : items) {
            CompoundTag tag = new CompoundTag();
            ks.saveAdditional(tag);
            li.add(tag);
        }
        NBT.put("items", li);

        li = new ListTag();
        for (ItemStack is : displayList) {
            CompoundTag tag = new CompoundTag();
            is.save(tag);
            li.add(tag);
        }
        NBT.put("display", li);
    }

    public static ItemMatch load(CompoundTag NBT) {
        ArrayList<ItemStack> dis = new ArrayList<>();
        HashSet<KeyedItemStack> set = new HashSet();
        ListTag li = NBT.getList("items", Tag.TAG_COMPOUND);
        for (Object o : li) {
            CompoundTag tag = (CompoundTag) o;
            KeyedItemStack ks = KeyedItemStack.load(tag);
            set.add(ks);
        }

        li = NBT.getList("display", Tag.TAG_COMPOUND);
        for (Object o : li) {
            CompoundTag tag = (CompoundTag) o;
            ItemStack is = ItemStack.of(tag);
            dis.add(is);
        }

        return new ItemMatch(set, dis);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
