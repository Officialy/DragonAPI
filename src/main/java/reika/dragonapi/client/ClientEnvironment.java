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
}
