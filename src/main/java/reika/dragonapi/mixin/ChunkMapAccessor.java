package reika.dragonapi.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes whether a chunk has been fully generated to disk, for {@code ReikaWorldHelper.isChunkGenerated}. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    @Invoker("isExistingChunkFull")
    boolean dragonapi$isExistingChunkFull(ChunkPos pos);

}
