package reika.dragonapi.interfaces.entity;

import net.minecraft.world.level.ChunkPos;

import java.util.Collection;

public interface ChunkLoadingEntity {

	Collection<ChunkPos> getChunksToLoad();

	/**
	 * Override setDead and call this in it, to unload your chunks.
	 */
	void onDestroy();

}
