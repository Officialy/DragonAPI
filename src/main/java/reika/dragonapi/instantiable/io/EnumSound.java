package reika.dragonapi.instantiable.io;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;
import reika.dragonapi.interfaces.registry.SoundEnum;

/** Physical-client sound-engine adapter; common emission uses ReikaSoundHelper and its client bridge. */
public class EnumSound extends AbstractSoundInstance {
    public final SoundEnum sound;

    public final double posX;
    public final double posY;
    public final double posZ;
    public final float volume;
    public final float pitch;
    public final boolean attenuate;

    public EnumSound(SoundEnum obj, SoundInstance ref) {
        this(obj, ref, ref.getAttenuation() != Attenuation.NONE);
    }

    public EnumSound(SoundEnum obj, SoundInstance ref, boolean atten) {
        this(obj, ref.getX(), ref.getY(), ref.getZ(), ref.getVolume(), ref.getPitch(), atten);
    }

    public EnumSound(
        SoundEnum obj, double x, double y, double z, float vol, float p, boolean att
    ) {
        super(obj.getSoundEvent(), obj.getCategory(), RandomSource.create());
        this.sound = obj;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.volume = vol;
        this.pitch = p;
        this.attenuate = att;
    }

    @Override
    public double getX() {
        return posX;
    }

    @Override
    public double getY() {
        return posY;
    }

    @Override
    public double getZ() {
        return posZ;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public Attenuation getAttenuation() {
        return attenuate ? Attenuation.LINEAR : Attenuation.NONE;
    }

    @Override
    public boolean isLooping() {
        return looping;
    }

    @Override
    public int getDelay() {
        return 0;
    }

    public EnumSound setRepeating() {
        looping = true;
        return this;
    }
}
