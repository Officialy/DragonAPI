package reika.dragonapi.interfaces;

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import net.minecraft.world.entity.Entity;

public interface EntityPathfinder {

    /**
     * Return null to indicate completion or no valid path.
     */
    DecimalPosition getNextWaypoint(Entity e);

    boolean isInRange(Entity e);

}
