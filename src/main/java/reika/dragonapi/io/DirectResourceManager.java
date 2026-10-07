package reika.dragonapi.io;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.io.*;

/** Owned client asset pack for explicitly registered filesystem, classpath and downloaded assets.
 * Sound definitions belong to the owner's datagen provider, just like ordinary packaged sounds. */
public final class DirectResourceManager implements ResourceManager, ResourceManagerReloadListener {
    private static final DirectResourceManager INSTANCE = new DirectResourceManager();
    private final Map<Identifier, DirectResource> assets = new LinkedHashMap<>();
    private final AssetPack pack = new AssetPack();
    private DirectResourceManager() {}
    public static DirectResourceManager getInstance() { return INSTANCE; }
    public PackResources sourcePack() { return pack; }

    @Override public synchronized Optional<Resource> getResource(Identifier id) {
        return Optional.ofNullable(assets.get(id));
    }
    public void registerDynamicAsset(String path, RemoteSourcedAsset asset) {
        registerDynamicAsset(Identifier.fromNamespaceAndPath(asset.reference, path), asset);
    }
    public synchronized void registerDynamicAsset(Identifier id, RemoteSourcedAsset asset) {
        assets.put(id, new DynamicDirectResource(asset));
    }
    public void registerCustomPath(String path, SoundSource category, boolean streaming) {
        registerCustomPath(Identifier.parse(path), path, category, streaming);
    }
    public void registerCustomPath(Identifier id, SoundSource category, boolean streaming) {
        registerCustomPath(id, "assets/" + id.getNamespace() + "/" + id.getPath(), category, streaming);
    }
    public synchronized void registerCustomPath(Identifier id, String sourcePath, SoundSource category, boolean streaming) {
        DirectResource resource = new DirectResource(sourcePath);
        resource.cacheData = !streaming;
        assets.put(id, resource);
    }
    public synchronized void unregister(Identifier id) { assets.remove(id); }
    @Override public synchronized Set<String> getNamespaces() {
        Set<String> namespaces = new TreeSet<>();
        assets.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        return Collections.unmodifiableSet(namespaces);
    }
    @Override public List<Resource> getResourceStack(Identifier id) {
        return getResource(id).map(List::of).orElseGet(List::of);
    }
    @Override public synchronized Map<Identifier, Resource> listResources(String directory, ResourceManager.Selector selector) {
        Map<Identifier, Resource> result = new LinkedHashMap<>();
        String prefix = directory.isEmpty() ? "" : directory + "/";
        assets.forEach((id, resource) -> {
            if (id.getPath().startsWith(prefix) && selector.isIncluded(id)) result.put(id, resource);
        });
        return Collections.unmodifiableMap(result);
    }
    @Override public Map<Identifier, List<Resource>> listResourceStacks(String directory, ResourceManager.Selector selector) {
        Map<Identifier, List<Resource>> result = new LinkedHashMap<>();
        listResources(directory, selector).forEach((id, resource) -> result.put(id, List.of(resource)));
        return Collections.unmodifiableMap(result);
    }
    @Override public Stream<PackResources> listPacks() { return Stream.of(pack); }
    public void initToSoundRegistry() {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()
                && reika.dragonapi.client.ClientEnvironment.hasGameInstance())
            reika.dragonapi.client.ClientSounds.reloadResources();
    }
    @Override public synchronized void onResourceManagerReload(ResourceManager manager) {
        assets.values().forEach(DirectResource::clearCache);
    }
    public static void addPack(net.neoforged.neoforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        var resources = new Pack.ResourcesSupplier() {
            @Override public PackMetadataResources openMetadata(PackLocationInfo location) { return INSTANCE.pack; }
            @Override public Stream<PackResources> openResources(PackLocationInfo location, Pack.Metadata metadata) { return Stream.of(INSTANCE.pack); }
        };
        var result = Pack.readMetaAndCreate(INSTANCE.pack.location(), resources, event.getPackType(),
                new PackSelectionConfig(true, Pack.Position.TOP, true));
        if (result == null) throw new IllegalStateException("Could not read DragonAPI asset pack metadata");
        event.addRepositorySource(consumer -> consumer.accept(result));
    }
    private final class AssetPack implements PackResources {
        private final PackLocationInfo location = new PackLocationInfo("dragonapi/direct_assets",
                Component.literal("DragonAPI direct assets"), PackSource.BUILT_IN, Optional.empty());
        @Override public PackLocationInfo location() { return location; }
        @Override public IoSupplier<InputStream> getRootResource(String... path) { return null; }
        @Override public <T> T getMetadataSection(MetadataSectionType<T> type) {
            var version = net.minecraft.SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
            var metadata = new PackMetadataSection(Component.literal("Registered DragonAPI assets"), new net.minecraft.util.InclusiveRange<>(version));
            return PackMetadataSection.CLIENT_TYPE.withValue(metadata).unwrapToType(type).orElse(null);
        }
        @Override public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
            if (type != PackType.CLIENT_RESOURCES) return null;
            return DirectResourceManager.this.getResource(id).<IoSupplier<InputStream>>map(resource -> resource::open).orElse(null);
        }
        @Override public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
            if (type == PackType.CLIENT_RESOURCES)
                DirectResourceManager.this.listResources(directory, id -> id.getNamespace().equals(namespace))
                        .forEach((id, resource) -> output.accept(id, resource::open));
        }
        @Override public Set<String> getNamespaces(PackType type) {
            return type == PackType.CLIENT_RESOURCES ? DirectResourceManager.this.getNamespaces() : Set.of();
        }
        @Override public void close() { DirectResourceManager.this.onResourceManagerReload(DirectResourceManager.this); }
    }
}
