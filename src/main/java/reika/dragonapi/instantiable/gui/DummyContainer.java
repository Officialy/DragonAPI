package reika.dragonapi.instantiable.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Slotless menus have no backing block or inventory reach restriction. */
public class DummyContainer extends AbstractContainerMenu {


	public DummyContainer(MenuType<?> p_38851_, int p_38852_) {
		super(p_38851_, p_38852_);
	}

	@Override
	public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player p_18946_) {
		return true;
	}

}
