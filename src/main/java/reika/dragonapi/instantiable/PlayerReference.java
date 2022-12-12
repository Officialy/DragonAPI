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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class PlayerReference {

	public final UUID uid;
	private final ItemStack heldItem;

	public PlayerReference(Player ep) {
		uid = ep.getUUID();
		heldItem = ep.getMainHandItem();
	}

	public ItemStack getHeldItem() {
		return heldItem != null ? heldItem.copy() : null;
	}

	public Player getPlayer(Level world) {
		return world.getPlayerByUUID(uid);
	}

}
