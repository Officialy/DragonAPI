package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final Map<String, RenderType> RENDER_TYPES = new HashMap<>();

    public GlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, AbstractClientPlayer pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        String glow = PlayerSpecificRenderer.instance.getGlow(pLivingEntity.getUUID());
        if (glow != null) {
            RenderType renderType = getRenderType(glow);
            VertexConsumer vertexconsumer = pBuffer.getBuffer(renderType);
            this.getParentModel().renderToBuffer(pMatrixStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static RenderType getRenderType(String texture) {
        return RENDER_TYPES.computeIfAbsent(texture, t -> {
            ResourceLocation loc = new ResourceLocation(DragonAPI.MODID, "textures/entity/glow/" + t + ".png");
            return RenderType.entityTranslucentEmissive(loc);
        });
    }
}