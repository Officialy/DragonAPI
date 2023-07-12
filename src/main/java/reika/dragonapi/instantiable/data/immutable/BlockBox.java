package reika.dragonapi.instantiable.data.immutable;

import reika.dragonapi.interfaces.BlockCheck;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.Random;

public final class BlockBox {

    public final int minX;
    public final int minY;
    public final int minZ;
    public final int maxX;
    public final int maxY;
    public final int maxZ;

    private boolean isEmpty = false;

    public BlockBox(int min, int max) {
        this(min, min, min, max, max, max);
    }

    public BlockBox(int x0, int y0, int z0, int x1, int y1, int z1) {
        minX = Math.min(x0, x1);
        minY = Math.min(y0, y1);
        minZ = Math.min(z0, z1);

        maxX = Math.max(x0, x1);
        maxY = Math.max(y0, y1);
        maxZ = Math.max(z0, z1);
    }

    public BlockBox(WorldLocation loc, WorldLocation loc2) {
        this(loc.pos.getX(), loc.pos.getY(), loc.pos.getZ(), loc2.pos.getX(), loc2.pos.getY(), loc2.pos.getZ());
    }

    public static BlockBox infinity() {
        return new BlockBox(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static BlockBox nothing() {
        BlockBox ret = new BlockBox(0, 0);
        ret.isEmpty = true;
        return ret;
    }

    public static BlockBox origin() {
        return new BlockBox(-1, 1); //not 0,1
    }

    public static BlockBox block(int x, int y, int z) {
        return new BlockBox(x, y, z, x + 1, y + 1, z + 1);
    }

    public static BlockBox block(BlockEntity te) {
        return block(te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ());
    }

    public static BlockBox block(ChunkPos cc) {
        return block(cc.getWorldPosition().getX(), cc.getWorldPosition().getY(), cc.getWorldPosition().getZ());
    }

    public int getSizeX() {
        return maxX - minX;
    }

    public int getSizeY() {
        return maxY - minY;
    }

    public int getSizeZ() {
        return maxZ - minZ;
    }

    public int getVolume() {
        return this.getSizeX() * this.getSizeY() * this.getSizeZ();
    }

    public int getSurfaceArea() {
        return 2 * this.getSizeX() + 2 * this.getSizeY() + 2 * this.getSizeZ();
    }

    public BlockBox expand(int amt) {
        return this.expand(amt, amt, amt);
    }

    public BlockBox expand(Direction dir, int amt) {
        int minx = minX;
        int miny = minY;
        int minz = minZ;
        int maxx = maxX;
        int maxy = maxY;
        int maxz = maxZ;
        switch (dir) {
            case EAST:
                maxx += amt;
                break;
            case WEST:
                minx -= amt;
                break;
            case NORTH:
                minz -= amt;
                break;
            case SOUTH:
                maxz += amt;
                break;
            case UP:
                maxy += amt;
                break;
            case DOWN:
                miny -= amt;
                break;
            default:
                break;
        }
        return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public BlockBox expandScale(double sx, double sy, double sz) {
        int midX = (minX + maxX) / 2;
        int midY = (minY + maxY) / 2;
        int midZ = (minZ + maxZ) / 2;
        int nx = Mth.floor(midX - sx * (midX - minX));
        int px = Mth.ceil(midX + sx * (maxX - midX));
        int ny = Mth.floor(midY - sy * (midY - minY));
        int py = Mth.ceil(midY + sy * (maxY - midY));
        int nz = Mth.floor(midZ - sz * (midZ - minZ));
        int pz = Mth.ceil(midZ + sz * (maxZ - midZ));
        return new BlockBox(nx, ny, nz, px, py, pz);
    }

    public BlockBox expand(int dx, int dy, int dz) {
        return new BlockBox(minX - dx, minY - dy, minZ - dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public BlockBox shift(Direction dir, int dist) {
        return this.shift(dist * dir.getStepX(), dist * dir.getStepY(), dist * dir.getStepZ());
    }

    public BlockBox shift(int dx, int dy, int dz) {
        return new BlockBox(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public BlockBox contract(Direction dir, int amt) {
        int minx = minX;
        int miny = minY;
        int minz = minZ;
        int maxx = maxX;
        int maxy = maxY;
        int maxz = maxZ;
        switch (dir) {
            case EAST:
                maxx -= amt;
                break;
            case WEST:
                minx += amt;
                break;
            case NORTH:
                minz += amt;
                break;
            case SOUTH:
                maxz -= amt;
                break;
            case UP:
                maxy -= amt;
                break;
            case DOWN:
                miny += amt;
                break;
            default:
                break;
        }
        return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public BlockBox contract(int dx, int dy, int dz) {
        return new BlockBox(minX + dx, minY + dy, minZ + dz, maxX - dx, maxY - dy, maxZ - dz);
    }

    public BlockBox clamp(Direction side, int value) {
        int minx = minX;
        int miny = minY;
        int minz = minZ;
        int maxx = maxX;
        int maxy = maxY;
        int maxz = maxX;
        switch (side) {
            case DOWN:
                miny = Math.max(value, miny);
                break;
            case UP:
                maxy = Math.min(value, maxy);
                break;
            case EAST:
                maxx = Math.min(value, maxx);
                break;
            case WEST:
                minx = Math.max(value, minx);
                break;
            case NORTH:
                minz = Math.max(value, minz);
                break;
            case SOUTH:
                maxz = Math.min(value, maxz);
                break;
            default:
                break;
        }
        return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public BlockBox clamp(Direction side, int x0, int y0, int z0, int dist) {
        int minx = minX;
        int miny = minY;
        int minz = minZ;
        int maxx = maxX;
        int maxy = maxY;
        int maxz = maxX;
        switch (side) {
            case DOWN:
                miny = Math.max(y0 - dist, miny);
                break;
            case UP:
                maxy = Math.min(y0 + 1 + dist, maxy);
                break;
            case EAST:
                maxx = Math.min(x0 + 1 + dist, maxx);
                break;
            case WEST:
                minx = Math.max(x0 - dist, minx);
                break;
            case NORTH:
                minz = Math.max(z0 - dist, minz);
                break;
            case SOUTH:
                maxz = Math.min(z0 + 1 + dist, maxz);
                break;
            default:
                break;
        }
        return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public BlockBox clampTo(BlockBox box) {
        int minX = Math.max(this.minX, box.minX);
        int minY = Math.max(this.minY, box.minY);
        int minZ = Math.max(this.minZ, box.minZ);
        int maxX = Math.min(this.maxX, box.maxX);
        int maxY = Math.min(this.maxY, box.maxY);
        int maxZ = Math.min(this.maxZ, box.maxZ);
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public BlockBox combineWith(BlockBox box) {
        int minX = Math.min(this.minX, box.minX);
        int minY = Math.min(this.minY, box.minY);
        int minZ = Math.min(this.minZ, box.minZ);
        int maxX = Math.max(this.maxX, box.maxX);
        int maxY = Math.max(this.maxY, box.maxY);
        int maxZ = Math.max(this.maxZ, box.maxZ);
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public BlockBox addCoordinate(int x, int y, int z) {
        if (isEmpty) {
            return block(x, y, z);
        }
        int minX = Math.min(this.minX, x);
        int minY = Math.min(this.minY, y);
        int minZ = Math.min(this.minZ, z);
        int maxX = Math.max(this.maxX, x + 1);
        int maxY = Math.max(this.maxY, y + 1);
        int maxZ = Math.max(this.maxZ, z + 1);
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean isBlockInside(BlockPos c) {
        return this.isBlockInside(c.getX(), c.getY(), c.getZ());
    }

    public boolean isBlockInside(int x, int y, int z) {
        boolean ix = ReikaMathLibrary.isValueInsideBoundsIncl(minX, maxX, x);
        boolean iy = ReikaMathLibrary.isValueInsideBoundsIncl(minY, maxY, y);
        boolean iz = ReikaMathLibrary.isValueInsideBoundsIncl(minZ, maxZ, z);
        return ix && iy && iz;
    }

    public boolean isBlockInsideExclusive(BlockPos c) {
        return this.isBlockInsideExclusive(c.getX(), c.getY(), c.getZ());
    }

    public boolean isBlockInsideExclusive(int x, int y, int z) {
        boolean ix = ReikaMathLibrary.isValueInsideBoundsIncl(minX, maxX - 1, x);
        boolean iy = ReikaMathLibrary.isValueInsideBoundsIncl(minY, maxY - 1, y);
        boolean iz = ReikaMathLibrary.isValueInsideBoundsIncl(minZ, maxZ - 1, z);
        return ix && iy && iz;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BlockBox b) {
            return b.maxX == maxX && b.maxY == maxY && b.maxZ == maxZ && b.minX == minX && b.minY == minY && b.minZ == minZ;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return minX + maxX + minY + maxY + minZ + maxZ;
    }

    @Override
    public String toString() {
        return String.format("%d, %d, %d >> %d, %d, %d", minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB asAABB() {
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static BlockBox load(CompoundTag tag) {
        int minx = tag.getInt("minx");
        int miny = tag.getInt("miny");
        int minz = tag.getInt("minz");
        int maxx = tag.getInt("maxx");
        int maxy = tag.getInt("maxy");
        int maxz = tag.getInt("maxz");
        return new BlockBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putInt("minx", minX);
        tag.putInt("miny", minY);
        tag.putInt("minz", minZ);
        tag.putInt("maxx", maxX);
        tag.putInt("maxy", maxY);
        tag.putInt("maxz", maxZ);
    }

    public BlockBox offset(BlockPos offset) {
        return this.offset(offset.getX(), offset.getY(), offset.getZ());
    }

    public BlockBox offset(int x, int y, int z) {
        return new BlockBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public BlockPos getRandomContainedCoordinate(Random rand) {
        return new BlockPos(minX + rand.nextInt(maxX - minX + 1), minY + rand.nextInt(maxY - minY + 1), minZ + rand.nextInt(maxZ - minZ + 1));
    }

    public BlockPos findBlock(Level world, BlockCheck bc) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    //ReikaJavaLibrary.pConsole(new BlockPos(x, y, z));
                    if (bc.matchInWorld(world, new BlockPos(x, y, z)))
                        return new BlockPos(x, y, z);
                }
            }
        }
        return null;
    }

    public int getLongestEdge() {
        return ReikaMathLibrary.multiMax(this.getSizeX(), this.getSizeY(), this.getSizeZ());
    }

    public int getCenterX() {
        return (maxX - minX) / 2 + minX;
    }

    public int getCenterY() {
        return (maxY - minY) / 2 + minY;
    }

    public int getCenterZ() {
        return (maxZ - minZ) / 2 + minZ;
    }

    public BlockPos getFarthestPointFrom(int x, int y, int z) {
        if (this.isBlockInside(x, y, z)) {
            int dxn = x - minX;
            int dxp = maxX - x;
            int dyn = y - minY;
            int dyp = maxY - y;
            int dzn = z - minZ;
            int dzp = maxZ - z;
            boolean negX = Math.abs(dxn) > Math.abs(dxp);
            boolean negY = Math.abs(dyn) > Math.abs(dyp);
            boolean negZ = Math.abs(dzn) > Math.abs(dzp);
            int rx = negX ? x - dxn : x + dxp;
            int ry = negY ? y - dyn : y + dyp;
            int rz = negZ ? z - dzn : z + dzp;
            return new BlockPos(rx, ry, rz);
        } else {
            int rx = x < minX ? maxX : minX;
            int ry = y < minY ? maxY : minY;
            int rz = z < minZ ? maxZ : minZ;
            return new BlockPos(rx, ry, rz);
        }
    }

    public static BlockBox between(Entity e1, Entity e2) {
        return new BlockBox(Mth.floor(e1.getX()), Mth.floor(e1.getY()), Mth.floor(e1.getZ()), Mth.floor(e2.getX()), Mth.floor(e2.getY()), Mth.floor(e2.getZ()));
    }

    public static BlockBox between(DecimalPosition e1, DecimalPosition e2) {
        return new BlockBox(Mth.floor(e1.xCoord), Mth.floor(e1.yCoord), Mth.floor(e1.zCoord), Mth.floor(e2.xCoord), Mth.floor(e2.yCoord), Mth.floor(e2.zCoord));
    }

    private static BlockBox between(BlockPos c1, BlockPos c2) {
        return new BlockBox(c1.getX(), c1.getY(), c1.getZ(), c2.getX(), c2.getY(), c2.getZ());
    }

    private static BlockBox between(WorldLocation c1, WorldLocation c2) {
        return new BlockBox(c1.pos.getX(), c1.pos.getY(), c1.pos.getZ(), c2.pos.getX(), c2.pos.getY(), c2.pos.getZ());
    }
}
