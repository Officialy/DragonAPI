package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.interfaces.Location;
import reika.dragonapi.libraries.io.NBTCompat;

import java.util.Comparator;

/**
 * An immutable integer block position (Reika's world-less coordinate), ported to 26.2. Core surface:
 * position + offset/scale, block/BlockEntity access, distances, equality, NBT round-trip, and the
 * {@link Location} contract.
 *
 * <p>Port note (deferred — unported helpers / niche uses): the worldgen constructors + placement
 * helpers ({@code ChunkSplicedGenerationCache}), fluid checks ({@code ReikaFluidHelper}), block-tick
 * scheduling ({@code BlockTickEvent}), the sphere/line generators, ByteBuf (de)serialisation, the
 * {@code NBTIO} handler, and the client {@code WorldRenderer} constructor. The block-metadata accessor
 * is dropped (26.2 is BlockState-based — use {@link #getBlockState}).
 */
public record Coordinate(int xCoord, int yCoord, int zCoord) implements Location, Comparable<Coordinate> {

    public Coordinate(double x, double y, double z) {
        this(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    public Coordinate(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public Coordinate(BlockEntity te) {
        this(te.getBlockPos());
    }

    public Coordinate(Entity e) {
        this(Mth.floor(e.getX()), Mth.floor(e.getY()), Mth.floor(e.getZ()));
    }

    public Coordinate(WorldLocation loc) {
        this(loc.pos);
    }

    public Coordinate(Vec3 vec) {
        this(vec.x, vec.y, vec.z);
    }

    public Coordinate(DecimalPosition vec) {
        this(vec.xCoord(), vec.yCoord(), vec.zCoord());
    }

    public int getX() {
        return xCoord;
    }

    public int getY() {
        return yCoord;
    }

    public int getZ() {
        return zCoord;
    }

    public BlockPos asBlockPos() {
        return new BlockPos(xCoord, yCoord, zCoord);
    }

    public Coordinate offset(int dx, int dy, int dz) {
        return new Coordinate(xCoord + dx, yCoord + dy, zCoord + dz);
    }

    public Coordinate offset(Direction dir, int dist) {
        return this.offset(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist);
    }

    public Coordinate offset(Coordinate c) {
        return this.offset(c.xCoord, c.yCoord, c.zCoord);
    }

    public Coordinate setX(int x) {
        return new Coordinate(x, yCoord, zCoord);
    }

    public Coordinate setY(int y) {
        return new Coordinate(xCoord, y, zCoord);
    }

    public Coordinate setZ(int z) {
        return new Coordinate(xCoord, yCoord, z);
    }

    public Coordinate scale(double s) {
        return this.scale(s, s, s);
    }

    public Coordinate scale(double x, double y, double z) {
        return new Coordinate(xCoord * x, yCoord * y, zCoord * z);
    }

    // ---- block / world access ----

    @Override
    public Block getBlock(BlockGetter world) {
        return world != null ? world.getBlockState(this.asBlockPos()).getBlock() : null;
    }

    public BlockState getBlockState(BlockGetter world) {
        return world != null ? world.getBlockState(this.asBlockPos()) : null;
    }

    @Override
    public BlockEntity getBlockEntity(BlockGetter world) {
        return world != null ? world.getBlockEntity(this.asBlockPos()) : null;
    }

    /**
     * Alias kept for 1.7.10 call-site parity.
     */
    public BlockEntity getTileEntity(BlockGetter world) {
        return this.getBlockEntity(world);
    }

    public boolean isEmpty(BlockGetter world) {
        return this.getBlockState(world).isAir();
    }

    public BlockKey getBlockKey(BlockGetter world) {
        return new BlockKey(this.getBlockState(world));
    }

    public void setBlock(Level world, BlockState state) {
        world.setBlock(this.asBlockPos(), state, 3);
    }

    public void setBlock(Level world, Block b) {
        this.setBlock(world, b.defaultBlockState());
    }

    // ---- distances ----

    @Override
    public double getDistanceTo(double x, double y, double z) {
        double dx = xCoord + 0.5 - x;
        double dy = yCoord + 0.5 - y;
        double dz = zCoord + 0.5 - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double getDistanceTo(Coordinate src) {
        return this.getDistanceTo(src.xCoord + 0.5, src.yCoord + 0.5, src.zCoord + 0.5);
    }

    public double getDistanceTo(Entity e) {
        return this.getDistanceTo(e.getX(), e.getY(), e.getZ());
    }

    public int getTaxicabDistanceTo(Coordinate c) {
        return this.getTaxicabDistanceTo(c.xCoord, c.yCoord, c.zCoord);
    }

    public int getTaxicabDistanceTo(double x, double y, double z) {
        return (int) (Math.abs(xCoord - x) + Math.abs(yCoord - y) + Math.abs(zCoord - z));
    }

    public boolean isWithinSquare(Coordinate c, int d) {
        return this.isWithinSquare(c, d, d, d);
    }

    public boolean isWithinSquare(Coordinate c, int dx, int dy, int dz) {
        return Math.abs(xCoord - c.xCoord) <= dx && Math.abs(yCoord - c.yCoord) <= dy && Math.abs(zCoord - c.zCoord) <= dz;
    }

    public int[] toArray() {
        return new int[]{xCoord, yCoord, zCoord};
    }

    // ---- NBT ----

    @Override
    public void writeToTag(CompoundTag data) {
        data.putInt("x", xCoord);
        data.putInt("y", yCoord);
        data.putInt("z", zCoord);
    }

    @Override
    public CompoundTag writeToTag() {
        CompoundTag data = new CompoundTag();
        this.writeToTag(data);
        return data;
    }

    @Override
    public void saveAdditional(String tag, CompoundTag NBT) {
        NBT.put(tag, this.writeToTag());
    }

    /**
     * Alias kept for 1.7.10 call-site parity.
     */
    public void writeToNBT(String tag, CompoundTag NBT) {
        this.saveAdditional(tag, NBT);
    }

    public static Coordinate readFromNBT(String tag, CompoundTag NBT) {
        return readTag(NBTCompat.getCompound(NBT, tag));
    }

    public static Coordinate readTag(CompoundTag data) {
        return new Coordinate(NBTCompat.getInt(data, "x", 0), NBTCompat.getInt(data, "y", 0), NBTCompat.getInt(data, "z", 0));
    }

    @Override
    public HitResult asMovingPosition(Direction s, Vec3 vec) {
        return new BlockHitResult(vec, s, this.asBlockPos(), false);
    }

    // ---- identity ----

    @Override
    public String toString() {
        return xCoord + ", " + yCoord + ", " + zCoord;
    }

    /**
     * V33a's hash. Seeded patterns (the Ender Forest colour cells, the Rainbow Forest's Voronoi tree
     * colours via {@link DecimalPosition#hashCode}) feed this straight into {@code new Random(...)},
     * so any other formula changes what generates where.
     */
    public static int coordHash(int x, int y, int z) {
        //return xCoord + (zCoord << 8) + (yCoord << 16);
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }

    @Override
    public int hashCode() {
        return coordHash(xCoord, yCoord, zCoord);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Coordinate w && this.equals(w.xCoord, w.yCoord, w.zCoord);
    }

    public boolean equals(BlockEntity te) {
        BlockPos p = te.getBlockPos();
        return this.equals(p.getX(), p.getY(), p.getZ());
    }

    public boolean equals(int x, int y, int z) {
        return xCoord == x && yCoord == y && zCoord == z;
    }

    @Override
    public int compareTo(Coordinate o) {
        int result = Integer.compare(xCoord, o.xCoord);
        if (result == 0) result = Integer.compare(yCoord, o.yCoord);
        if (result == 0) result = Integer.compare(zCoord, o.zCoord);
        return result;
    }

    public record DistanceComparator(Coordinate target, boolean taxicab) implements Comparator<Coordinate> {

        @Override
        public int compare(Coordinate o1, Coordinate o2) {
            return taxicab ? Integer.compare(o1.getTaxicabDistanceTo(target), o2.getTaxicabDistanceTo(target))
                    : Double.compare(o1.getDistanceTo(target), o2.getDistanceTo(target));
        }
    }
}
