package reika.dragonapi.instantiable.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class DummyContainer extends AbstractContainerMenu {


	public DummyContainer(MenuType<?> p_38851_, int p_38852_) {
		super(p_38851_, p_38852_);
		//TODO Auto-generated constructor stub
	}

	@Override
	public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
		return null;
	}

	@Override
	public boolean stillValid(Player p_18946_) {
		// TODO Auto-generated method stub
		return false;
	}

}
