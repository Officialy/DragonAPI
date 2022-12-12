package reika.dragonapi.instantiable.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;

public class RawKeyPressEvent extends Event {

    public final KeyWatcher.Key key;
    public final Player player;

    public RawKeyPressEvent(KeyWatcher.Key k, Player ep) {
        key = k;
        player = ep;
    }

}
