package reika.dragonapi.interfaces.entity;

import reika.dragonapi.instantiable.rendering.ReikaParticleEngine;

public interface CustomRenderFX {

    ReikaParticleEngine.RenderMode getRenderMode();

    ReikaParticleEngine.TextureMode getTexture();

    boolean rendersOverLimit();

    double getRenderRange();
}
