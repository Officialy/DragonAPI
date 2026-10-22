package reika.dragonapi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;

/** Source pass-0 blending with depth writes belongs before world transparency, in the native solid phase. */
public final class SolidModelRendering {
    public static final RenderPhaseKey<SubmitNode> PHASE=new RenderPhaseKey<>(collection->collection.solid);
    private SolidModelRendering() {}
    public static void submitGeometry(SubmitNodeCollector collector,PoseStack pose,int order,RenderType type,
            SubmitNodeCollector.CustomGeometryRenderer geometry) {
        collector.order(order).submitSpecial(PHASE,new CustomFeatureRenderer.Submit(pose.last().copy(),type,geometry));
    }
    public static void submitGeometry(SubmitNodeCollector collector,PoseStack pose,RenderType type,
            SubmitNodeCollector.CustomGeometryRenderer geometry) {
        collector.submitSpecial(PHASE,new CustomFeatureRenderer.Submit(pose.last().copy(),type,geometry));
    }
}
