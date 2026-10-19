package reika.dragonapi.instantiable.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderMatrixContextTest {
    @Test void viewMultipliesModelInNativeOrder() {
        var context=RenderMatrixContext.compose(new Matrix4f().rotationY((float)Math.PI/2),
                new Matrix4f().translation(2,3,4),new Matrix4f().scaling(2,3,4),new Matrix4f().translation(.25F,.5F,0));
        var position=context.modelView().transformPosition(new Vector3f());
        assertEquals(4,position.x,1e-5); assertEquals(3,position.y,1e-5); assertEquals(-2,position.z,1e-5);
        assertEquals(2,context.projection().m00()); assertEquals(3,context.projection().m11());
        assertEquals(.25F,context.texture().m30()); assertEquals(.5F,context.texture().m31());
    }
    @Test void deferredDrawSurvivesInputAndOutputMatrixReuse() {
        var view=new Matrix4f().translation(2,3,4);
        var projection=new Matrix4f().perspective(1.2F,1.6F,.05F,256);
        var texture=new Matrix4f().translation(.25F,.75F,0).scale(2);
        var expectedProjection=new Matrix4f(projection);
        var expectedTexture=new Matrix4f(texture);
        var context=RenderMatrixContext.capture(view,projection,texture);
        view.zero(); projection.zero(); texture.zero();
        context.modelView().zero(); context.projection().zero(); context.texture().zero();
        assertEquals(new Matrix4f().translation(2,3,4),context.modelView());
        assertEquals(expectedProjection,context.projection()); assertEquals(expectedTexture,context.texture());
    }
    @Test void contextsRetainDifferentPassesRatherThanSharedGlobalState() {
        var shared=new Matrix4f().translation(1,2,3);
        var first=RenderMatrixContext.capture(shared,new Matrix4f().scaling(2),new Matrix4f().translation(.1F,0,0));
        shared.translation(7,8,9);
        var second=RenderMatrixContext.capture(shared,new Matrix4f().scaling(3),new Matrix4f().translation(.2F,0,0));
        assertEquals(1,first.modelView().m30()); assertEquals(7,second.modelView().m30());
        assertEquals(2,first.projection().m00()); assertEquals(3,second.projection().m00());
        assertEquals(.1F,first.texture().m30()); assertEquals(.2F,second.texture().m30());
    }
    @Test void missingTransformsAreRejectedInsteadOfInventingIdentity() {
        assertThrows(NullPointerException.class,()->RenderMatrixContext.capture(null,new Matrix4f(),new Matrix4f()));
        assertThrows(NullPointerException.class,()->RenderMatrixContext.capture(new Matrix4f(),null,new Matrix4f()));
        assertThrows(NullPointerException.class,()->RenderMatrixContext.capture(new Matrix4f(),new Matrix4f(),null));
    }
}
