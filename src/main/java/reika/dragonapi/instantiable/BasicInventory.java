/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public abstract class BasicInventory implements Container {

	public final int inventorySize;
	public final int stackLimit;
	public final String name;

	protected ItemStack[] inv;

	public BasicInventory(String n, int size) {
		this(n, size, 64);
	}

	public BasicInventory(String n, int size, int limit) {
		inventorySize = size;
		stackLimit = limit;
		inv = new ItemStack[size];
		name = n;
	}

	@Override
	public final int getContainerSize() {
		return inventorySize;
	}

	@Override
	public final ItemStack getItem(int slot) {
		return inv[slot];
	}

	public final void setInventorySlotContents(int slot, ItemStack is) {
		inv[slot] = is;
	}


	public final String getInventoryName() {
		return name;
	}


	public final boolean hasCustomInventoryName() {
		return true;
	}

	@Override
	public int getMaxStackSize() {
		return stackLimit;

	}
	@Override
	public boolean stillValid(Player ep) {
		return false;
	}

	@Override
	public void startOpen(Player ep) {
	}

	@Override
	public void stopOpen(Player ep) {
	}

	public final ItemStack[] getItems() {
		return Arrays.copyOf(inv, inv.length);
	}

}
