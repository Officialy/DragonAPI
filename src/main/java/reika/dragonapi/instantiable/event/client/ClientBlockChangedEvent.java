package reika.dragonapi.instantiable.event.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

/** Native client block-dirty notification, after the world's state change; custom model dependency invalidation only. */
public final class ClientBlockChangedEvent extends Event {
    public final ClientLevel level;
    public final BlockPos pos;
    public final BlockState previous, current;
    public ClientBlockChangedEvent(ClientLevel level, BlockPos pos, BlockState previous, BlockState current) {
        this.level=level; this.pos=pos.immutable(); this.previous=previous; this.current=current;
    }
}
