package reika.dragonapi.client;

import java.io.File;
import java.io.InputStream;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;

/**
 * Client-only environment lookups, isolated so that no class the dedicated server loads names
 * {@link Minecraft}. Callers must gate on {@code FMLEnvironment.getDist().isClient()} first.
 */
public final class ClientEnvironment {

    private ClientEnvironment() {}

    public static File gameDirectory() {
        return Minecraft.getInstance().gameDirectory;
    }

    public static GameProfile sessionProfile() {
        return Minecraft.getInstance().getGameProfile();
    }

    public static boolean isLocalServer() {
        return Minecraft.getInstance().isLocalServer();
    }

    /** The local player. Declared LocalPlayer on Minecraft, so the same rule applies as for level(). */
    public static @Nullable Player player() {
        return Minecraft.getInstance().player;
    }

    /** The client's own level. Its declared type is ClientLevel, which is why this cannot live in a
     *  class the dedicated server loads: verifying such a method resolves the field descriptor. */
    public static @Nullable Level level() {
        return Minecraft.getInstance().level;
    }

    /** The launcher's version string, read reflectively as it has no accessor. */
    public static String launchedVersion() {
        try {
            java.lang.reflect.Field f = Minecraft.class.getDeclaredField("launchedVersion");
            f.setAccessible(true);
            return (String)f.get(Minecraft.getInstance());
        }
        catch (Exception e) {
            reika.dragonapi.DragonAPI.LOGGER.error("Failed to read the client launched-version field", e);
            return e.toString();
        }
    }

    /** A stable per-user key for client-side config: the session UUID, falling back to the name. */
    public static String profileIdentifier() {
        java.util.UUID id = Minecraft.getInstance().getUser().getProfileId();
        return id != null ? id.toString() : Minecraft.getInstance().getUser().getName();
    }

    /** True once the game instance exists; false during client datagen, where there is none. Like
     *  everything here this is client-only — it cannot answer "am I on a server", it throws there. */
    public static boolean hasGameInstance() {
        return Minecraft.getInstance() != null;
    }

    public static void clearChat() {
        var minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.hud.getChat().clearMessages(true));
    }

    public static void writeChatString(String message) {
        var minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) reika.dragonapi.libraries.io.ReikaChatHelper.sendChatToPlayer(minecraft.player, message);
        });
    }

    public static void writeChatComponent(net.minecraft.network.chat.Component message) {
        var minecraft = Minecraft.getInstance();
        var snapshot = message.copy();
        minecraft.execute(() -> {
            if (minecraft.player != null) minecraft.player.sendSystemMessage(snapshot);
        });
    }

    /** Opens a resource from the *client* resource manager (assets), not the server's data packs. */
    public static @Nullable InputStream resourceStream(net.minecraft.resources.Identifier id) throws java.io.IOException {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        return resource.isPresent() ? resource.get().open() : null;
    }
}
