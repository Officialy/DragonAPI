package reika.dragonapi.instantiable.io;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.io.DirectResourceManager;

public class EnumSound implements SoundInstance {

    public final double posX;
    public final double posY;
    public final double posZ;
    public final float volume;
    public final float pitch;

    public final SoundEnum sound;
    private final ResourceLocation res;

    public final boolean attenuate;

    private boolean repeat = false;

    public EnumSound(SoundEnum obj, SoundInstance ref) {
        this(obj, ref, ref.getAttenuation() != SoundInstance.Attenuation.NONE);
    }

    public EnumSound(SoundEnum obj, SoundInstance ref, boolean atten) {
        this(obj, ref.getX(), ref.getY(), ref.getZ(), ref.getVolume(), ref.getPitch(), atten);
    }

    public EnumSound(SoundEnum obj, double x, double y, double z, float vol, float p, boolean att) {
        sound = obj;
        res = obj.getPath();
        posX = x;
        posY = y;
        posZ = z;
        volume = vol;
        pitch = p;
        attenuate = att;
    }

    public EnumSound setRepeating() {
        repeat = true;
        return this;
    }

    @Override
    public ResourceLocation getLocation() {
        return res;
    }

    //todo FIX THESE NULLS
    @Nullable
    @Override
    public WeighedSoundEvents resolve(SoundManager pManager) {
        return pManager.getSoundEvent(res); //todo check if this is right
    }

    @Override
    public Sound getSound() {
        return (Sound) sound; //todo check if this works
    }

    @Override
    public SoundSource getSource() {
        return sound.getCategory(); //todo check if this is right
    }

    @Override
    public boolean isLooping() {
        return repeat;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public int getDelay() {
        return 0;
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
    public Attenuation getAttenuation() {
        return attenuate ? Attenuation.LINEAR : Attenuation.NONE;
    }
}
