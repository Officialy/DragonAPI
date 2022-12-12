package reika.dragonapi.interfaces.blockentity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;

public interface CraftingTile<V> extends Container {

    CraftingContainer constructContainer();

    boolean handleCrafting(V wr, Player ep, boolean keyDown);

    boolean isReadyToCraft();

    int getOutputSlot();

    V getToCraft();

    void setToCraft(V recipe);

}
