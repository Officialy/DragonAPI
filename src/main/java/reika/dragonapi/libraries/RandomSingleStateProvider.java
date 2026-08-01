package reika.dragonapi.libraries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RandomSingleStateProvider extends BlockStateProvider {
    public static final MapCodec<RandomSingleStateProvider> CODEC = RecordCodecBuilder.mapCodec(p_161576_ -> p_161576_
            .group(Codec.list(BlockState.CODEC).fieldOf(("states")).forGetter(p_161592_ -> p_161592_.states)).apply(p_161576_, RandomSingleStateProvider::new)
    );

    protected final List<BlockState> states;

    public static DataResult<RandomSingleStateProvider> create(List<BlockState> p_161598_) {
        return p_161598_.isEmpty() ? DataResult.error(() -> "RandomSingleStateProvider with no states") : DataResult.success(new RandomSingleStateProvider(p_161598_));
    }

    public RandomSingleStateProvider(List<BlockState> p_161596_) {
        this.states = p_161596_;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderTypes.RANDOM_SINGLE_STATE_PROVIDER.get();
    }

    @Override
    public @NotNull BlockState getState(net.minecraft.world.level.WorldGenLevel level, RandomSource random, BlockPos pos) {
        if (this.states.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = this.states.get(random.nextInt(this.states.size()));
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }
}