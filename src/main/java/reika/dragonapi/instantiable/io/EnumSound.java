package reika.dragonapi.instantiable.io;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import reika.dragonapi.interfaces.registry.SoundEnum;

public class EnumSound extends AbstractSoundInstance {
    public final SoundEnum sound;

    private final double posX;
    private final double posY;
    private final double posZ;
    private final float vol;
    private final float pit;
    private final boolean atten;

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
        this.vol = vol;
        this.pit = p;
        this.atten = att;
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
        return vol;
    }

    @Override
    public float getPitch() {
        return pit;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public Attenuation getAttenuation() {
        return atten ? Attenuation.LINEAR : Attenuation.NONE;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public int getDelay() {
        return 0;
    }

    public EnumSound setRepeating() {
        return this;
    }
}
