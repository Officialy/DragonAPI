package reika.dragonapi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import reika.dragonapi.instantiable.rendering.TessellatorVertexList;
import reika.dragonapi.instantiable.rendering.TessellatorVertexList.VertexData;

/** Client-only buffer emission. This helper neither starts nor closes the caller's buffer or render pass. */
public final class TessellatorVertexRenderer {
    private TessellatorVertexRenderer() {}

    /** Initial/current draw state, replacing the old global tessellator's sticky attributes. Color is native ARGB. */
    public record Attributes(int color, float u, float v, int light, int overlay, float normalX, float normalY, float normalZ) {}

    public static Attributes render(TessellatorVertexList vertices, PoseStack.Pose pose, VertexConsumer buffer, Attributes initial) {
        return render(vertices.snapshot(), pose, buffer, initial);
    }

    /** Returns the final inherited attributes, allowing several lists to form one explicit draw sequence. */
    public static Attributes render(List<VertexData> vertices, PoseStack.Pose pose, VertexConsumer buffer, Attributes initial) {
        Objects.requireNonNull(vertices, "vertices"); Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(buffer, "buffer"); Objects.requireNonNull(initial, "initial attributes");
        int color = initial.color;
        float u = initial.u, v = initial.v;
        for (VertexData vertex : vertices) {
            // 1.7.10 setColorOpaque_I ignored the high byte; subsequent uncolored vertices inherited this opaque RGB.
            if (vertex.hasColor()) color = 0xFF000000 | vertex.color();
            if (vertex.hasUV()) { u = (float)vertex.u(); v = (float)vertex.v(); }
            buffer.addVertex(pose, (float)vertex.x(), (float)vertex.y(), (float)vertex.z())
                    .setColor(color).setUv(u, v).setLight(initial.light).setOverlay(initial.overlay)
                    .setNormal(pose, initial.normalX, initial.normalY, initial.normalZ);
        }
        return new Attributes(color, u, v, initial.light, initial.overlay, initial.normalX, initial.normalY, initial.normalZ);
    }

    /** Capture both geometry and transform now. The deferred callback never retains the mutable list/pose or a world. */
    public static void submit(TessellatorVertexList vertices, PoseStack pose, SubmitNodeCollector collector, RenderType type, Attributes attributes) {
        var captured = vertices.snapshot();
        Objects.requireNonNull(collector, "collector"); Objects.requireNonNull(type, "render type");
        Objects.requireNonNull(attributes, "attributes");
        if (captured.isEmpty()) return;
        var transform = new PoseStack();
        transform.last().set(pose.last());
        collector.submitCustomGeometry(transform, type, (submittedPose, buffer) -> render(captured, submittedPose, buffer, attributes));
    }
}
