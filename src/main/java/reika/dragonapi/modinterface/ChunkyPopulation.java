package reika.dragonapi.modinterface;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.BoundedPopulation;
import reika.dragonapi.instantiable.worldgen.PopulationScope;

/** Optional Chunky bridge: no Chunky classes are linked when the mod is absent. */
public final class ChunkyPopulation {
    private ChunkyPopulation() {}

    public static final class Handle {
        private final ServerLevel level;
        private final Object shape;
        private final Method contains;
        private long id; // Accessed only by server-thread callbacks.
        private volatile boolean closed;
        private Handle(ServerLevel level, Object shape, Method contains, String selection) {
            this.level = level; this.shape = shape; this.contains = contains;
            level.getServer().execute(() -> id = BoundedPopulation.get(level).begin(
                    "chunky", selection, PopulationScope.observedChunks(), true));
        }
        public void observe(int chunkX, int chunkZ) {
            if (closed) return;
            try {
                // Chunky's progress also includes positions rejected by non-rectangular shapes.
                if (!(boolean) contains.invoke(shape, chunkX * 16d + 8, chunkZ * 16d + 8)) return;
                level.getServer().execute(() -> BoundedPopulation.get(level).observe(id, new ChunkPos(chunkX, chunkZ)));
            } catch (ReflectiveOperationException ex) {
                DragonAPI.LOGGER.error("Could not read Chunky's selection; pausing bounded population", ex);
                close(false, false);
            }
        }
        public void close(boolean complete, boolean cancelled) {
            if (closed) return;
            closed = true;
            level.getServer().execute(() -> {
                var population = BoundedPopulation.get(level);
                population.terrainFinished(id);
                if (!complete) {
                    if (cancelled) population.cancel(id); else population.pause(id);
                }
            });
        }
    }

    public static Handle open(Object task) {
        try {
            Object selection = invoke(task, "getSelection");
            Object world = invoke(selection, "world");
            if (!(invoke(world, "getWorld") instanceof ServerLevel level)) return null;
            Object shape = invoke(task, "getShape");
            Method contains = Class.forName("org.popcraft.chunky.shape.Shape", false, task.getClass().getClassLoader())
                    .getMethod("isBounding", double.class, double.class);
            String key = invoke(selection, "centerX") + "/" + invoke(selection, "centerZ") + "/"
                    + invoke(selection, "radiusX") + "/" + invoke(selection, "radiusZ") + "/" + invoke(selection, "shape");
            return new Handle(level, shape, contains, key);
        } catch (ReflectiveOperationException ex) {
            DragonAPI.LOGGER.error("Chunky population integration could not read this task; use /reikapregen for its area", ex);
            return null;
        }
    }

    public static void complete(Object task, Handle handle) {
        if (handle == null) return;
        try {
            boolean complete = (boolean) invoke(invoke(task, "getProgress"), "isComplete");
            // run() can return with loads still in flight. Only the final update ends admission;
            // explicit pause/cancel is handled by stop(), not by this incomplete progress value.
            if (complete) handle.close(true, false);
        } catch (ReflectiveOperationException ex) {
            DragonAPI.LOGGER.error("Could not read Chunky's task result; pausing bounded population", ex);
            handle.close(false, false);
        }
    }
    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }
}
