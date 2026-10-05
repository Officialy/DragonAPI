package reika.dragonapi.instantiable;

import reika.dragonapi.auxiliary.trackers.PlayerFirstTimeTracker;

import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.libraries.ReikaInventoryHelper;

public abstract class ItemOnSpawnTracker implements PlayerFirstTimeTracker.PlayerTracker {

	@Override
	public void onNewPlayer(Player ep) {
		ItemStack stack = this.getItem().copy();
		if (ReikaInventoryHelper.checkForItemStack(stack, ep.getInventory(), false))
			return;
		// This routine fills only slots with room, then drops the remainder. Inventory.add can
		// discard an uninserted stack for creative players and can leave a partial remainder.
		ep.getInventory().placeItemBackInInventory(stack, false, Prediction.SERVER_ONLY);
	}


	public abstract ItemStack getItem();

}

