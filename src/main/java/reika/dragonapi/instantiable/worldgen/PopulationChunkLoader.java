package reika.dragonapi.instantiable.worldgen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import reika.dragonapi.DragonAPI;

/** Explicit asynchronous FULL scheduling; a loading ticket alone does not request FULL tasks. */
public final class PopulationChunkLoader {
    private ChunkPos center;
    private int radius, refreshed, retryAt;
    private CompletableFuture<?> loading;

    public boolean ready(ServerLevel level, TicketType ticket, ChunkPos origin, int range) {
        int tick = level.getServer().getTickCount();
        if (!origin.equals(center) || radius != range) {
            release(level, ticket);
            center = origin; radius = range; refreshed = tick;
            loading = level.getChunkSource().addTicketAndLoadWithRadius(ticket, center, radius);
            retryAt = tick + 20;
        } else if (tick - refreshed >= 20) {
            level.getChunkSource().addTicketWithRadius(ticket, center, radius); refreshed = tick;
        }
        if (!loading.isDone()) return false;
        if (!loading.isCompletedExceptionally() && loading.getNow(null) instanceof ChunkResult<?> result && result.isSuccess()) return true;
        if (tick >= retryAt) {
            DragonAPI.LOGGER.warn("Retrying FULL population footprint at {} in {} after a failed chunk load", center, level.dimension().identifier());
            loading = level.getChunkSource().addTicketAndLoadWithRadius(ticket, center, radius);
            retryAt = tick + 20;
        }
        return false;
    }

    public void release(ServerLevel level, TicketType ticket) {
        if (center != null) level.getChunkSource().removeTicketWithRadius(ticket, center, radius);
        center = null; loading = null;
    }
}
