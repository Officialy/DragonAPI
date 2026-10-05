package reika.dragonapi.libraries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.StreamSupport;

public class RandomTagSingleStateProvider implements BlockStateProvider {
    // The codec now only needs to know about the tag, which is the sole source of its state.
    public static final MapCodec<RandomTagSingleStateProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(p -> p.tag))
            .apply(instance, RandomTagSingleStateProvider::new)
    );

    private final TagKey<Block> tag;
    // Cache the list of states for performance. It's populated lazily on first use.
    private List<BlockState> states;

    public RandomTagSingleStateProvider(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override
    public @NotNull MapCodec<RandomTagSingleStateProvider> codec() {
        return CODEC;
    }

    /**
     * Lazily initializes the list of states from the tag.
     * This method is synchronized to prevent race conditions during world generation.
     */
    private void initializeStates() {
        // Use double-checked locking for thread-safe lazy initialization.
        if (this.states == null) {
            synchronized (this) {
                if (this.states == null) {
                    // Use getTagOrEmpty which is the modern, safe way to access tag contents.
                    Iterable<Holder<Block>> holders = BuiltInRegistries.BLOCK.getTagOrEmpty(this.tag);

                    // Convert the iterable of holders to a list of default block states.
                    List<BlockState> resolvedStates = StreamSupport.stream(holders.spliterator(), false)
                            .map(Holder::value)
                            .map(Block::defaultBlockState)
                            .toList();

                    // If the tag was empty or didn't exist, fall back to AIR to prevent crashes.
                    if (resolvedStates.isEmpty()) {
                        this.states = List.of(Blocks.AIR.defaultBlockState());
                    } else {
                        this.states = resolvedStates;
                    }
                }
            }
        }
    }

    @Override
    public @NotNull BlockState getState(LevelAccessor level, @NotNull RandomSource random, @NotNull BlockPos pos) {
        // TreeFeature calls a trunk provider once for every log. Consuming its RandomSource here
        // chose a different species for every Y coordinate. A "single state" provider instead
        // keys the choice to the world and trunk column, so one straight tree has one wood while
        // separate trees still vary and the result remains deterministic across chunk retries.
        initializeStates();
        // 26.3 passes a LevelAccessor; worldgen always supplies a WorldGenLevel (as does ServerLevel).
        long seed = level instanceof WorldGenLevel world ? world.getSeed() : 0L;
        seed ^= (long)pos.getX() * 341873128712L;
        seed ^= (long)pos.getZ() * 132897987541L;
        seed ^= seed >>> 29;
        return this.states.get(Math.floorMod(seed, this.states.size()));
    }
}
