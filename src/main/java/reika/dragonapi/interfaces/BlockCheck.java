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

    public boolean matchInWorld(Level world, BlockPos pos);

    public boolean match(BlockState b);

    public boolean match(BlockCheck bc);

    public void place(Level world, BlockPos pos, int flags);

    public ItemStack asItemStack();


    public ItemStack getDisplay();

    public BlockKey asBlockKey();

    public static interface BlockEntityCheck extends BlockCheck {


        public BlockEntity getBlockEntity();

    }
}
