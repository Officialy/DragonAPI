package reika.dragonapi.io;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.io.DirectResource;
import reika.dragonapi.instantiable.io.DynamicDirectResource;
import reika.dragonapi.instantiable.io.RemoteSourcedAsset;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class DirectResourceManager implements ResourceManager, ResourceManagerReloadListener {

    //    private final HashMap<String, SoundEventAccessorComposite> accessors = new HashMap<>();
    private final HashMap<String, RemoteSourcedAsset> dynamicAssets = new HashMap<>();
    private final HashSet<String> streamedPaths = new HashSet<>();

    private static final DirectResourceManager instance = new DirectResourceManager();

    private static final String TAG = "custom_path";

    private DirectResourceManager() {
        super();
    }

    public static DirectResourceManager getInstance() {
        return instance;
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(TAG, path);
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation loc) {
        String dom = loc.getNamespace();
        String path = loc.getPath();
        RemoteSourcedAsset rem = dynamicAssets.get(path);
        DirectResource ret = rem != null ? new DynamicDirectResource(rem) : new DirectResource(path);
        if (streamedPaths.contains(ret.path))
            ret.cacheData = false;
        return Optional.of(ret);
    }

    public void registerDynamicAsset(String path, RemoteSourcedAsset a) {
        dynamicAssets.put(path, a);
    }

    public void registerCustomPath(String path, SoundSource cat, boolean streaming) {
        ResourceLocation rl = new ResourceLocation(TAG, path);
//        SoundPoolEntry spe = new SoundPoolEntry(rl, 1, 1, streaming);
        WeighedSoundEvents pos = new WeighedSoundEvents(rl, null);
//        SoundEventAccessorComposite cmp = new SoundEventAccessorComposite(rl, 1, 1, cat);
//        cmp.addSoundToEventPool(pos);
//        accessors.put(path, cmp);
        if (streaming) {
            streamedPaths.add(path);
        }
    }

    public void initToSoundRegistry() {
        SoundManager sh = Minecraft.getInstance().getSoundManager();
        if (sh == null) {
            DragonAPI.LOGGER.error("Attempted to initialize sound entries before the sound handler was created!");
            return;
        }
//        SoundRegistry srg = sh.sndRegistry;
//        if (srg == null) {
//            DragonAPI.LOGGER.error("Attempted to initialize sound entries before the sound registry was created!");
//            return;
//        }
//        for (String path : accessors.keySet()) {
//            srg.registerSound(accessors.get(path));
//        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager rm) {
//  todo      ((ReloadableResourceManager)rm).domainResourceManagers.put(TAG, this);
        this.initToSoundRegistry();
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of(TAG);
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation resource) {
        return List.of(this.getResource(resource).get());
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String p_215563_, Predicate<ResourceLocation> p_215564_) {
        return null;
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215565_, Predicate<ResourceLocation> p_215566_) {
        return null;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return null;
    }
}
