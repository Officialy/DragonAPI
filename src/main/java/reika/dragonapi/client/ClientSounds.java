package reika.dragonapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
        // playLocalSound builds a PositionedSoundInstance internally, which is the only thing that
        // can reach AbstractSoundInstance's package-private fields and so get 3D panning right.
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
            level.playLocalSound(x, y, z, s.getSoundEvent(), s.getCategory(), v, pitch, attenuate);
    }

    public static void play(SoundEvent snd, double x, double y, double z, float vol, float pitch, boolean attenuate) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
            level.playLocalSound(x, y, z, snd, SoundSource.AMBIENT, vol, pitch, attenuate);
    }

    /** True once the sound engine exists. Kept here so DirectResourceManager -- which the dedicated
     *  server registers as a reload listener -- never names SoundManager itself. */
    public static boolean hasSoundManager() {
        return Minecraft.getInstance().getSoundManager() != null;
    }
}
