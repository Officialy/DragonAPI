/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import reika.dragonapi.instantiable.data.maps.MultiMap;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

public class BlockEntityTickWatcher {

    public static final BlockEntityTickWatcher instance = new BlockEntityTickWatcher();

    private final MultiMap<Class, TileWatcher> data = new MultiMap();

    private BlockEntityTickWatcher() {

    }

    public static void tick(BlockEntity te) {
        instance.tickTile(te);
    }

    public void watchTiles(Class type, TileWatcher t) {
        data.addValue(type, t);
    }

    private void tickTile(BlockEntity te) {
        Collection<TileWatcher> c = data.get(te.getClass());
        if (c != null) {
            for (TileWatcher tile : c) {
                tile.onTick(te);
            }
        }
    }

    public interface TileWatcher {

        void onTick(BlockEntity te);

        boolean tickOnce();

    }

}
