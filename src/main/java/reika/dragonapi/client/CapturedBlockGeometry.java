package reika.dragonapi.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;

/** Owned recording consumer for the actual 26.3 BLOCK format (position, ARGB, UV0, packed light). */
public final class CapturedBlockGeometry implements VertexConsumer {
    public record Vertex(float x, float y, float z, int color, float u, float v, int light) {
        public void emit(VertexConsumer output) { output.addVertex(x,y,z).setColor(color).setUv(u,v).setLight(light); }
    }
    private final List<Vertex> vertices = new ArrayList<>();
    private float x,y,z,u,v;
    private int color,light,mask;
    private boolean started,finished;

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        if (finished) throw new IllegalStateException("Geometry capture is finished");
        endVertex(); this.x=x; this.y=y; this.z=z; mask=0; started=true; return this;
    }
    private void endVertex() {
        if (!started) return;
        if (mask != 7) throw new IllegalStateException("Terrain vertex requires color, UV and light (written mask=" + mask + ")");
        vertices.add(new Vertex(x,y,z,color,u,v,light)); started=false;
    }
    private boolean write(int bit) {
        if (finished || !started) throw new IllegalStateException("No active terrain vertex");
        if ((mask & bit) != 0) return false;
        mask |= bit; return true;
    }
    @Override public VertexConsumer setColor(int color) { if (write(1)) this.color=color; return this; }
    @Override public VertexConsumer setColor(int r,int g,int b,int a) { return setColor((a&255)<<24|(r&255)<<16|(g&255)<<8|(b&255)); }
    @Override public VertexConsumer setUv(float u,float v) { if (write(2)) { this.u=u; this.v=v; } return this; }
    @Override public VertexConsumer setUv2(int u,int v) { if (write(4)) light=(u&65535)|(v&65535)<<16; return this; }
    // These attributes are absent from native BLOCK buffers too; they cannot affect terrain vertices.
    @Override public VertexConsumer setUv1(int u,int v) { return this; }
    @Override public VertexConsumer setUv3(float u,float v) { return this; }
    @Override public VertexConsumer setNormal(float x,float y,float z) { return this; }
    @Override public VertexConsumer setLineWidth(float width) { return this; }

    public List<Vertex> finish() {
        if (finished) throw new IllegalStateException("Geometry capture is finished");
        endVertex();
        if (vertices.size()%4 != 0) throw new IllegalStateException("Terrain geometry must contain complete quads");
        finished=true; return List.copyOf(vertices);
    }
}
