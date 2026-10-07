package reika.dragonapi.instantiable.event.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerInteractEventClient extends PlayerEvent {

    public enum Result {
        ALLOW,
        DENY,
        DEFAULT
    }

    public final Result action;
    public final int x;
    public final int y;
    public final int z;
    public final net.minecraft.core.Direction face;
    public final Level world;

    public PlayerInteractEventClient(Player player, Result action, int x, int y, int z, int face, Level world) {
        this(player, action, x, y, z, legacyFace(face), world);
    }

    private static net.minecraft.core.Direction legacyFace(int face) {
        if (face == -1) return null; // A click in the air has no block face.
        if (face < 0 || face > 5) throw new IllegalArgumentException("Invalid block face " + face);
        return net.minecraft.core.Direction.from3DDataValue(face);
    }

    public PlayerInteractEventClient(Player player, Result action, int x, int y, int z, net.minecraft.core.Direction face, Level world) {
        super(player);
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.world = world;
    }
}

