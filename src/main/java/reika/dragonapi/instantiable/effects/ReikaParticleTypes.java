package reika.dragonapi.instantiable.effects;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import reika.dragonapi.DragonAPI;
import net.minecraft.core.registries.BuiltInRegistries;

public class ReikaParticleTypes {

    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, DragonAPI.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STRING = REGISTRY.register("string", () -> new SimpleParticleType(true));

}
