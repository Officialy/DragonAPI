package reika.dragonapi.instantiable.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;

public class EntityAboutToRayTraceEvent extends EntityEvent {

    public final Vec3 startPos;
    public final Vec3 endPos;

    public EntityAboutToRayTraceEvent(Entity e) {
        super(e);

        startPos = e.position();
        endPos = new Vec3(e.position().x + e.getDeltaMovement().x, e.position().y + e.getDeltaMovement().y, e.position().z + e.getDeltaMovement().z);
    }

    public static void fire(Entity e) {
        MinecraftForge.EVENT_BUS.post(new EntityAboutToRayTraceEvent(e));
    }
}
