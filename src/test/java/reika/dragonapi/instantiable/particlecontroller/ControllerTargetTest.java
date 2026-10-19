package reika.dragonapi.instantiable.particlecontroller;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import reika.dragonapi.interfaces.ControllerTarget;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTargetTest {
    @Test void targetAccelerationUsesRealVelocityRatherThanCardinalDirection() {
        var target=new ControllerTarget(2,3,4,new Vec3(0.125,-0.25,0.375),7);
        var controller=new TargetMotionController(4,7,10,0.5);
        assertEquals(1.125,controller.getMotionXFor(target));
        assertEquals(1.75,controller.getMotionYFor(target));
        assertEquals(3.375,controller.getMotionZFor(target));
    }
    @Test void sharedSpiralRetainsVelocityAndUsesTargetHeight() {
        var target=new ControllerTarget(2,3,4,new Vec3(0.125,-0.25,0.375),7);
        var controller=new SpiralMotionController(10,20,90,0.75,2,0);
        controller.updateTarget(target);
        assertEquals(10,controller.getPositionXFor(target),1e-10);
        assertEquals(22,controller.getPositionZFor(target),1e-10);
        assertEquals(3,controller.getPositionYFor(target));
        assertEquals(0.125,controller.getMotionXFor(target));
        assertEquals(0.375,controller.getMotionZFor(target));
        assertEquals(0.75,controller.getMotionYFor(target));
    }
    @Test void attractionNormalizesByDistanceRatherThanDistanceSquared() {
        var target=new ControllerTarget(3,0,4,Vec3.ZERO,0);
        var controller=new AttractiveMotionController(0,0,0,1,0,1);
        controller.updateTarget(target);
        assertEquals(-0.6,controller.getMotionXFor(target),1e-12);
        assertEquals(-0.8,controller.getMotionZFor(target),1e-12);
    }
    @Test void oldNullTargetCallsRemainUsableForStateOnlyPaths() {
        var controller=new CollectingPositionController(0,1,2,10,11,12,10);
        controller.update(null);
        assertEquals(1,controller.getPositionX(null));
        assertEquals(2,controller.getPositionY(null));
        assertEquals(3,controller.getPositionZ(null));
    }
}
