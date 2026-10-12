package reika.dragonapi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.command.generation.GenerationTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.auxiliary.trackers.BoundedPopulation;
import reika.dragonapi.instantiable.worldgen.PopulationScope;

/** Hooks the actual 26.3 task, including starts from code and /execute in another dimension. */
@Mixin(value = GenerationTask.class, remap = false)
public abstract class MixinGenerationTask {
    @Shadow @Final private ServerLevel serverLevel;
    @Shadow @Final private int x;
    @Shadow @Final private int z;
    @Shadow @Final private int radius;
    @Unique private long dragonapi$population;

    @ModifyVariable(method = "run", at = @At("HEAD"), argsOnly = true)
    private GenerationTask.Listener dragonapi$populationListener(GenerationTask.Listener listener) {
        dragonapi$population = BoundedPopulation.get(serverLevel).begin("neoforge", x + "/" + z + "/" + radius,
                PopulationScope.rectangle(x - radius, z - radius, x + radius, z + radius), true);
        long id = dragonapi$population;
        GenerationTask task = (GenerationTask) (Object) this;
        return new GenerationTask.Listener() {
            @Override public void update(int ok, int error, int skipped, int total) {
                if ((long) ok + error + skipped >= total)
                    serverLevel.getServer().execute(() -> BoundedPopulation.get(serverLevel).terrainFinished(id));
                listener.update(ok, error, skipped, total);
            }
            @Override public void complete(int error) {
                // NeoForge can call complete while its final batch is still completing.
                if ((long) task.getOkCount() + task.getErrorCount() + task.getSkippedCount() >= task.getTotalCount())
                    serverLevel.getServer().execute(() -> BoundedPopulation.get(serverLevel).terrainFinished(id));
                listener.complete(error);
            }
        };
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void dragonapi$pausePopulation(CallbackInfo ci) {
        long id = dragonapi$population;
        if (id != 0) serverLevel.getServer().execute(() -> {
            var population = BoundedPopulation.get(serverLevel);
            population.terrainFinished(id); population.pause(id);
        });
    }
}
