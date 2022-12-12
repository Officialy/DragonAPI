package reika.dragonapi.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface WinterBiomeStrengthControl {

    float getWinterSkyStrength(Level world, Player ep);

}
