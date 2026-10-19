package reika.dragonapi.instantiable.rendering;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Matrices captured for one draw or deferred submission. There is no implicit
 * global matrix state: the caller supplies the transforms belonging to its pass.
 * Inputs and returned matrices are copied, so later stack/frame reuse is safe.
 * This value has no client, GPU, or render-thread dependency.
 */
public final class RenderMatrixContext {
    private final Matrix4f modelView;
    private final Matrix4f projection;
    private final Matrix4f texture;

    private RenderMatrixContext(Matrix4fc modelView,Matrix4fc projection,Matrix4fc texture) {
        this.modelView=new Matrix4f(Objects.requireNonNull(modelView,"modelView"));
        this.projection=new Matrix4f(Objects.requireNonNull(projection,"projection"));
        this.texture=new Matrix4f(Objects.requireNonNull(texture,"texture"));
    }
    /** Capture an already-composed model-view matrix and the pass's projection/texture transforms. */
    public static RenderMatrixContext capture(Matrix4fc modelView,Matrix4fc projection,Matrix4fc texture) {
        return new RenderMatrixContext(modelView,projection,texture);
    }
    /** Compose view * model. Model coordinates must already use the caller's chosen camera origin. */
    public static RenderMatrixContext compose(Matrix4fc view,Matrix4fc model,Matrix4fc projection,Matrix4fc texture) {
        var modelView=new Matrix4f(Objects.requireNonNull(view,"view")).mul(Objects.requireNonNull(model,"model"));
        return new RenderMatrixContext(modelView,projection,texture);
    }
    public Matrix4f modelView() { return new Matrix4f(modelView); }
    public Matrix4f projection() { return new Matrix4f(projection); }
    public Matrix4f texture() { return new Matrix4f(texture); }
}
