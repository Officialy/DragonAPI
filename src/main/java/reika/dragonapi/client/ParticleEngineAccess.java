package reika.dragonapi.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.particles.ParticleLimit;
import reika.dragonapi.extras.ThrottleableEffectRenderer;

/** Internal bridge implemented on each actual native engine; no engine/resource replacement. */
public interface ParticleEngineAccess {
    ThrottleableEffectRenderer dragonapi$getController();
    void dragonapi$attachController(ThrottleableEffectRenderer controller);
    void dragonapi$changeCount(ParticleLimit limit,int change);
    ClientLevel dragonapi$getLevel();
    ParticleResources dragonapi$getResources();
    int dragonapi$countOwnedParticles();
}
