package reika.dragonapi.libraries.io;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import reika.dragonapi.interfaces.registry.SoundEnum;

public class SingleSound implements SoundEnum {

    public final String name;
    public final String path;


    private SoundSource category;

    public SingleSound(String n, String p) {
        name = n;
        path = p;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
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

    }

    @Override
    public void playSound(Entity e, float volume, float pitch) {

    }

    @Override
    public void playSound(Level world, BlockPos pos, float volume, float pitch, boolean attenuate) {

    }

    @Override
    public void playSoundNoAttenuation(Level world, BlockPos pos, float volume, float pitch, int range) {

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
}
