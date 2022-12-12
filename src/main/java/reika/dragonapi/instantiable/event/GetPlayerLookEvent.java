package reika.dragonapi.instantiable.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class GetPlayerLookEvent extends PlayerEvent {

    public final HitResult originalLook;

    public final Vec3 playerVec;
    public final Vec3 auxVec;

    public HitResult newLook;

    public GetPlayerLookEvent(Player ep, HitResult mov, Vec3 v1, Vec3 v2) {
        super(ep);
        originalLook = mov;
        newLook = mov;

        playerVec = v1;
        auxVec = v2;
    }
}
