package reika.dragonapi.auxiliary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;
import reika.dragonapi.interfaces.entity.ChunkLoadingEntity;

/**
 * NeoForge 26.2 implementation of DragonAPI's block-entity chunk-ticket manager.
 *
 * <p>Tickets are persistent and owned by the loading block position. On world load the controller
 * validates the owner is still a {@link ChunkLoadingTile} and trims the restored ticket set to the
 * chunks the tile currently requests. Live updates likewise add and remove only the delta, preserving
 * the exact V33a manager contract rather than accumulating stale forced chunks.
 */
public final class ChunkManager {

	public static final ChunkManager instance = new ChunkManager();

	private static final TicketController CONTROLLER = new TicketController(
			Identifier.fromNamespaceAndPath(DragonAPI.MODID, "block_entity_chunk_loading"),
			(level, helper) -> {
				for (Map.Entry<BlockPos, TicketSet> entry : helper.getBlockTickets().entrySet()) {
					BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
					if (!(blockEntity instanceof ChunkLoadingTile loadingTile)) {
						helper.removeAllTickets(entry.getKey());
						continue;
					}
					Set<Long> requested = new HashSet<>();
					for (ChunkPos chunk : loadingTile.getChunksToLoad())
						requested.add(chunk.pack());
					for (long packed : entry.getValue().normal()) {
						if (!requested.contains(packed))
							helper.removeTicket(entry.getKey(), packed, false);
					}
					for (long packed : entry.getValue().naturalSpawning())
						helper.removeTicket(entry.getKey(), packed, true);
				}
			});

	private final Map<WorldLocation, Set<ChunkPos>> liveTickets = new HashMap<>();
	private final Map<java.util.UUID, Set<ChunkPos>> entityTickets = new HashMap<>();

	private ChunkManager() {}

	public static void register(IEventBus modBus) {
		modBus.addListener(ChunkManager::registerController);
	}

	private static void registerController(RegisterTicketControllersEvent event) {
		event.register(CONTROLLER);
	}

	public synchronized void loadChunks(ChunkLoadingTile loadingTile) {
		if (!(loadingTile instanceof BlockEntity blockEntity) || !(blockEntity.getLevel() instanceof ServerLevel level))
			return;
		WorldLocation owner = new WorldLocation(blockEntity);
		Set<ChunkPos> requested = Set.copyOf(loadingTile.getChunksToLoad());
		Set<ChunkPos> previous = liveTickets.getOrDefault(owner, Set.of());
		for (ChunkPos chunk : previous) {
			if (!requested.contains(chunk))
				CONTROLLER.forceChunk(level, blockEntity.getBlockPos(), chunk.x(), chunk.z(), false, false);
		}
		for (ChunkPos chunk : requested) {
			if (!previous.contains(chunk))
				CONTROLLER.forceChunk(level, blockEntity.getBlockPos(), chunk.x(), chunk.z(), true, false);
		}
		liveTickets.put(owner, requested);
	}

	public synchronized void unloadChunks(BlockEntity blockEntity) {
		if (blockEntity.getLevel() != null)
			this.unloadChunks(new WorldLocation(blockEntity));
	}

	public synchronized void unloadChunks(Level level, BlockPos pos) {
		this.unloadChunks(new WorldLocation(level, pos));
	}

	public synchronized void unloadChunks(WorldLocation owner) {
		Set<ChunkPos> chunks = liveTickets.remove(owner);
		Level world = owner.getWorld();
		if (!(world instanceof ServerLevel level))
			return;
		if (chunks == null) {
			BlockEntity blockEntity = level.getBlockEntity(owner.pos);
			if (blockEntity instanceof ChunkLoadingTile loadingTile)
				chunks = Set.copyOf(loadingTile.getChunksToLoad());
		}
		if (chunks != null) {
			for (ChunkPos chunk : chunks)
				CONTROLLER.forceChunk(level, owner.pos, chunk.x(), chunk.z(), false, false);
		}
	}

	/**
	 * V33a {@code loadChunks(ChunkLoadingEntity)}: tickets owned by the entity itself. They persist with the world, and
	 * the entity re-requests (idempotently) when it next ticks; its {@link ChunkLoadingEntity#onDestroy} releases them.
	 */
	public synchronized <E extends Entity & ChunkLoadingEntity> void loadChunks(E entity) {
		if (!(entity.level() instanceof ServerLevel level))
			return;
		Set<ChunkPos> requested = Set.copyOf(entity.getChunksToLoad());
		Set<ChunkPos> previous = entityTickets.getOrDefault(entity.getUUID(), Set.of());
		for (ChunkPos chunk : previous) {
			if (!requested.contains(chunk))
				CONTROLLER.forceChunk(level, entity, chunk.x(), chunk.z(), false, false);
		}
		for (ChunkPos chunk : requested) {
			if (!previous.contains(chunk))
				CONTROLLER.forceChunk(level, entity, chunk.x(), chunk.z(), true, false);
		}
		entityTickets.put(entity.getUUID(), requested);
	}

	public synchronized <E extends Entity & ChunkLoadingEntity> void unloadChunks(E entity) {
		if (!(entity.level() instanceof ServerLevel level))
			return;
		Set<ChunkPos> chunks = entityTickets.remove(entity.getUUID());
		if (chunks == null)
			chunks = Set.copyOf(entity.getChunksToLoad());
		for (ChunkPos chunk : chunks)
			CONTROLLER.forceChunk(level, entity, chunk.x(), chunk.z(), false, false);
	}

	public synchronized boolean isLoaded(BlockEntity blockEntity) {
		return blockEntity.getLevel() != null && liveTickets.containsKey(new WorldLocation(blockEntity));
	}

	/** Range is in chunks, matching the original DragonAPI API. Coordinates are block coordinates. */
	public static Collection<ChunkPos> getChunkSquare(int blockX, int blockZ, int radius) {
		int chunkX = blockX >> 4;
		int chunkZ = blockZ >> 4;
		Collection<ChunkPos> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++)
				chunks.add(new ChunkPos(chunkX + dx, chunkZ + dz));
		}
		return chunks;
	}
}
