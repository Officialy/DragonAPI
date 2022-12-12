package reika.dragonapi.instantiable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.TimerMap;

public class BlockUpdateCallback implements TimerMap.TimerCallback {
    public final WorldLocation location;

    public BlockUpdateCallback(Level world, int x, int y, int z) {
        this(new WorldLocation(world, x, y, z));
    }

    public BlockUpdateCallback(BlockEntity te) {
        this(new WorldLocation(te));
    }

    public BlockUpdateCallback(WorldLocation loc) {
        location = loc;
    }

    @Override
    public void call() {
        location.triggerBlockUpdate(false);
    }

}
