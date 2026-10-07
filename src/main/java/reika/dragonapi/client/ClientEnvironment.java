package reika.dragonapi.client;

import java.io.File;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;

/**
 * Client-only environment lookups, isolated so that no class the dedicated server loads names
 * {@link Minecraft}. Callers must gate on {@code FMLEnvironment.dist.isClient()} first.
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
    public static net.minecraft.world.entity.player.Player player() {
        return Minecraft.getInstance().player;
    }

    /** The client's own level. Its declared type is ClientLevel, which is why this cannot live in a
     *  class the dedicated server loads: verifying such a method resolves the field descriptor. */
    public static net.minecraft.world.level.Level level() {
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
            e.printStackTrace();
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
        Minecraft.getInstance().gui.hud.getChat().clearMessages(true);
    }

    /** Opens a resource from the *client* resource manager (assets), not the server's data packs. */
    public static java.io.InputStream resourceStream(net.minecraft.resources.Identifier id) throws java.io.IOException {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        return resource.isPresent() ? resource.get().open() : null;
    }
}
