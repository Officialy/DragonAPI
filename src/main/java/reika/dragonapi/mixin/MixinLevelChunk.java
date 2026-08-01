package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.dragonapi.instantiable.event.SetBlockEvent;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void dragonapi$beforeSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
        LevelChunk chunk = (LevelChunk)(Object)this;
        SetBlockEvent.Pre.fire(chunk, pos.getX() & 15, pos.getY(), pos.getZ() & 15, state.getBlock());
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void dragonapi$afterSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
        if (callback.getReturnValue() != null) {
            LevelChunk chunk = (LevelChunk)(Object)this;
            SetBlockEvent.Post.fire(chunk, pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        }
    }
}