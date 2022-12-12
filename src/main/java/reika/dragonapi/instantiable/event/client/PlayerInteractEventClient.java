package reika.dragonapi.instantiable.event.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class PlayerInteractEventClient extends PlayerEvent {

    public final Result action;
    public final int x;
    public final int y;
    public final int z;
    public final int face;
    public final Level world;

    public PlayerInteractEventClient(Player player, Result action, int x, int y, int z, int face, Level world) {
        super(player);
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.world = world;
    }
}
