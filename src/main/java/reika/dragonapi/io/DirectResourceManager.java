package reika.dragonapi.io;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import reika.dragonapi.client.ClientSounds;
import net.neoforged.api.distmarker.OnlyIn;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.io.DirectResource;
import reika.dragonapi.instantiable.io.DynamicDirectResource;
import reika.dragonapi.instantiable.io.RemoteSourcedAsset;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


public class DirectResourceManager implements ResourceManager, ResourceManagerReloadListener {

    //    private final HashMap<String, SoundEventAccessorComposite> accessors = new HashMap<>();
    private final HashMap<String, RemoteSourcedAsset> dynamicAssets = new HashMap<>();
    private final HashSet<String> streamedPaths = new HashSet<>();

    private static final DirectResourceManager instance = new DirectResourceManager();

    private DirectResourceManager() {
        super();
    }

    public static DirectResourceManager getInstance() {
        return instance;
    }

    @Override
    public Optional<Resource> getResource(Identifier loc) {
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

        if (streaming) {
            streamedPaths.add(path);
        }
    }

    public void initToSoundRegistry() {
        // A dedicated server reloads resources too, and this listener is registered on both dists;
        // touching the sound manager there resolves client classes that do not exist.
        if (!FMLEnvironment.getDist().isClient())
            return;
        if (!ClientSounds.hasSoundManager()) {
            DragonAPI.LOGGER.error("Attempted to initialize sound entries before the sound handler was created!");
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
        return null;
    }

    @Override
    public List<Resource> getResourceStack(Identifier resource) {
        return List.of(this.getResource(resource).get());
    }

    @Override
    public Map<Identifier, Resource> listResources(String directory, ResourceManager.Selector selector) {
        return Map.of(); // serves individual dynamic assets through getResource only; nothing to enumerate
    }

    @Override
    public Map<Identifier, List<Resource>> listResourceStacks(String directory, ResourceManager.Selector selector) {
        return Map.of();
    }

    @Override
    public Stream<PackResources> listPacks() {
        return Stream.empty();
    }
}
