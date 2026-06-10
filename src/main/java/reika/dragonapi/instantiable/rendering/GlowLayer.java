package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.PlayerSpecificRenderer;

import java.util.HashMap;
import java.util.Map;

public class GlowLayer extends RenderLayer<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> {

    private static final Map<String, RenderType> RENDER_TYPES = new HashMap<>();

    public GlowLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void submit(PoseStack pMatrixStack, SubmitNodeCollector pBuffer, int pPackedLight, net.minecraft.client.renderer.entity.state.AvatarRenderState pLivingEntity, float pNetHeadYaw, float pHeadPitch) {
        // TODO 1.21+: Port to SubmitNodeCollector
    }

    private static RenderType getRenderType(String texture) {
        return RENDER_TYPES.computeIfAbsent(texture, t -> {
            Identifier loc = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "textures/entity/glow/" + t + ".png");
            return RenderTypes.entityTranslucentEmissive(loc);
        });
    }
}
