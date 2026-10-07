package reika.dragonapi.libraries.io;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import reika.dragonapi.interfaces.registry.SoundEnum;

public class SingleSound implements SoundEnum {

    public final String name;
    public final Identifier path;


    private SoundSource category;

    public SingleSound(String n, Identifier p) {
        name = n;
        path = p;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Identifier getPath() {
        return path;
    }


    public void setSoundSource(SoundSource cat) {
        category = cat;
    }

    @Override

    public SoundSource getCategory() {
        return category != null ? category : SoundSource.MASTER;
    }

    @Override
    public int ordinal() {
        return 0;
    }

    @Override
    public boolean canOverlap() {
        return true;
    }

    @Override
    public void playSound(Level world, BlockPos pos, float volume, float pitch) {
        ReikaSoundHelper.playSound(this, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch);
    }

    @Override
    public void playSound(Entity e, float volume, float pitch) {
        ReikaSoundHelper.playSound(this, e.level(), e, volume, pitch);
    }

    @Override
    public void playSound(Level world, BlockPos pos, float volume, float pitch, boolean attenuate) {
        ReikaSoundHelper.playSound(this, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch, attenuate);
    }

    @Override
    public void playSoundNoAttenuation(Level world, BlockPos pos, float volume, float pitch, int range) {
        reika.dragonapi.libraries.io.ReikaPacketHelper.sendSoundPacket(this, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch, false, range);
    }

    @Override
    public boolean attenuate() {
        return true;
    }

    @Override

    public float getModulatedVolume() {
        return 1;
    }

    @Override
    public boolean preload() {
        return false;
    }

    @Override
    public SoundEvent getSoundEvent() {
        // SingleSound doesn't have registered sound events, so create a temporary one
        return SoundEvent.createVariableRangeEvent(path);
    }
}
