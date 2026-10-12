package reika.dragonapi.auxiliary.trackers;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.worldgen.PopulationScope;

/** Shared, persisted admission for finite pregeneration, independent of optional preloader mods. */
@EventBusSubscriber(modid = DragonAPI.MODID)
public final class BoundedPopulation extends SavedData {
    public interface Queue { long pending(Predicate<ChunkPos> origins); }
    private static final Map<ServerLevel, BoundedPopulation> CACHE = new ConcurrentHashMap<>();
    public static final Codec<BoundedPopulation> CODEC = CompoundTag.CODEC.xmap(BoundedPopulation::new, BoundedPopulation::save);
    private static final SavedDataType<BoundedPopulation> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DragonAPI.MODID, "bounded_population"), BoundedPopulation::new,
            CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    private final Map<Long, Session> sessions = new LinkedHashMap<>();
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private long nextId = 1;
    private int ticks;

    private static final class Session {
        long id;
        String source, key;
        PopulationScope origins;
        boolean feeding, paused, done, cancelled;
        int quietTicks;
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Id", id); tag.putString("Source", source); tag.putString("Key", key);
            tag.put("Origins", origins.save()); tag.putBoolean("Feeding", feeding);
            tag.putBoolean("Paused", paused); tag.putBoolean("Done", done); tag.putBoolean("Cancelled", cancelled);
            return tag;
        }
        static Session load(CompoundTag tag) {
            Session s = new Session();
            s.id = tag.getLongOr("Id", 0); s.source = tag.getStringOr("Source", "manual");
            s.key = tag.getStringOr("Key", ""); s.origins = PopulationScope.load(tag.getCompoundOrEmpty("Origins"));
            // An interrupted terrain producer cannot be assumed to resume after a restart.
            s.paused = tag.getBooleanOr("Paused", false) || tag.getBooleanOr("Feeding", false);
            s.done = tag.getBooleanOr("Done", false); s.cancelled = tag.getBooleanOr("Cancelled", false);
            return s;
        }
    }

    private BoundedPopulation() {}
    private BoundedPopulation(CompoundTag tag) {
        nextId = tag.getLongOr("NextId", 1);
        for (var entry : tag.getListOrEmpty("Sessions")) if (entry instanceof CompoundTag record) {
            Session s = Session.load(record); sessions.put(s.id, s); nextId = Math.max(nextId, s.id + 1);
        }
    }
    public static BoundedPopulation get(ServerLevel level) {
        return CACHE.computeIfAbsent(level, l -> l.getDataStorage().computeIfAbsent(TYPE));
    }
    public static boolean keepsServerActive(net.minecraft.server.MinecraftServer server) {
        return CACHE.entrySet().stream().anyMatch(entry -> entry.getKey().getServer() == server
                && entry.getValue().sessions.values().stream().anyMatch(s -> !s.done && (!s.paused || s.feeding)));
    }
    private CompoundTag save() {
        CompoundTag tag = new CompoundTag(); ListTag list = new ListTag();
        sessions.values().forEach(s -> list.add(s.save()));
        tag.put("Sessions", list); tag.putLong("NextId", nextId); return tag;
    }
    public void registerQueue(String name, Queue queue) { queues.putIfAbsent(name, queue); }

    /** Called on the server thread by integrations; reattaches a paused Chunky selection on continue. */
    public long begin(String source, String key, PopulationScope origins, boolean feeding) {
        for (Session s : sessions.values()) if (!s.done && s.paused && s.source.equals(source) && s.key.equals(key)) {
            s.paused = false; s.feeding = feeding; s.quietTicks = 0; setDirty(); return s.id;
        }
        // Keep a small status history; active/paused work is never evicted.
        while (sessions.values().stream().filter(s -> s.done).count() >= 16) {
            long oldest = sessions.values().stream().filter(s -> s.done).findFirst().orElseThrow().id;
            sessions.remove(oldest);
        }
        Session s = new Session(); s.id = nextId++; s.source = source; s.key = key;
        s.origins = origins; s.feeding = feeding; sessions.put(s.id, s); setDirty(); return s.id;
    }
    public void observe(long id, ChunkPos origin) {
        Session s = sessions.get(id);
        if (s != null && !s.done) { s.origins.observe(origin); s.quietTicks = 0; setDirty(); }
    }
    public void terrainFinished(long id) {
        Session s = sessions.get(id);
        if (s != null && !s.done) { s.feeding = false; s.quietTicks = 0; setDirty(); }
    }
    public void pause(long id) {
        Session s = sessions.get(id);
        if (s != null && !s.done) { s.paused = true; setDirty(); }
    }
    public void resume(long id) {
        Session s = sessions.get(id);
        if (s != null && !s.done) { s.paused = false; s.quietTicks = 0; setDirty(); }
    }
    public void cancel(long id) {
        Session s = sessions.get(id);
        if (s != null && !s.done) { s.done = true; s.cancelled = true; s.feeding = false; setDirty(); }
    }
    public boolean admits(ChunkPos origin) {
        return sessions.values().stream().anyMatch(s -> !s.done && !s.paused && s.origins.contains(origin));
    }
    public boolean finished(long id) { Session s = sessions.get(id); return s != null && s.done; }
    public long sessionId(String source, String key) {
        return sessions.values().stream().filter(s -> s.source.equals(source) && s.key.equals(key))
                .mapToLong(s -> s.id).max().orElse(0);
    }
    public long latestSession(String source) {
        return sessions.values().stream().filter(s -> s.source.equals(source)).mapToLong(s -> s.id).max().orElse(0);
    }
    public void pauseAll() { sessions.keySet().forEach(this::pause); }
    public void resumeAll() { sessions.keySet().forEach(this::resume); }
    public void cancelAll() { sessions.keySet().forEach(this::cancel); }
    public long pending(long id) {
        Session s = sessions.get(id); return s == null ? 0 : queues.values().stream().mapToLong(q -> q.pending(s.origins::contains)).sum();
    }
    public java.util.List<String> status() {
        var result = new java.util.ArrayList<String>();
        for (Session s : sessions.values()) result.add("#" + s.id + " " + s.source + " "
                + (s.cancelled ? "cancelled" : s.done ? "complete" : s.paused ? "paused" : s.feeding ? "terrain running" : "finishing structures")
                + ": " + s.origins.size() + " origin chunks, " + pending(s.id) + " pending jobs");
        return result;
    }
    private void tick(ServerLevel level) {
        if (++ticks % 20 != 0) return;
        for (Session s : sessions.values()) if (!s.done && !s.paused) {
            if (s.feeding || pending(s.id) != 0) { s.quietTicks = 0; continue; }
            // A quiet grace period covers queue handlers later in this tick and late chunk completions.
            if ((s.quietTicks += 20) >= 100) {
                s.done = true; setDirty();
                DragonAPI.LOGGER.info("{} bounded population #{} finished in {}", s.source, s.id, level.dimension().identifier());
            }
        }
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) { var data = CACHE.get(level); if (data != null) data.tick(level); }
    }
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) get(level);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) CACHE.remove(level);
    }
}
