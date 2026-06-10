package reika.dragonapi.instantiable.effects;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class StringParticleFX extends Particle {

    public StringParticleFX(ClientLevel world, double x, double y, double z, String s, double xd, double yd, double zd) {
        super(world, x, y, z);
    }

    public void setScale(float f) {
    }

    public void setLife(int f) {
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }

}
