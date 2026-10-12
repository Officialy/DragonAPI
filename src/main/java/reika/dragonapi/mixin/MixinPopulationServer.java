package reika.dragonapi.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.auxiliary.trackers.BoundedPopulation;

/** Dedicated-server empty-player pausing must not interrupt an authorized pregeneration pass. */
@Mixin(MinecraftServer.class)
public abstract class MixinPopulationServer {
    @Shadow private int emptyTicks;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void dragonapi$keepPopulationRunning(BooleanSupplier haveTime, CallbackInfo ci) {
        if (BoundedPopulation.keepsServerActive((MinecraftServer) (Object) this)) emptyTicks = 0;
    }
}
