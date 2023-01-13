package reika.dragonapi.instantiable.gui.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ResultSlotItemHandler extends SlotItemHandler {

    public ResultSlotItemHandler(IItemHandler handler, int index, int xPosition, int yPosition) {
        super(handler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack p_39553_) {
        return false;
    }

    @Override
    public void onTake(Player p_150563_, ItemStack p_150564_) {
        this.checkTakeAchievements(p_150564_);
        super.onTake(p_150563_, p_150564_);
    }

    @Override
    protected void onQuickCraft(ItemStack p_39555_, int p_39556_) {
        this.checkTakeAchievements(p_39555_);
    }

}
