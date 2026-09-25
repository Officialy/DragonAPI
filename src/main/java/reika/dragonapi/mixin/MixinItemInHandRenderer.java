package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.instantiable.rendering.HeldMapRenderers;

/** Routes {@link HeldMapRenderers} items through vanilla's first-person map rendering. */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @WrapOperation(method = "submitArmWithItem", constant = @Constant(classValue = MapItem.class))
    private boolean dragonapi$heldMapIsMap(Object item, Operation<Boolean> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stack) {
        return original.call(item) || HeldMapRenderers.get(stack) != null;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void dragonapi$renderHeldMap(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            ItemStack stack, CallbackInfo ci) {
        HeldMapRenderers.HeldMapRenderer renderer = HeldMapRenderers.get(stack);
        if (renderer == null)
            return;
        // Vanilla renderMap's own transform into 128-unit map space.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        renderer.render(poseStack, collector, lightCoords, stack);
        ci.cancel();
    }

}
