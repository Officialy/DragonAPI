package reika.dragonapi.instantiable.io;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import reika.dragonapi.interfaces.registry.SoundEnum;

public class EnumSound extends AbstractSoundInstance {
    public final SoundEnum sound;

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
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = vol;
        this.pitch = p;
        this.looping = false;
        this.delay = 0;
        this.attenuation = att ? Attenuation.LINEAR : Attenuation.NONE;
        this.relative = false;
    }

    public EnumSound setRepeating() {
        this.looping = true;
        return this;
    }
}
