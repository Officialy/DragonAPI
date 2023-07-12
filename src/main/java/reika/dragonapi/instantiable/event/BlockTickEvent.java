package reika.dragonapi.instantiable.event;

//@Cancelable do not add annotation, since that ends up triggering forge ASM

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.instantiable.event.base.WorldPositionEvent;
import reika.dragonapi.libraries.level.ReikaBlockHelper;

import java.util.Random;

/**
 * This method is fired whenever blocks are ticked via updateTick() via ambient world block ticks and any of my mods' code running forced ticks.
 * The event may or may not be cancelable (check isCancelable() first), in which case the tick will not occur.
 */
public class BlockTickEvent extends WorldPositionEvent {

    public static boolean disallowAllUpdates = false;

    public final BlockState block;

    private final int flags;

    public BlockTickEvent(Level world, BlockPos pos, BlockState b, int flags) {
        super(world, pos);
        block = b;
        this.flags = flags;
    }

    @Override
    public boolean isCancelable() {
        return !UpdateFlags.REQUIRE.isFlagPresent(flags);
    }

    public boolean isFlagPresent(UpdateFlags flag) {
        return flag.isFlagPresent(flags);
    }

    public static void fire(ServerLevel world, BlockPos pos, int flags) {
        fire(world.getBlockState(pos), world, pos, world.random, flags);
    }

    public static void fire(ServerLevel world, BlockPos pos, UpdateFlags flag) {
        fire(world.getBlockState(pos), world, pos, flag);
    }

    public static void fire(BlockState b, ServerLevel world, BlockPos pos, UpdateFlags flag) {
        fire(b, world, pos, world.random, flag);
    }

    public static void fire(ServerLevel world, BlockPos pos, RandomSource rand, int flags) {
        fire(world.getBlockState(pos), world, pos, rand, flags);
    }

    public static void fire(ServerLevel world, BlockPos pos, RandomSource rand, UpdateFlags flag) {
        fire(world.getBlockState(pos), world, pos, rand, flag);
    }

    public static void fire(BlockState b, ServerLevel world, BlockPos pos, RandomSource rand, UpdateFlags flag) {
        fire(b, world, pos, rand, flag.flag);
    }

    public static void fire(BlockState b, ServerLevel world, BlockPos pos, RandomSource rand, int flags) {
        if (!disallowAllUpdates && canTickAt(b, world, pos, flags) && !MinecraftForge.EVENT_BUS.post(new BlockTickEvent(world, pos, b, flags))) {
            b.tick(world, pos, rand);
        }
    }

    private static boolean canTickAt(BlockState b, Level world, BlockPos pos, int flags) {
        if (UpdateFlags.FORCED.isFlagPresent(flags) || UpdateFlags.REQUIRE.isFlagPresent(flags))
            return true;
        return !DragonOptions.STOPUNLOADSPREAD.getState() || !requireLoadedArea(b) || hasLoadedRadius(world, pos);
    }

    private static boolean requireLoadedArea(BlockState b) {
        return ReikaBlockHelper.isLiquid(b) || b.getBlock() instanceof GrassBlock || b.getBlock() instanceof FireBlock;
    }

    private static boolean hasLoadedRadius(Level world, BlockPos pos) {
        int cx = (pos.getX() >> 4) << 4;
        int cz = (pos.getZ() >> 4) << 4;
        return world.hasChunksAt(cx - 16, pos.getY(), cz - 16, cx + 16, pos.getY(), cz + 16);
    }

    public enum UpdateFlags {
        NATURAL(),
        SCHEDULED(),
        FORCED(),
        REQUIRE();

        public final int flag;

        UpdateFlags() {
            flag = 1 << this.ordinal();
        }

        public boolean isFlagPresent(int flags) {
            return (flags & flag) != 0;
        }

        public static int getForcedUnstoppableTick() {
            return FORCED.flag + REQUIRE.flag;
        }
    }
}
