package reika.dragonapi.libraries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class RandomTagSingleStateProvider extends BlockStateProvider {
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
    protected @NotNull BlockStateProviderType<?> type() {
        return BlockStateProviderTypes.RANDOM_TAG_SINGLE_STATE_PROVIDER.get();
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
    public @NotNull BlockState getState(net.minecraft.world.level.WorldGenLevel level, @NotNull RandomSource random, @NotNull BlockPos pos) {
        // Ensure the states list is initialized before use.
        initializeStates();

        // Return a random state from our cached list.
        return this.states.get(random.nextInt(this.states.size()));
    }
}