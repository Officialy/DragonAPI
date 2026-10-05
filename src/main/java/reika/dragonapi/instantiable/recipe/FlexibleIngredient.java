package reika.dragonapi.instantiable.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;

import reika.dragonapi.instantiable.ItemMatch;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.io.CustomRecipeList;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public final class FlexibleIngredient {

	public static final FlexibleIngredient EMPTY = new FlexibleIngredient();

	private final ItemMatch filter;
	private final TagKey<Item> itemTag;
	public final float chanceToUse;
	public final int numberToUse;

	private FlexibleIngredient() {
		this((ItemMatch)null, 0, 1);
	}

	public FlexibleIngredient(Block in, float chance, int toDecr) {
		this(new ItemMatch(in), chance, toDecr);
	}

	public FlexibleIngredient(Item in, float chance, int toDecr) {
		this(new ItemMatch(in), chance, toDecr);
	}

	public FlexibleIngredient(ItemStack in, float chance, int toDecr) {
		this(in != null && !in.isEmpty() ? new ItemMatch(in) : null, chance, toDecr);
	}

	public FlexibleIngredient(String ore, float chance, int toDecr) {
		this(ItemMatch.empty(), ReikaItemHelper.getOreTag(ore), chance, toDecr);
		if (itemTag == null)
			throw new IllegalArgumentException("Invalid ore ingredient name: " + ore);
	}

	public FlexibleIngredient(Collection<ItemStack> in, float chance, int toDecr) {
		this(in != null ? new ItemMatch(in) : null, chance, toDecr);
	}

	public FlexibleIngredient(ItemMatch in, float chance, int toDecr) {
		this(in, null, chance, toDecr);
	}

	private FlexibleIngredient(ItemMatch in, TagKey<Item> tag, float chance, int toDecr) {
		filter = in;
		itemTag = tag;
		chanceToUse = chance/100F;
		numberToUse = toDecr;
	}

	public void addItem(ItemStack is) {
		if (filter != null) {
			filter.addItem(new KeyedItemStack(is).setIgnoreNBT(true).setSized(false).setSimpleHash(true));
		}
	}

	public FlexibleIngredient lock() {
		if (filter != null) {
			for (KeyedItemStack ks : filter.getItemList()) {
				ks.lock();
			}
		}
		return this;
	}

	public ItemStack getItemForDisplay(boolean size) {
		if (!this.exists())
			return ItemStack.EMPTY;
		Iterator<ItemStack> it = this.getItems().iterator();
		if (!it.hasNext())
			return ItemStack.EMPTY;
		ItemStack base = it.next();
		return base.copyWithCount(size ? numberToUse : 1);
	}

	@Override
	public String toString() {
		return (itemTag != null ? itemTag.location() + " + " : "")
				+ (filter != null ? filter.getItemList() : "[]") + " x" + numberToUse + "@" + chanceToUse + "%";
	}

	public String fullID(IngredientIDHandler id) {
		if (!this.exists())
			return "Empty";
		String explicit = filter != null && !filter.isEmpty() ? id.fullIDForItems(filter.getItemList()) : "";
		return (itemTag != null ? itemTag.location() + (explicit.isEmpty() ? "" : " + ") : "")
				+ explicit + " x" + numberToUse + "@" + chanceToUse + "%";
	}

	/** Does NOT check size, just identity! */
	public boolean match(ItemStack in) {
		return this.exists() ? in != null && !in.isEmpty()
				&& ((itemTag != null && in.is(itemTag)) || (filter != null && filter.match(in)))
				: (in == null || in.isEmpty());
	}

	private boolean checkSize(ItemStack is) {
		return !this.exists() || is.getCount() >= numberToUse;
	}

	public boolean matchWithSize(ItemStack in) {
		return this.match(in) && this.checkSize(in);
	}

	public boolean exists() {
		return (filter != null && !filter.isEmpty()) || (itemTag != null
				&& BuiltInRegistries.ITEM.get(itemTag).filter(items -> items.size() > 0).isPresent());
	}

	public Collection<ItemStack> getItems() {
		ArrayList<ItemStack> c = new ArrayList<>();
		if (filter != null)
			for (KeyedItemStack ks : filter.getItemList())
				c.add(ks.getItemStack());
		if (itemTag != null)
			BuiltInRegistries.ITEM.get(itemTag).ifPresent(items ->
					items.forEach(holder -> c.add(new ItemStack(holder.value()))));
		return c;
	}

	public static FlexibleIngredient parseLua(CustomRecipeList crl, LuaBlock b, boolean allowEmptyItemList) {
		if (b == null)
			return EMPTY;
		Collection<ItemStack> li = b.hasChild("items") ? CustomRecipeList.parseItemCollection(b.getChild("items").getDataValues(), true) : null;
		if (li == null || li.isEmpty()) {
			if (allowEmptyItemList)
				return EMPTY;
			else
				throw new RuntimeException("Lua block "+b.name+" found no items!");
		}
		int num = b.getInt("number_to_use");
		if (num <= 0)
			throw new IllegalArgumentException("No number to use specified!");
		if (!b.containsKeyInherit("consumption_chance"))
			throw new IllegalArgumentException("No consumption chance specified!");
		return new FlexibleIngredient(li, (float)b.getDouble("consumption_chance"), num);
	}

	public interface IngredientIDHandler {

		String fullIDForItems(Collection<KeyedItemStack> c);

	}
}
