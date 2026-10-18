package reika.dragonapi.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.instantiable.event.client.BossColorEvent;

@Mixin(GameRenderer.class)
public abstract class MixinBossColorReset {
    @Inject(method = "tick", at = @At("RETURN"))
    private void dragonapi$bossColorReset(CallbackInfo callback) {
        if (Minecraft.getInstance().level != null) BossColorEvent.reset();
    }
}
