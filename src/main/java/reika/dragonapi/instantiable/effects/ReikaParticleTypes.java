package reika.dragonapi.instantiable.effects;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import reika.dragonapi.DragonAPI;

public class ReikaParticleTypes {

    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DragonAPI.MODID);

    public static final RegistryObject<SimpleParticleType> STRING = REGISTRY.register("string", () -> new SimpleParticleType(true));

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpecial(STRING.get(), (type, level, x, y, z, xd, yd, zd) -> new StringParticleFX(level, x, y, z, null, xd, yd, zd));
    }

}
