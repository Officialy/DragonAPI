/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.lua;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.libraries.ReikaInventoryHelper;

public class LuaHasItem extends LuaMethod {

	public LuaHasItem() {
		super("hasItem", Container.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		if (args.length < 1 || args.length > 3)
			throw new LuaMethodException("Expected item registry name (or legacy numeric ID), optional damage, optional count");
		Item item;
		if (args[0] instanceof String name) {
			var key = net.minecraft.resources.Identifier.tryParse(name);
			item = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key).orElse(null);
		} else {
			int id = LuaArguments.integer(args[0], "item ID", 0, Integer.MAX_VALUE);
			item = net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(id);
		}
		if (item == null || item == net.minecraft.world.item.Items.AIR)
			throw new LuaMethodException("Unknown item: " + args[0]);
		int damage = args.length >= 2 ? LuaArguments.integer(args[1], "damage", -1, Integer.MAX_VALUE) : -1;
		int count = args.length >= 3 ? LuaArguments.integer(args[2], "count", 1, Integer.MAX_VALUE) : -1;
		Container inventory = (Container)te;
		ItemStack[] contents = new ItemStack[inventory.getContainerSize()];
		for (int i = 0; i < contents.length; i++) contents[i] = inventory.getItem(i);
		return new Object[]{contains(contents, item, damage, count, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()))};
	}

	private static boolean contains(ItemStack[] contents, Item item, int damage, int count, java.util.Set<ItemStack> visited) {
		for (ItemStack stack : contents) {
			if (stack == null || stack.isEmpty()) continue;
			if (stack.is(item) && (damage == -1 || damage == 32767 || stack.getDamageValue() == damage)
					&& (count == -1 || stack.getCount() == count)) return true;
			if (stack.getItem() instanceof reika.dragonapi.interfaces.item.ActivatedInventoryItem nested && visited.add(stack)
					&& contains(nested.getInventory(stack), item, damage, count, visited)) return true;
		}
		return false;
	}

	@Override
	public String getDocumentation() {
		return "Checks for the item in an inventory.\nArgs: registry name or legacy ID, damage (optional; -1/32767 wildcard), exact stack count (optional)\nReturns: true/false";
	}

	@Override
	public String getArgsAsString() {
		return "string item or int id, int damage*, int count*";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
