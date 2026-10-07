package reika.dragonapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import reika.dragonapi.interfaces.registry.SoundEnum;

/**
 * Client-side local sound playback.
 *
 * <p>Split out of {@code ReikaSoundHelper} because that class is loaded on a dedicated server -- the
 * mod-construction path reaches it through {@code SoundLoader} -- and naming
 * {@code SoundInstance}/{@code ClientLevel} in its signatures made the server fail to load it at all,
 * taking RotaryCraft and every mod after it down with it.
 */
public final class ClientSounds {

    private ClientSounds() {}

    public static void play(SoundEnum s, double x, double y, double z, float vol, float pitch, boolean attenuate) {
        float v = vol * s.getModulatedVolume();
        if (v <= 0)
            return;
        play(s.getSoundEvent(), s.getCategory(), x, y, z, v, pitch, attenuate);
    }

    public static void play(SoundEvent snd, double x, double y, double z, float vol, float pitch, boolean attenuate) {
        play(snd, SoundSource.AMBIENT, x, y, z, vol, pitch, attenuate);
    }

    private static void play(SoundEvent sound, SoundSource source, double x, double y, double z,
            float volume, float pitch, boolean attenuate) {
        if (Minecraft.getInstance().level == null)
            return;
        // The boolean on Level.playLocalSound is distance DELAY, not attenuation. Construct
        // the spatial instance explicitly so attenuated and global sounds both obey the caller.
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.location(),
                source, volume, pitch, SoundInstance.createUnseededRandom(), false, 0,
                attenuate ? SoundInstance.Attenuation.LINEAR : SoundInstance.Attenuation.NONE,
                x, y, z, false));
    }

    /** True once the sound engine exists. Kept here so DirectResourceManager -- which the dedicated
     *  server registers as a reload listener -- never names SoundManager itself. */
    public static void reloadResources() {
        var minecraft = Minecraft.getInstance();
        minecraft.execute(minecraft::reloadResourcePacks);
    }

    public static boolean hasSoundManager() {
        return Minecraft.getInstance().getSoundManager() != null;
    }
}
