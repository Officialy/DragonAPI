package reika.dragonapi.instantiable.event.base;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

public abstract class WorldPositionEvent extends PositionEventBase {

    public final Level world;

    public WorldPositionEvent(Level world, BlockPos pos) {
        super(world, pos);
        this.world = world;
    }

    public final Biome getBiome() {
        return /*isFakeWorld ? Biomes.OCEAN : */world.getBiomeManager().getBiome(new BlockPos(xCoord, 0, zCoord)).value();
    }

    public final void setBiome(Biome b) {
//        if (isFakeWorld)
//            return;
        ReikaWorldHelper.setBiomeForXZ(world, xCoord, zCoord, b);
    }

    public final boolean setBlock(Block b) {
        return world.setBlock(new BlockPos(xCoord, yCoord, zCoord), b.defaultBlockState(), 1);
    }

    public final boolean setBlock(Block b, int flags) {
//        if (isFakeWorld)
//            return false;
        return world.setBlock(new BlockPos(xCoord, yCoord, zCoord), b.defaultBlockState(), flags);
    }

    public final ResourceKey<Level> dimensionID() {
        return world != null ? world.dimension() : Level.OVERWORLD;
    }

    public final WorldLocation getLocation() {
        return new WorldLocation(world, xCoord, yCoord, zCoord);
    }

}