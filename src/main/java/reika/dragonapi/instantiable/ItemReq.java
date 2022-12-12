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

import reika.dragonapi.exception.MisuseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Random;

public class ItemReq {

	public final Item itemID;
	public final float chanceToUse;
	private int numberNeeded;

	private CompoundTag nbt;

	public ItemReq(Item id, float chance) {
		itemID = id;
		if (chance > 1)
			chance = 1;
		chanceToUse = chance;
		numberNeeded = -1;
	}

	public ItemReq(Block b, float chance) {
		this(Item.BY_BLOCK.get(b), chance);
	}

	public ItemReq(Item id, int number) {
		if (number < 1) {
			throw new MisuseException("You must specify a valid number of items required!");
		}
		itemID = id;
		chanceToUse = -1;
		numberNeeded = number;
	}

	public boolean alwaysConsume() {
		return numberNeeded != -1 || chanceToUse == 1;
	}

	public void setNBTTag(CompoundTag tag) {
		nbt = tag;
	}

	public int getNumberNeeded() {
		return numberNeeded;
	}

	public void use() {
		if (numberNeeded > 0)
			numberNeeded--;
	}

	public boolean callAndConsume() {
		if (numberNeeded > 0)
			numberNeeded--;
		int chance = (int) (1F / chanceToUse);
		Random r = new Random();
		return r.nextInt(chance) <= 0;
	}

	public ItemStack asItemStack() {
		if (numberNeeded != -1)
			return new ItemStack(itemID, numberNeeded);
		else if (this.alwaysConsume())
			return new ItemStack(itemID, 1);
		else
			return new ItemStack(itemID, 1);//return new ItemStack.getItem, (int)(100*chanceToUse), metadata);
	}

}
