package reika.dragonapi.interfaces;

import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface BlockCheck {

    boolean matchInWorld(Level world, BlockPos pos);

    boolean match(BlockState b);

    boolean match(BlockCheck bc);

    void place(Level world, BlockPos pos, int flags);

    ItemStack asItemStack();


    ItemStack getDisplay();

    BlockKey asBlockKey();

    interface BlockEntityCheck extends BlockCheck {


        BlockEntity getBlockEntity();

    }
}
