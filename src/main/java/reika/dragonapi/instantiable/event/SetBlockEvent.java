package reika.dragonapi.instantiable.event;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import reika.dragonapi.instantiable.event.base.WorldPositionEvent;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * Fired both before, and when a setBlock propagates and succeeds inside a chunk. This is fired both client and server side.
 * This is called hundreds of thousands of times all over the codebase, dozens of times a tick, so you need to be efficient.
 */
public abstract class SetBlockEvent extends WorldPositionEvent {

    /**
     * You can toggle this off briefly to bypass the events if you are doing a lot of block sets (eg large scale worldgen) and you do not care
     * about other interceptions failing. ENSURE IT IS BACK ON AFTERWARDS.
     */
    public static boolean eventEnabledPre = true;

    /**
     * You can toggle this off briefly to bypass the events if you are doing a lot of block sets (eg large scale worldgen) and you do not care
     * about other interceptions failing. ENSURE IT IS BACK ON AFTERWARDS.
     */
    public static boolean eventEnabledPost = true;

    /**
     * Is this being called as part of a chunk generation. Usually indicates irrelevance to stuff like trackers. Also usually indicates
     * being called thousands of times in rapid succession. As a result, you MUST be efficient in handling these ones.
     */
    public final boolean isWorldgen;
    private final LevelChunk chunk;

    public final ChunkPos chunkLocation;

    public SetBlockEvent(LevelChunk ch, int x, int y, int z) {
        super(ch.getLevel(), new BlockPos(ch.getPos().x * 16 + x, y, ch.getPos().z * 16 + z));
        chunk = ch;
        chunkLocation = new ChunkPos(ch.getPos().x, ch.getPos().z);
        isWorldgen = true;// todo !ReikaWorldHelper.isChunkPastCompletelyFinishedGenerating(world, ch.getPos().x, ch.getPos().z);
    }

    public static class Pre extends SetBlockEvent {

        public final Block currentBlock;
        public final Block newBlock;

        public Pre(LevelChunk ch, int x, int y, int z, Block b) {
            super(ch, x, y, z);

            currentBlock = ch.getBlockState(new BlockPos(x, y, z)).getBlock(); //USE CHUNK, NOT WORLD

            newBlock = b != null ? b : currentBlock; //is a call to setMeta //todo figure out what this did and possibly remove since meta bad
        }

        public static void fire(LevelChunk ch, int x, int y, int z, Block b) {
            if (eventEnabledPre)
                MinecraftForge.EVENT_BUS.post(new Pre(ch, x, y, z, b));
        }

    }

    public static class Post extends SetBlockEvent {

        public Post(LevelChunk ch, int x, int y, int z) {
            super(ch, x, y, z);
        }

        public static void fire(LevelChunk ch, int x, int y, int z) {
            if (eventEnabledPost)
                MinecraftForge.EVENT_BUS.post(new Post(ch, x, y, z));
        }

    }

}
