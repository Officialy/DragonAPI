package reika.dragonapi.network;

import net.minecraft.resources.Identifier;
import reika.dragonapi.DragonAPI;

/**
 * Helper class for DragonAPI network channel management.
 */
public final class DragonNetwork {
    
    private DragonNetwork() {}
    
    /**
     * Creates a Identifier in the DragonAPI namespace.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DragonAPI.MODID, path);
    }
}









