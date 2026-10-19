package reika.dragonapi.instantiable.rendering;

import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.rendering.ReikaParticleEngine.RenderMode;
import reika.dragonapi.instantiable.rendering.ReikaParticleEngine.RenderModeFlags;
import static org.junit.jupiter.api.Assertions.*;

class ParticleRenderModeTest {
    @Test void modeSnapshotsStayStableAfterTheBuilderChanges() {
        var mode=new RenderMode(); int captured=mode.snapshot();
        assertTrue(mode.getFlag(RenderModeFlags.FOG));
        assertFalse(mode.getFlag(RenderModeFlags.ADDITIVE));
        mode.setFlag(RenderModeFlags.FOG,false).setFlag(RenderModeFlags.ADDITIVE,true);
        assertNotEquals(captured,mode.snapshot());
        assertEquals(new RenderMode().snapshot(),captured);
        assertTrue((captured&RenderModeFlags.FOG.mask())!=0);
    }
}
