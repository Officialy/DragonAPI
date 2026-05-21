package reika.dragonapi.network;

import net.minecraft.resources.ResourceLocation;
import reika.dragonapi.DragonAPI;

/**
 * Helper class for DragonAPI network channel management.
 */
public final class DragonNetwork {
    
    private DragonNetwork() {}
    
    /**
     * Creates a ResourceLocation in the DragonAPI namespace.
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DragonAPI.MODID, path);
    }
}









