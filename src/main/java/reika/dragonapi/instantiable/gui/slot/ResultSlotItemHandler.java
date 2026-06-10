package reika.dragonapi.instantiable.gui.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import reika.dragonapi.instantiable.storage.ManagedItemHandler;

/**
 * Output-only "result" slot: rejects placement, runs achievement hooks on extraction.
 *
 * <p>1.21.9: was a {@code SlotItemHandler} subclass. The whole {@code IItemHandler} family
 * was deprecated; we now extend the new {@link ResourceHandlerSlot} and bind to a
 * {@link ManagedItemHandler} via its {@code set} index modifier so callers don't have to
 * supply the {@code IndexModifier} themselves.
 */
public class ResultSlotItemHandler extends ResourceHandlerSlot {

    public ResultSlotItemHandler(ManagedItemHandler handler, int index, int xPosition, int yPosition) {
        super(handler, handler::set, index, xPosition, yPosition);
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
