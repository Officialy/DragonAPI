package reika.dragonapi.interfaces;

import net.minecraft.world.level.Level;
import reika.dragonapi.interfaces.registry.OreEnum;

import java.util.Random;

public interface OreGenerator {

    void generateOre(OreEnum ore, Random random, Level world, int chunkX, int chunkZ);

}
