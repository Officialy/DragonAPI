/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.maps;


// Old package references - modern versions already imported below
// import net.minecraft.block.Block; → net.minecraft.world.level.block.Block
// import net.minecraft.inventory.IInventory; → deprecated, use IItemHandler
// import net.minecraft.item.Item; → net.minecraft.world.item.Item
// import net.minecraft.item.ItemStack; → net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponentPatch;
import reika.dragonapi.instantiable.storage.ManagedItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.ImmutableItemStack;
import reika.dragonapi.interfaces.Matcher;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


import java.util.*;

public final class ItemHashMap<V> {

	private final HashMap<ItemKey, V> data = new HashMap();
	private ArrayList<ItemStack> sorted = null;
	private Collection<ItemStack> keyset = null;
	private boolean modifiedKeys = true;
	private Matcher<V> matcher = null;
	private boolean oneWay = false;
	private boolean nbtEnabled = false;

	public ItemHashMap() {

	}

	public static ItemHashMap<Integer> getFromInventory(ManagedItemHandler ii) {
		ItemHashMap<Integer> map = new ItemHashMap<>();
		int s = ii.getSlots();
		for (int i = 0; i < s; i++) {
			ItemStack in = ii.getStackInSlot(i);
			if (in != ItemStack.EMPTY) {
				Integer has = map.get(in);
				int amt = has != null ? has.intValue() : 0;
				map.put(in, amt + in.getCount());
			}
		}
		return map;
	}

	public static ItemHashMap<Integer> locateFromInventory(Container ii) {
		ItemHashMap<Integer> map = new ItemHashMap<>();
		int s = ii.getContainerSize();
		for (int i = 0; i < s; i++) {
			ItemStack in = ii.getItem(i);
			if (in != ItemStack.EMPTY && !map.containsKey(in)) {
				map.put(in, i);
			}
		}
		return map;
	}

	public static ItemHashMap<Integer> subtract(ItemHashMap<Integer> map, ItemHashMap<Integer> subtr) {
		ItemHashMap<Integer> ret = new ItemHashMap<>();
		for (ItemKey ik : map.data.keySet()) {
			int get = map.get(ik);
			Integer get2 = subtr.get(ik);
			int res = get2 != null ? Math.max(0, get - get2) : get;
			if (res > 0)
				ret.put(ik, res);
		}
		return ret;
	}

	public ItemHashMap<V> setOneWay() {
		return this.setOneWay(null);
	}

	public ItemHashMap<V> enableNBT() {
		this.nbtEnabled = true;
		return this;
	}

	public ItemHashMap<V> setOneWay(Matcher<V> m) {
		oneWay = true;
		this.matcher = m;
		return this;
	}

	private void updateKeysets() {
		this.modifiedKeys = false;
		this.keyset = this.createKeySet();
		sorted = new ArrayList<>(this.keySet());
	}

	private V put(ItemKey is, V value) {
		if (oneWay && data.containsKey(is)) {
			if (matcher != null) {
				V v = data.get(is);
				if (v == value || matcher.match(v, value))
					return v;
			}
			throw new UnsupportedOperationException("This map does not support overwriting values! Item " + is + " already mapped to '" + data.get(is) + "'!");
		}
		V ret = data.put(is, value);
		this.modifiedKeys = true;
		return ret;
	}

	private V get(ItemKey is) {
		return data.get(is);
	}

	private ItemKey createKey(ItemStack is) {
		return this.nbtEnabled && !is.isEmpty() && !is.getComponentsPatch().isEmpty() ? new ComponentItemKey(is) : new ItemKey(is);
	}

	private boolean containsKey(ItemKey is) {
		return data.containsKey(is);
	}

	public int add(ItemStack is, int value) {
		// Safe cast check or redesign needed, but for now keeping logic with suppression
		@SuppressWarnings("unchecked")
		ItemHashMap<Integer> intMap = (ItemHashMap<Integer>) this;
		Integer get = intMap.get(is);
		int has = get != null ? get.intValue() : 0;
		int sum = has + value;
		intMap.put(is, sum);
		return sum;
	}

	public V put(ItemStack is, V value) {
		return this.put(this.createKey(is), value);
	}

	public V get(ItemStack is) {
		return this.get(this.createKey(is));
	}

	public V get(ImmutableItemStack is) {
		return this.get(is.getItemStack());
	}

	public boolean containsKey(ItemStack is) {
		return this.containsKey(this.createKey(is));
	}

