package reika.dragonapi.client;

import java.util.Queue;
import net.minecraft.client.particle.Particle;

/** Internal native group ownership bridge for retiring a level's particles before clearing its collections. */
public interface ParticleGroupAccess { Queue<Particle> dragonapi$getParticles();
    net.minecraft.client.particle.ParticleEngine dragonapi$getEngine(); }
