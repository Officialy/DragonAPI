package reika.dragonapi.instantiable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.*;

public class ItemMatch {

    private final HashSet<KeyedItemStack> items = new HashSet();

    private final ArrayList<ItemStack> displayList = new ArrayList<>();

    /**
     * 1.7.10 ore-dictionary names became item tags. Tag contents are only known once tags load (often
     * after a static ItemMatch is built), so tags are tested at match time instead of being expanded.
     */
    private final LinkedHashSet<TagKey<Item>> tags = new LinkedHashSet<>();

    private ItemMatch() {

    }

    /** Mutable match set for ingredients that gain concrete alternatives after construction. */
    public static ItemMatch empty() {
        return new ItemMatch();
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

    public ItemMatch(TagKey<Item> tag) {
        tags.add(tag);
    }

    /** One matcher satisfied by any of the tags (1.7.10's "oreA/oreB" alternative syntax). */
    @SafeVarargs
    public static ItemMatch anyOf(TagKey<Item>... tags) {
        ItemMatch m = new ItemMatch();
        m.tags.addAll(Arrays.asList(tags));
        return m;
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
        } else if (o instanceof TagKey<?> t && t.isFor(Registries.ITEM)) {
            @SuppressWarnings("unchecked")
            TagKey<Item> tag = (TagKey<Item>) t;
            return new ItemMatch(tag);
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
        m.tags.addAll(tags);
        m.displayList.addAll(displayList);
        return m;
    }

    public ItemMatch addItem(KeyedItemStack ks) {
        ks = ks.setSimpleHash(true).setIgnoreNBT(!reika.dragonapi.instantiable.data.ItemKeyComponents.hasAdditionalChanges(ks.getItemStack())).lock();
        items.add(ks);
        //if (FMLEnvironment.getDist() == Dist.CLIENT)
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
        if (is == null || is.isEmpty())
            return false;
        for (TagKey<Item> tag : tags)
            if (is.is(tag))
                return true;
        return items.stream().anyMatch(key -> key.match(is));
    }

    public Set<TagKey<Item>> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Set<KeyedItemStack> getItemList() {
        return Collections.unmodifiableSet(items);
    }

    @Override
    public String toString() {
        return tags.isEmpty() ? items.toString() : items + " + tags " + tags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, tags);
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
            return m.items.equals(items) && m.tags.equals(tags);
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
            tag.putString("id", BuiltInRegistries.ITEM.getKey(is.getItem()).toString());
            tag.putInt("count", is.getCount());
            li.add(tag);
        }
        NBT.put("display", li);

        li = new ListTag();
        for (TagKey<Item> tag : tags) {
            CompoundTag t = new CompoundTag();
            t.putString("tag", tag.location().toString());
            li.add(t);
        }
        NBT.put("tags", li);
    }

    public static ItemMatch load(CompoundTag NBT) {
        ArrayList<ItemStack> dis = new ArrayList<>();
        HashSet<KeyedItemStack> set = new HashSet();
        ListTag li = NBT.getListOrEmpty("items");
        for (Object o : li) {
            CompoundTag tag = (CompoundTag) o;
            KeyedItemStack ks = KeyedItemStack.load(tag);
            set.add(ks);
        }

        li = NBT.getListOrEmpty("display");
        for (Object o : li) {
            CompoundTag tag = (CompoundTag) o;
            ItemStack is = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
            dis.add(is);
        }

        ItemMatch m = new ItemMatch(set, dis);
        for (Object o : NBT.getListOrEmpty("tags")) {
            Identifier id = Identifier.tryParse(((CompoundTag) o).getStringOr("tag", ""));
            if (id != null)
                m.tags.add(TagKey.create(Registries.ITEM, id));
        }
        return m;
    }

    public boolean isEmpty() {
        return items.isEmpty() && tags.isEmpty();
    }
}



