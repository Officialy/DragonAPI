package reika.dragonapi.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import reika.dragonapi.client.ParticleStateAccess;
import reika.dragonapi.interfaces.ControllerTarget;

@Mixin(Particle.class)
public abstract class MixinParticleState implements ParticleStateAccess {
    @Shadow protected double x,y,z,xd,yd,zd;
    @Shadow protected int age;
    @Override public ControllerTarget dragonapi$controllerTarget() {
        return new ControllerTarget(x,y,z,new Vec3(xd,yd,zd),age);
    }
}
