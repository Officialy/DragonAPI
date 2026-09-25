package reika.dragonapi.instantiable.rendering;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn in first person like a vanilla map: held in both hands when the off hand is empty, otherwise in one,
 * with the arms and swing that vanilla gives maps. 1.7.10 mods copied vanilla's map-hand code into an
 * IItemRenderer to get this; here a mod registers the item and draws only the map face.
 * {@code MixinItemInHandRenderer} routes registered items through vanilla's map path.
 */
public final class HeldMapRenderers {

    /** Draws the map face. The pose is vanilla's map space: 0..128 in x and y, z toward the viewer negative. */
    @FunctionalInterface
    public interface HeldMapRenderer {
        void render(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ItemStack stack);
    }

    private static final Map<Item, HeldMapRenderer> renderers = new IdentityHashMap<>();

    private HeldMapRenderers() {}

    public static void register(Item item, HeldMapRenderer renderer) {
        renderers.put(item, renderer);
    }

    public static HeldMapRenderer get(ItemStack stack) {
        return stack.isEmpty() ? null : renderers.get(stack.getItem());
    }

}
