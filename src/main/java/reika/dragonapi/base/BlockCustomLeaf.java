package reika.dragonapi.base;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.immutable.BlockBox;

import java.util.ArrayList;
import java.util.Random;

public abstract class BlockCustomLeaf extends LeavesBlock {

    protected final Random rand = new Random();

    protected BlockCustomLeaf(BlockBehaviour.Properties properties, boolean tick) {
        super(properties.destroyTime(0.2f).randomTicks().sound(SoundType.GRASS));
//        if (FMLLoader.getDist() == Dist.CLIENT)
//            this.setGraphicsLevel(Minecraft.getInstance().options.fancyGraphics);
    }

    public abstract boolean isNatural();

    /**
     * Chance that fire will spread and consume this block.
     * 300 being a 100% chance, 0, being a 0% chance.
     *
     * @param state The current state
     * @param world The current world
     * @param pos   Block position in world
     * @param face  The face that the fire is coming from
     * @return A number ranging from 0 to 300 relating used to determine if the block will be consumed by fire
     */
    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 30;
    }

    /**
     * Called when fire is updating on a neighbor block.
     * The higher the number returned, the faster fire will spread around this block.
     *
     * @param state The current state
     * @param world The current world
     * @param pos   Block position in world
     * @param face  The face that the fire is coming from
     * @return A number that is used to determine the speed of fire growth around the block
     */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 60;
    }

}
