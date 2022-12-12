package reika.dragonapi.instantiable;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

public abstract class TemperatureEffect {

    public static final TemperatureEffect rockMelting = new BlockConversionEffect(1500){

        @Override
        protected void doAction(Level world, BlockPos pos, int temperature) {
            if (ReikaWorldHelper.isMeltable(world, pos, temperature))
                super.doAction(world, pos, temperature);
        }

        @Override
        protected Block getBlock(int temperature) {
            return Blocks.LAVA;
        }

    };

    public static final TemperatureEffect groundGlassing = new BlockConversionEffect(900){
        @Override
        protected Block getBlock(int temperature) {
            return Blocks.GLASS;
        }
    };

    public static final TemperatureEffect snowVaporization = new BlockConversionEffect(0){
        @Override
        protected Block getBlock(int temperature) {
            return Blocks.AIR;
        }
    };

    public static final TemperatureEffect iceMelting = new BlockConversionEffect(0){
        @Override
        protected Block getBlock(int temperature) {
            return Blocks.WATER;
        }
    };

    public static final TemperatureEffect woodIgnition = new IgnitionEffect(450);
    public static final TemperatureEffect woolIgnition = new IgnitionEffect(600);
    public static final TemperatureEffect tntIgnition = new IgnitionEffect(300);
    public static final TemperatureEffect plantIgnition = new IgnitionEffect(230);

    public final int minimumTemperature;

    protected TemperatureEffect(int temp) {
        minimumTemperature = temp;
    }

    public final void apply(Level world, BlockPos pos, int temperature, TemperatureCallback call) {
        this.doAction(world, pos, temperature);
        if (call != null) {
            call.onApplyTemperature(world, pos, temperature);
        }
    }

    private static class IgnitionEffect extends TemperatureEffect {

        protected IgnitionEffect(int temp) {
            super(temp);
        }

        @Override
        protected final void doAction(Level world, BlockPos pos, int temperature) {
            if (ReikaWorldHelper.flammable(world, pos))
                ReikaWorldHelper.ignite(world, pos);
        }

    }

    private static abstract class BlockConversionEffect extends TemperatureEffect {

        protected BlockConversionEffect(int temp) {
            super(temp);
        }

        @Override
        protected void doAction(Level world, BlockPos pos, int temperature) {
            world.setBlock(pos, this.getBlock(temperature).defaultBlockState(), 0);
        }

        protected abstract Block getBlock(int temperature);

    }

    protected abstract void doAction(Level world, BlockPos pos, int temperature);

    public static interface TemperatureCallback {
        void onApplyTemperature(Level world, BlockPos pos, int temperature);

    }
}
