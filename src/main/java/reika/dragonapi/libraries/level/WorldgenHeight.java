package reika.dragonapi.libraries.level;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;

/** Adapts the old 0..64 underground interval to the dimension's floor..64 interval. */
public final class WorldgenHeight {
    private WorldgenHeight() {}

    public static int caveFloor(LevelHeightAccessor world) {
        return Math.min(world.getMinY() + 4, world.getMaxY());
    }

    public static int undergroundY(LevelHeightAccessor world, int legacyY) {
        int ceiling = Math.min(64, world.getMaxY());
        int y = world.getMinY() + Math.round(legacyY * (ceiling - world.getMinY()) / 64F);
        return Mth.clamp(y, caveFloor(world), world.getMaxY());
    }

    /** Inclusive bounds, clipped before rolling so short dimensions do not waste ore attempts. */
    public static int randomBetween(LevelHeightAccessor world, RandomSource random, int minimum, int maximum) {
        int low = Mth.clamp(minimum, world.getMinY(), world.getMaxY());
        int high = Mth.clamp(maximum, low, world.getMaxY());
        return low + random.nextInt(high - low + 1);
    }
}
