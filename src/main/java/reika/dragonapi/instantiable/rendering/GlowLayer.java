package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;

import java.util.HashMap;
import java.util.Map;

public class GlowLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public GlowLayer(RenderLayerParent<AvatarRenderState, PlayerModel> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void submit(PoseStack pMatrixStack, SubmitNodeCollector pBuffer, int pPackedLight, AvatarRenderState pLivingEntity, float pNetHeadYaw, float pHeadPitch) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || pLivingEntity.isInvisible || pLivingEntity.isSpectator) return;
        var entity = level.getEntity(pLivingEntity.id);
        if (entity == null) return;
        Identifier texture = PlayerSpecificRenderer.instance.getGlowTexture(entity.getUUID());
        if (texture == null) return;
        pBuffer.submitModel(getParentModel(), pLivingEntity, pMatrixStack,
                RenderTypes.entityTranslucentEmissive(texture), 15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
    }

}
