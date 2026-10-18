package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.client.BossLightmapRenderer;
import reika.dragonapi.instantiable.event.client.BossColorEvent;

@Mixin(Lightmap.class)
public abstract class MixinBossLightmap {
    @Unique private BossLightmapRenderer dragonapi$bossRenderer;
    @Shadow public abstract GpuTextureView getTextureView();

    @WrapMethod(method = "render")
    private void dragonapi$bossLightmap(LightmapRenderState state, Operation<Void> original) {
        if (state.needsUpdate && state.bossOverlayWorldDarkening > 0) {
            var color = BossColorEvent.fire(true);
            if (!color.equals(BossColorEvent.DEFAULT)) {
                if (dragonapi$bossRenderer == null) dragonapi$bossRenderer = new BossLightmapRenderer();
                dragonapi$bossRenderer.render(state, getTextureView(), color);
                return;
            }
        }
        original.call(state);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void dragonapi$closeBossRenderer(CallbackInfo callback) {
        if (dragonapi$bossRenderer != null) {
            dragonapi$bossRenderer.close();
            dragonapi$bossRenderer = null;
        }
    }
}
