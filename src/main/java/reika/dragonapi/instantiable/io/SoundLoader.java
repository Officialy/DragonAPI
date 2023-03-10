package reika.dragonapi.instantiable.io;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLLoader;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.interfaces.registry.SoundEnum;
import reika.dragonapi.interfaces.registry.StreamableSound;
import reika.dragonapi.interfaces.registry.VariableSound;
import reika.dragonapi.io.DirectResourceManager;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.io.SingleSound;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class SoundLoader {

    private final Class<? extends SoundEnum> soundClass;
    private final HashMap<SoundEnum, SoundResource> soundMap = new HashMap();

    public SoundLoader(SoundEnum... ss) {
        if (ss.length == 0)
            throw new IllegalArgumentException("You cannot register an empty sound list!");
        soundClass = ss[0].getClass();
        for (SoundEnum s : ss) {
            this.addToMap(s);
        }
        this.init();
    }

    public SoundLoader(Class<? extends SoundEnum> c) {
        soundClass = c;
        for (SoundEnum s : c.getEnumConstants()) {
            this.addToMap(s);
        }
        this.init();
    }

    private void addToMap(SoundEnum s) {
        soundMap.put(s, new SoundResource(s));
        if (s instanceof VariableSound) {
            for (SoundVariant<?> var : ((VariableSound) s).getVariants()) {
                this.addToMap(var);
            }
        }
    }

    private void init() {
        if (soundClass == SingleSound.class) {
            for (SoundEnum s : soundMap.keySet()) {
                ReikaSoundHelper.registerSingleSound((SingleSound) s);
                DragonAPI.LOGGER.info("Registered sound "+s+" as a single sound");
            }
        } else {
            ReikaSoundHelper.registerSoundSet(soundClass);
            DragonAPI.LOGGER.info("Registered sound set "+soundClass.getSimpleName());
        }
    }

    public final void register() {
        for (Map.Entry<SoundEnum, SoundResource> et : soundMap.entrySet()) {
            this.registerSound(et.getKey(), et.getValue());
            DragonAPI.LOGGER.info("Registered sound enum: "+et.getKey() + " as " + et.getValue().reference);
        }
    }

    private void registerSound(SoundEnum e, SoundResource sr) {
        ResourceLocation p = e.getPath();
        boolean stream = e instanceof StreamableSound && ((StreamableSound) e).isStreamed();
//        DirectResourceManager.getInstance().registerCustomPath(p, e.getCategory(), stream);
        this.onRegister(e, p);
        if (e.preload()) {
            try {
                sr.resource = (DirectResource) DirectResourceManager.getInstance().getResource(sr.reference).orElseThrow();
                if (stream)
                    sr.resource.cacheData = false;
            } catch (NoSuchElementException ex) {
                DragonAPI.LOGGER.error("Caught error when preloading sound '" + e + "':");
                ex.printStackTrace();
            }
        }
    }

    public final ResourceLocation getResource(SoundEnum sound) {
        return soundMap.get(sound).reference;
    }

    protected void onRegister(SoundEnum e, ResourceLocation p) {

    }

    private static class SoundResource {

        private final SoundEnum sound;
        private final ResourceLocation reference;

        private DirectResource resource;

        private SoundResource(SoundEnum s) {
            sound = s;
            reference = FMLLoader.getDist().isClient() ? getReference(s) : null;
        }

        private static ResourceLocation getReference(SoundEnum s) {
            return s.getPath();
        }

    }
}
