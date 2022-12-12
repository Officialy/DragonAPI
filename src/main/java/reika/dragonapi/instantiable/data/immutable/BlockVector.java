package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockVector {

    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    public final Direction direction;

    public BlockVector(Direction dir, BlockPos c) {
        this(dir, c.getX(), c.getY(), c.getZ());
    }

    public BlockVector(Direction dir, BlockEntity c) {
        this(dir, c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ());
    }

    public BlockVector(Direction dir, WorldLocation c) {
        this(dir, c.pos.getX(), c.pos.getY(), c.pos.getZ());
    }

    public BlockVector(Direction dir, int x, int y, int z) {
        this(x, y, z, dir);
    }

    public BlockVector(int x, int y, int z, Direction dir) {
        xCoord = x;
        yCoord = y;
        zCoord = z;
        direction = dir;
    }

    public static BlockVector load(CompoundTag tag) {
        int x = tag.getInt("x");
        int y = tag.getInt("y");
        int z = tag.getInt("z");
        Direction dir = Direction.values()[tag.getInt("dir")];
        return new BlockVector(x, y, z, dir);
    }

    @Override
    public String toString() {
        return xCoord + ", " + yCoord + ", " + zCoord + " > " + direction;
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putInt("x", xCoord);
        tag.putInt("y", yCoord);
        tag.putInt("z", zCoord);
        tag.putInt("dir", direction.ordinal());
    }

    public BlockPos getCoord() {
        return new BlockPos(xCoord, yCoord, zCoord);
    }

}
