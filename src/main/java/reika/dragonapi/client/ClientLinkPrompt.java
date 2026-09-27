package reika.dragonapi.client;

import com.mojang.blaze3d.Blaze3D;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

/**
 * The confirm-before-opening-a-link prompt, kept in its own class because it names
 * {@link ConfirmLinkScreen}.
 *
 * <p>This used to live directly in {@code DragonAPI}, and that stopped the whole stack from running
 * on a dedicated server: FML constructs a mod by reflecting over its class, which eagerly resolves
 * the bootstrap methods of any lambda in it, and the lambda here named a screen type that does not
 * exist server-side. Mod construction failed and every dependent mod was skipped. No class the
 * dedicated server loads may name a client-only type.
 */
public final class ClientLinkPrompt {

    private ClientLinkPrompt() {}

    public static void openURL(String url) {
        // 26.3: link screens and the platform opener take a URI; Util$OS#openUri moved to Blaze3D.
        URI uri = URI.create(url);
        Minecraft.getInstance().gui.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed)
                Blaze3D.openUri(uri);
            Minecraft.getInstance().gui.setScreen(null);
        }, uri, true));
    }
}
