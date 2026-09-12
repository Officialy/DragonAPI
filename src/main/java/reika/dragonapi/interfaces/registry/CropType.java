package reika.dragonapi.interfaces.registry;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CropType {
    boolean existsInGame();
    boolean isRipe(Level level, BlockPos pos);
    void setHarvested(Level level, BlockPos pos);
    void makeRipe(Level level, BlockPos pos);
    int getGrowthState(Level level, BlockPos pos);
    boolean isSeedItem(ItemStack stack);
    boolean destroyOnHarvest();
    List<ItemStack> getDrops(Level level, BlockPos pos, int fortune);
    boolean isCrop(BlockState state);
    boolean neverDropsSecondSeed();

    final class CropMethods {
        private CropMethods() {}

        public static void removeOneSeed(CropType crop, List<ItemStack> drops) {
            if (crop.neverDropsSecondSeed()) return;
            var iterator = drops.iterator();
            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                if (crop.isSeedItem(stack)) {
                    if (stack.getCount() > 1) stack.shrink(1);
                    else iterator.remove();
                    return;
                }
            }
        }
    }
}
