package reika.dragonapi.interfaces;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Common immutable controller input for entities and client particles. No client types leak here. */
public record ControllerTarget(double x,double y,double z,Vec3 velocity,int age) {
    public static ControllerTarget of(Entity entity) {
        return entity==null?null:new ControllerTarget(entity.getX(),entity.getY(),entity.getZ(),entity.getDeltaMovement(),entity.tickCount);
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public Vec3 getDeltaMovement() { return velocity; }
    public double distanceToSqr(double tx,double ty,double tz) {
        double dx=x-tx,dy=y-ty,dz=z-tz; return dx*dx+dy*dy+dz*dz;
    }
    public double distanceTo(double tx,double ty,double tz) { return Math.sqrt(distanceToSqr(tx,ty,tz)); }
    public double distanceTo(Entity target) { return Math.sqrt(distanceToSqr(target.getX(),target.getY(),target.getZ())); }
}
