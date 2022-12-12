/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.blockentity;

import net.minecraft.world.level.ChunkPos;

import java.util.Collection;

/**
 * For TileEntities that load chunks. Only implement this on a BlockEntity!
 */
public interface ChunkLoadingTile extends BreakAction {

	//public void setTicket(Ticket t);

	Collection<ChunkPos> getChunksToLoad();

	//public boolean loadChunk(ChunkPos chip);

}
