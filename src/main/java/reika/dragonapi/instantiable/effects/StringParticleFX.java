package reika.dragonapi.instantiable.effects;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;

public class StringParticleFX extends Particle {

    public static final ParticleRenderType GROUP = new ParticleRenderType("DRAGONAPI_TEXT", "DT");
    public final String text;
    private float textScale = 1;

    public StringParticleFX(ClientLevel world, double x, double y, double z, String s, double xd, double yd, double zd) {
        super(world, x, y, z);
        text = java.util.Objects.requireNonNull(s);
        setParticleSpeed(xd, yd, zd);
        lifetime = 20;
        hasPhysics = false;
    }

    public void setScale(float f) {
        if (!Float.isFinite(f) || f <= 0) throw new IllegalArgumentException("Text scale must be positive and finite");
        textScale = f;
    }

    public void setLife(int f) {
        if (f <= 0) throw new IllegalArgumentException("Text lifetime must be positive");
        setLifetime(f);
    }

    @Override
    public ParticleRenderType getGroup() {
        return GROUP;
    }

    public TextState extract(float partialTick) {
        return new TextState(text, net.minecraft.util.Mth.lerp(partialTick, xo, x),
                net.minecraft.util.Mth.lerp(partialTick, yo, y), net.minecraft.util.Mth.lerp(partialTick, zo, z), textScale);
    }

    public record TextState(String text, double x, double y, double z, float scale) {}
}
