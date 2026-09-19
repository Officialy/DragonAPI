package reika.dragonapi.mixin;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.test.GameTestRuntimeMonitor;

@Mixin(GameTestRunner.class)
public abstract class MixinGameTestRunner {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    private ImmutableList<GameTestBatch> batches;

    @Inject(method = "runBatch", at = @At("HEAD"))
    private void dragonapi$beforeBatch(int batchIndex, CallbackInfo ci) {
        int removed = batchIndex > 0 ? this.dragonapi$removeSyntheticPlayers() : 0;
        int testCount = batchIndex < this.batches.size() ? this.batches.get(batchIndex).gameTestInfos().size() : 0;
        GameTestRuntimeMonitor.preparingBatch(batchIndex, this.batches.size(), testCount, removed);
    }

    @Inject(method = "runBatch", at = @At("RETURN"))
    private void dragonapi$afterBatchPreparation(int batchIndex, CallbackInfo ci) {
        GameTestRuntimeMonitor.runningBatch(batchIndex, this.batches.size());
    }

    private int dragonapi$removeSyntheticPlayers() {
        List<ServerPlayer> players = List.copyOf(this.level.getServer().getPlayerList().getPlayers());
        int removed = 0;
        for (ServerPlayer player : players) {
            if (GameTestRuntimeMonitor.isSyntheticPlayer(player)) {
                this.level.getServer().getPlayerList().remove(player);
                removed++;
            }
        }
        return removed;
    }
}
