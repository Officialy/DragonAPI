package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reika.dragonapi.auxiliary.trackers.ProfilingEventBus;

/** Construct the native game-bus subtype before the global bus reference is published. */
@Mixin(value = NeoForge.class, remap = false)
public abstract class MixinNeoForgeEventBus {
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/BusBuilder;build()Lnet/neoforged/bus/api/IEventBus;"), remap = false)
    private static IEventBus dragonapi$profileableBus(BusBuilder builder, Operation<IEventBus> original) {
        return ProfilingEventBus.create(builder);
    }
}
