package reika.dragonapi.libraries.level;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldgenHeightTest {
    @Test void modernUndergroundKeepsDepthOrderAndSurface() {
        var world = LevelHeightAccessor.create(-64, 384);
        assertEquals(-60, WorldgenHeight.caveFloor(world));
        assertEquals(-44, WorldgenHeight.undergroundY(world, 10));
        assertEquals(0, WorldgenHeight.undergroundY(world, 32));
        assertEquals(64, WorldgenHeight.undergroundY(world, 64));
        var nether = LevelHeightAccessor.create(0, 256);
        assertEquals(32, WorldgenHeight.undergroundY(nether, 32));
    }

    @Test void samplingClipsBothBoundsAndIncludesModernTop() {
        var shortWorld = LevelHeightAccessor.create(-16, 32);
        var random = RandomSource.create(8912);
        int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
        for (int i = 0; i < 2048; i++) {
            int y = WorldgenHeight.randomBetween(shortWorld, random, -60, 319);
            assertTrue(shortWorld.isInsideBuildHeight(y));
            minimum = Math.min(minimum, y);
            maximum = Math.max(maximum, y);
        }
        assertEquals(-16, minimum);
        assertEquals(15, maximum); // getMaxY is inclusive in 26.3.
        assertEquals(319, WorldgenHeight.randomBetween(LevelHeightAccessor.create(-64, 384), random, 319, 319));
    }
}
