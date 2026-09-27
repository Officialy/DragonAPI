package reika.dragonapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import reika.dragonapi.instantiable.rendering.HeldMapRenderers;

/**
 * Routes {@link HeldMapRenderers} items through vanilla's first-person map rendering.
 * 26.3 replaced {@code ItemInHandRenderer} with {@link FirstPersonHandsAndItemsRenderer} and
 * detects a held map by its {@code MAP_ID} component rather than {@code instanceof MapItem}.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class MixinItemInHandRenderer {

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean dragonapi$heldMapIsMap(ItemStack stack, DataComponentType<?> type, Operation<Boolean> original) {
        return original.call(stack, type) || type == DataComponents.MAP_ID && HeldMapRenderers.get(stack) != null;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void dragonapi$renderHeldMap(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            ItemStack stack, boolean mainHand, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        HeldMapRenderers.HeldMapRenderer renderer = HeldMapRenderers.get(stack);
        if (renderer == null)
            return;
        // Vanilla renderMap's own transform into 128-unit map space.
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
        poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        renderer.render(poseStack, collector, lightCoords, stack);
        ci.cancel();
    }

}
