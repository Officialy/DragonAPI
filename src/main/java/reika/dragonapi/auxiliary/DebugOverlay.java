package reika.dragonapi.auxiliary;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.sdl.SDLKeyboard;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/** Client diagnostics extracted once per native HUD frame, without global render state. */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class DebugOverlay {
    public static final Identifier LAYER = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "debug");

    private DebugOverlay() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, (graphics, delta) -> drawDebugOverlay(graphics));
    }

    public static void drawDebugOverlay(GuiGraphicsExtractor graphics) {
        extract(graphics, DragonOptions.TABNBT.getState() && isDebugKeyDown(DragonOptions.DEBUGKEY.getValue()));
    }

    /** Configured values are SDL scancodes on 26.3; invalid legacy/user values must not index native memory. */
    private static boolean isDebugKeyDown(int key) {
        var keyboard = SDLKeyboard.SDL_GetKeyboardState();
        return keyboard != null && key >= 0 && key < keyboard.limit() && keyboard.get(key) != 0;
    }

    private static void extract(GuiGraphicsExtractor graphics, boolean inspectNBT) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.hud.isHidden()) return;
        if (DragonAPI.debugtest) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().scale(3, 3);
                graphics.text(minecraft.font, Component.translatable("overlay.dragonapi.debug.enabled"), 1, 1, 0xffffffff, false);
            } finally {
                graphics.pose().popMatrix();
            }
        }
        if (!inspectNBT || minecraft.gui.screen() != null) return;
        var hit = ReikaPlayerAPI.getLookedAtBlockClient(4, false);
        if (hit == null) return;
        var blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        if (blockEntity == null) return;
        List<String> lines = inspect(blockEntity);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(minecraft.font, lines.get(index),
                    1 + graphics.guiWidth() / 2 * (index / 24), 1 + minecraft.font.lineHeight * (index % 24), 0xffffffff, false);
        }
    }

    private static List<String> inspect(BlockEntity blockEntity) {
        try {
            // Original writeToNBT included type and coordinates; retain them along with modern components/attachments.
            return ReikaNBTHelper.parseNBTAsLines(blockEntity.saveWithFullMetadata(blockEntity.getLevel().registryAccess()));
        } catch (Exception error) {
            List<String> lines = new ArrayList<>();
            lines.add(ChatFormatting.RED + error.getClass().getName() + ": " + error.getLocalizedMessage());
            var trace = error.getStackTrace();
            for (int index = 0; index < Math.min(4, trace.length); index++) lines.add(trace[index].toString());
            return lines;
        }
    }
}
