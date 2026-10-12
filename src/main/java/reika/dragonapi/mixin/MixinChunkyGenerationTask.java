package reika.dragonapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.modinterface.ChunkyPopulation;

/** Observe real task processing, including already-FULL chunks skipped by the pregenerator. */
@Pseudo
@Mixin(targets = "org.popcraft.chunky.GenerationTask", remap = false)
public abstract class MixinChunkyGenerationTask {
    @Unique private ChunkyPopulation.Handle dragonapi$population;

    @Inject(method = "run()V", at = @At("HEAD"))
    private void dragonapi$begin(CallbackInfo ci) { dragonapi$population = ChunkyPopulation.open(this); }

    @Inject(method = "update(IIZ)V", at = @At("HEAD"))
    private void dragonapi$observe(int x, int z, boolean loaded, CallbackInfo ci) {
        if (dragonapi$population != null) dragonapi$population.observe(x, z);
    }

    @Inject(method = "update(IIZ)V", at = @At("RETURN"))
    private void dragonapi$completedChunk(int x, int z, boolean loaded, CallbackInfo ci) {
        ChunkyPopulation.complete(this, dragonapi$population);
    }

    @Inject(method = "run()V", at = @At("RETURN"))
    private void dragonapi$complete(CallbackInfo ci) { ChunkyPopulation.complete(this, dragonapi$population); }

    @Inject(method = "stop(Z)V", at = @At("HEAD"))
    private void dragonapi$stop(boolean cancelled, CallbackInfo ci) {
        if (dragonapi$population != null) dragonapi$population.close(false, cancelled);
    }
}
