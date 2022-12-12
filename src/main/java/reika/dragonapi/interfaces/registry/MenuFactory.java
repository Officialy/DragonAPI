package reika.dragonapi.interfaces.registry;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface MenuFactory {
    AbstractContainerMenu create(int id, Inventory inventory);
}