package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import reika.dragonapi.instantiable.event.BlockTransformResultEvent;

/**
 * Posts {@link BlockTransformResultEvent} for the state an item's block transformer picked, so the
 * tool-modification hook NeoForge's {@code BlockToolModificationEvent} gave 26.2 tilling, stripping and
 * flattening survives their move onto 26.3's data-driven transformers.
 */
@Mixin(BlockTransformer.class)
public abstract class MixinBlockTransformer {

	@WrapOperation(method = "transformBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;getOptionalState(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private @Nullable BlockState dragonapi$fireTransformResult(BlockStateProvider provider, LevelAccessor level,
			RandomSource random, BlockPos pos, Operation<BlockState> original,
			@Local(argsOnly = true) UseOnContext context) {
		BlockState chosen = original.call(provider, level, random, pos);
		return chosen == null ? null : BlockTransformResultEvent.fire(context, chosen);
	}
}
