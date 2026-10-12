package reika.dragonapi.mixin;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spawn chunks arrive before Chunky's ServerStartingEvent initializes its provider. */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor", remap = false)
public abstract class MixinDistantHorizonsChunkyAccessor {
    @Shadow private boolean listenerBound;
    @Unique private static Method dragonapi$providerGetter;
    @Unique private WeakReference<Object> dragonapi$boundProvider = new WeakReference<>(null);

    @Inject(method = "tryRunFirstTimeSetup()V", at = @At("HEAD"), cancellable = true)
    private void dragonapi$waitForChunky(CallbackInfo ci) {
        Object provider;
        try {
            if (dragonapi$providerGetter == null)
                dragonapi$providerGetter = Class.forName("org.popcraft.chunky.ChunkyProvider", false,
                        this.getClass().getClassLoader()).getMethod("get");
            provider = dragonapi$providerGetter.invoke(null);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalStateException && "Chunky is not loaded.".equals(cause.getMessage())) {
                // Cancel before DH marks the listener bound; the next chunk update retries normally.
                listenerBound = false;
                dragonapi$boundProvider.clear();
                ci.cancel();
                return;
            }
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Could not resolve Chunky for Distant Horizons", cause);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Incompatible Chunky provider API for Distant Horizons", ex);
        }
        if (dragonapi$boundProvider.get() != provider) {
            // The same DH accessor survives integrated-world changes; each Chunky instance has its own event bus.
            listenerBound = false;
            dragonapi$boundProvider = new WeakReference<>(provider);
        }
    }
}
