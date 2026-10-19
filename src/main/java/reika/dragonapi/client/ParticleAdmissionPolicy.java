package reika.dragonapi.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;

/** Optional policy for a native engine with custom, frozen per-particle render batches. */
public interface ParticleAdmissionPolicy {
    void validateAdmission(Particle particle);
    void particleAdmitted(Particle particle);
    Object particleBucket(Particle particle);
    boolean countsToParticleLimit(Particle particle);
    void retireParticle(Particle particle);
    SingleQuadParticle.Layer particleLayer(Particle particle,SingleQuadParticle.Layer source);
}
