package reika.dragonapi.client;

import net.minecraft.client.particle.SingleQuadParticle;

/** Native layer classification for preserving separate terrain/item/particle-sheet budgets. */
public interface ParticleLayerAccess { SingleQuadParticle.Layer dragonapi$getLayer(); }
