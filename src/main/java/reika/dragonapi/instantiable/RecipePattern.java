package reika.dragonapi.instantiable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import net.minecraft.world.inventory.CraftingContainer;

import java.util.function.Supplier;


public final class RecipePattern extends CraftingContainer {
    private static final BlankContainer craft = new BlankContainer(null, -1);

    private static final class BlankContainer extends AbstractContainerMenu {
        private BlankContainer(MenuType<?> type, int id) {
            super(type, id);
        }

        @Override
        public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player p_38874_) {
            return false;
        }
    }

    public RecipePattern(Container ii, int from) {
        super(craft, 3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int idx = i*3+j;
                this.setItem(i*3+j, ii.getItem(idx+from)); //no//since will otherwise add vertically
            }
        }
    }

    public RecipePattern(ItemStack... items) {
        super(craft, 3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int idx = i*3+j;
                if (idx < items.length)
                    this.setItem(i*3+j, items[idx]); //no//since will otherwise add vertically
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RecipePattern) {
            for (int i = 0; i < 9; i++) {
                if (!ReikaItemHelper.matchStacks(this.getItem(i), ((RecipePattern) o).getItem(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

}
