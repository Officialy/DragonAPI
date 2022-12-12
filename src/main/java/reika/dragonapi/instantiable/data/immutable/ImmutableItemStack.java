/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

import reika.dragonapi.interfaces.registry.BlockEnum;
import reika.dragonapi.interfaces.registry.ItemEnum;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ImmutableItemStack {

	private final ItemStack data;

	public ImmutableItemStack(Item i) {
		this(new ItemStack(i));
	}

	public ImmutableItemStack(Block b) {
		this(new ItemStack(b));
	}

	public ImmutableItemStack(ItemEnum i) {
		this(new ItemStack(i.getItem()));
	}

	public ImmutableItemStack(BlockEnum b) {
		this(new ItemStack(b.getBlock()));
	}

	public ImmutableItemStack(Item i, int num) {
		this(new ItemStack(i, num));
	}

	public ImmutableItemStack(ItemStack is) {
		data = is.copy();
	}

	public int getCount() {
		return data.getCount();
	}

	public Item getItem() {
		return data.getItem();
	}

	public int getMaxStackSize() {
		return data.getMaxStackSize();
	}

	public ItemStack getItemStack() {
		return data.copy();
	}

	public boolean match(ItemStack is) {
		return is == this.getItemStack();
	}

	public boolean match(ImmutableItemStack is) {
		return is.getItemStack() == this.getItemStack();
	}

}
