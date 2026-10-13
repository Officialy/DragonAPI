package reika.dragonapi.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import reika.dragonapi.auxiliary.trackers.PlayerChunkTracker;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
    @Inject(method = "skipPlayer", at = @At("HEAD"), cancellable = true)
    private void dragonapi$suspendPlayerTickets(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (PlayerChunkTracker.shouldStopChunkloadingFor(player)) callback.setReturnValue(true);
    }
    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void onUpdateChunkTracking(ServerPlayer player, CallbackInfo ci) {
        if (PlayerChunkTracker.shouldStopChunkloadingFor(player)) {
            ci.cancel();
        }
    }
}
