package reika.dragonapi.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import reika.dragonapi.test.GameTestRuntimeMonitor;

@Mixin(GameTestServer.class)
public abstract class MixinGameTestServer {

    @Shadow
    private List<GameTestBatch> testBatches;

    @Shadow
    private MultipleTestTracker testTracker;

    @Inject(method = "evaluateTestsToRun", at = @At("RETURN"), cancellable = true)
    private void dragonapi$useSmallerBatches(CallbackInfoReturnable<List<GameTestBatch>> cir) {
        int batchSize = Math.max(1, Math.min(50, Integer.getInteger("dragonapi.gametest.batchSize", 12)));
        List<GameTestBatch> original = cir.getReturnValue();
        List<GameTestBatch> split = new ArrayList<>();
        // 26.3 batches are keyed by environment AND dimension (GameTestBatchFactory.BatchKey); number
        // the split batches per key the same way and keep each batch's dimension.
        Map<List<Object>, Integer> indices = new HashMap<>();
        for (GameTestBatch batch : original) {
            List<GameTestInfo> tests = List.copyOf(batch.gameTestInfos());
            List<Object> key = List.of(batch.environment(), batch.dimension());
            for (int start = 0; start < tests.size(); start += batchSize) {
                int end = Math.min(start + batchSize, tests.size());
                int index = indices.getOrDefault(key, 0);
                split.add(new GameTestBatch(index, List.copyOf(tests.subList(start, end)), batch.environment(), batch.dimension()));
                indices.put(key, index + 1);
            }
        }
        cir.setReturnValue(List.copyOf(split));
    }

    @Inject(method = "initServer", at = @At("RETURN"))
    private void dragonapi$startGameTestWatchdog(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            int total = this.testBatches.stream().mapToInt(batch -> batch.gameTestInfos().size()).sum();
            GameTestRuntimeMonitor.start((GameTestServer)(Object)this, total, this.testBatches.size());
        }
    }

    @Redirect(
            method = "startTests",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/gametest/framework/GameTestRunner$Builder;build()Lnet/minecraft/gametest/framework/GameTestRunner;"
            )
    )
    private GameTestRunner dragonapi$clearCompletedBatchStructures(GameTestRunner.Builder builder) {
        return builder.clearBetweenBatches().build();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void dragonapi$gameTestTickStarted(CallbackInfo ci) {
        GameTestRuntimeMonitor.tickStarted((GameTestServer)(Object)this, this.testTracker);
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void dragonapi$gameTestTickCompleted(CallbackInfo ci) {
        GameTestRuntimeMonitor.tickCompleted((GameTestServer)(Object)this, this.testTracker);
        // Once the tracker is complete the server leaves the tick loop and synchronously saves the
        // test level. That is expected shutdown work, not a hung game tick; disarm the hard-stop
        // watchdog before saving so a very large suite can never be killed mid-save.
        if (this.testTracker != null && this.testTracker.isDone()) {
            GameTestRuntimeMonitor.stop();
        }
    }

    @Inject(method = "onServerExit", at = @At("HEAD"))
    private void dragonapi$stopGameTestWatchdog(CallbackInfo ci) {
        GameTestRuntimeMonitor.stop();
    }
}
