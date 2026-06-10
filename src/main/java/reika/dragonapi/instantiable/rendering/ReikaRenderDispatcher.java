package reika.dragonapi.instantiable.rendering;

import net.minecraft.world.level.block.Block;
import reika.dragonapi.interfaces.IBlockRenderer;

public class ReikaRenderDispatcher {
    public static void init() {
        // Obsolete in 1.21.3 - replace with BakedModel
    }

    public static synchronized void registerBlockRenderer(Block block, IBlockRenderer renderer) {
        // Obsolete
    }

    public static synchronized void registerRenderer(IBlockRenderer renderer) {
        // Obsolete
    }
}