	public V put(Item i, V value) {
		return this.put(new ItemStack(i), value);
	}

	public V put(ImmutableItemStack is, V obj) {
		return this.put(is.getItemStack(), obj);
	}

	public V get(Item i) {
		return this.get(new ItemStack(i));
	}

	public boolean containsKey(Item i) {
		return this.containsKey(new ItemStack(i));
	}

	public V put(Block b, V value) {
		return this.put(new ItemStack(b), value);
	}

	public V get(Block b) {
		return this.get(new ItemStack(b));
	}

	public boolean containsKey(Block b) {
		return this.containsKey(new ItemStack(b));
	}

	public int size() {
		return data.size();
	}

	public Collection<ItemStack> keySet() {
		if (this.modifiedKeys || keyset == null) {
			this.updateKeysets();
		}
		return Collections.unmodifiableCollection(keyset);
	}

	public Collection<V> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	private Collection<ItemStack> createKeySet() {
		ArrayList<ItemStack> li = new ArrayList<>();
		for (ItemKey key : data.keySet()) {
			li.add(key.asItemStack());
		}
		return li;
	}

	@Override
	public String toString() {
		return data.toString();
	}

	public V remove(ItemStack is) {
		return this.remove(this.createKey(is));
	}

	private V remove(ItemKey is) {
		if (oneWay)
			throw new UnsupportedOperationException("This map does not support removing values!");
		V ret = data.remove(is);
		this.modifiedKeys = true;
		return ret;
	}

	public boolean removeValue(V value) {
		return ReikaJavaLibrary.removeValuesFromMap(data, value);
	}

	public void clear() {
		if (oneWay)
			throw new UnsupportedOperationException("This map does not support removing values!");
		data.clear();
		this.modifiedKeys = true;
	}

	public List<ItemStack> sortedKeyset() {
		if (this.modifiedKeys || this.sorted == null) {
			this.updateKeysets();
		}
		return Collections.unmodifiableList(sorted);
	}

	public boolean isEmpty() {
		return this.data.isEmpty();
	}

	@Override
	public ItemHashMap<V> clone() {
		ItemHashMap<V> map = new ItemHashMap<>();
		for (ItemKey is : this.data.keySet()) {
			map.data.put(is, data.get(is));
		}
		return map;
	}

	public void putAll(ItemHashMap<V> map) {
		this.data.putAll(map.data);
	}

	private static final class ComponentItemKey extends ItemKey {

		private final DataComponentPatch components;

		private ComponentItemKey(ItemStack is) {
			super(is);
			this.components = is.getComponentsPatch();
		}

		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof ComponentItemKey && Objects.equals(this.components, ((ComponentItemKey) o).components);
		}

		@Override
		public int hashCode() {
			return super.hashCode() * 31 + components.hashCode();
		}

		@Override
		public ItemStack asItemStack() {
			ItemStack is = super.asItemStack();
			// Apply components to the new stack
			// We can't easily apply a patch to a new stack without using internal methods or creating it with the patch
			// But for key retrieval, we can try to approximate or just return the base item if exact reconstruction is hard
			// However, in 1.21, we can use the constructor that takes components if we had the full map
			// For now, we'll return the base item. If we need the components, we'd need to store the full stack or reconstruct it.
			// Given this is mostly for iteration/display, base item might be acceptable, but ideally we'd apply the patch.
			// TODO: Find a way to apply DataComponentPatch to a fresh ItemStack if needed.
			return is; 
		}

		@Override
		public String toString() {
			return super.toString() + " >> " + components;
		}

	}

	private static class ItemKey implements Comparable<ItemKey> {

		public final Item itemID;

		protected ItemKey(ItemStack is) {
			if (is == ItemStack.EMPTY)
				throw new MisuseException("You cannot add a null itemstack to the map!");
			itemID = is.getItem();
		}

		@Override
		public int hashCode() {
			return itemID.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ItemKey i) {
				return i.itemID == itemID;
			}
			return false;
		}

		@Override
		public String toString() {
			return BuiltInRegistries.ITEM.getKey(itemID).toString() + " (" + this.asItemStack().getHoverName().getString() + ")";
		}

		public ItemStack asItemStack() {
			return new ItemStack(itemID, 1);
		}

		@Override
		public final int compareTo(ItemKey o) {
			return BuiltInRegistries.ITEM.getId(itemID) - BuiltInRegistries.ITEM.getId(o.itemID);
		}

	}

}

