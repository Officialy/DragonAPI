package reika.dragonapi.instantiable;

import net.minecraftforge.event.entity.player.PlayerEvent;
import reika.dragonapi.auxiliary.trackers.PlayerFirstTimeTracker;
import reika.dragonapi.auxiliary.trackers.PlayerHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.libraries.ReikaInventoryHelper;

public abstract class ItemOnSpawnTracker implements PlayerFirstTimeTracker.PlayerTracker {

	@Override
	public void onNewPlayer(Player ep) {
		if (ReikaInventoryHelper.checkForItemStack(this.getItem(), ep.getInventory(), false))
			return;
		if (!ep.getInventory().add(this.getItem())) ep.drop(this.getItem(), true);
	}


	public abstract ItemStack getItem();

}
