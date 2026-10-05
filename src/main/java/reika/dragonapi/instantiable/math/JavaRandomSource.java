package reika.dragonapi.instantiable.math;

import java.util.Random;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Java's exact random draws exposed to both original helpers and the modern feature API. */
public final class JavaRandomSource extends Random implements RandomSource {
    public JavaRandomSource(long seed) { super(seed); }
    @Override public RandomSource fork() { return new JavaRandomSource(nextLong()); }
    @Override public PositionalRandomFactory forkPositional() { return new LegacyRandomSource.LegacyPositionalRandomFactory(nextLong()); }
    @Override public int nextInt(int origin,int bound) { return RandomSource.super.nextInt(origin,bound); }
}
