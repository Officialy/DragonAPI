package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import reika.dragonapi.interfaces.Location;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.Objects;

public class WorldLocation implements Location, Comparable<WorldLocation> {

    public final BlockPos pos = BlockPos.ZERO;

    private boolean isRemote = false;

    private Level clientWorld;

    private final ResourceKey<Level> dimension = Minecraft.getInstance().level.dimension();

    public WorldLocation(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            isRemote = true;
            clientWorld = level;
        }
    }

    private WorldLocation(WorldLocation loc) {
        this(loc.getWorld(), loc.pos.getX(), loc.pos.getY(), loc.pos.getZ());
    }

    public WorldLocation(BlockEntity te) {
        this(Objects.requireNonNull(te.getLevel()), te.getBlockPos());
    }

    public WorldLocation(Entity e) {
        this(e.level(), e.blockPosition());
    }

    public WorldLocation(Level world, HitResult hit) {
        this(world, hit.getLocation().x(), hit.getLocation().y(), hit.getLocation().z());
    }

    public WorldLocation(Level world, Vec3 vec) {
        this(world, vec.x(), vec.y(), vec.z());
    }

    public WorldLocation(Level world, DecimalPosition d) {
        this(world, Mth.floor(d.xCoord), Mth.floor(d.yCoord), Mth.floor(d.zCoord));
    }

    public WorldLocation(Level world, double x, double y, double z) {
        this(world, new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    public WorldLocation(ResourceKey<Level> world, double x, double y, double z) {
        this(
                ServerLifecycleHooks.getCurrentServer().getLevel(world),
                new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    public static int coordHash(BlockPos pos) {
        // return pos.getX() + (pos.getZ() << 8) + (pos.getY() << 16);
        final int prime = 31;
        int result = 1;
        result = prime * result + pos.getX();
        result = prime * result + pos.getY();
        result = prime * result + pos.getZ();
        return result;
    }

    public WorldChunk getChunk() {
        return new WorldChunk(getWorld(), pos.getX() >> 4, pos.getZ() >> 4);
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public Block getBlock() {
        Level world = this.getWorld();
        return world != null
                ? world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getBlock()
                : null;
    }

    public void setBlock(Block b) {
        this.setBlock(b.defaultBlockState());
    }

    public void setBlock(BlockState id) {
        Level world = this.getWorld();
        if (world != null) {
            world.setBlock(new BlockPos(pos.getX(), pos.getY(), pos.getZ()), id, 3);
        }
    }

    public Block getBlock(BlockGetter world) {
        return world != null
                ? world.getBlockState(
                                new BlockPos(new BlockPos(pos.getX(), pos.getY(), pos.getZ())))
                        .getBlock()
                : null;
    }

    public boolean isEmpty() {
        return this.getBlock() == Blocks.AIR;
    }

    public BlockEntity getBlockEntity() {
        return this.getBlockEntity(null);
    }

    public BlockEntity getBlockEntity(BlockGetter call) {
        BlockGetter world = call != null ? call : this.getWorld();
        return world != null
                ? world.getBlockEntity(new BlockPos(pos.getX(), pos.getY(), pos.getZ()))
                : null;
    }

    public int getRedstone() {
        Level world = this.getWorld();
        return world != null
                ? world.getBestNeighborSignal(new BlockPos(pos.getX(), pos.getY(), pos.getZ()))
                : 0;
    }

    public int getRedstoneOnSide(Direction dir) {
        Direction opp = dir.getOpposite();
        Level world = this.getWorld();
        return world != null
                ? world.getDirectSignal(
                        new BlockPos(
                                pos.getX() + opp.getStepX(),
                                pos.getY() + opp.getStepY(),
                                pos.getZ() + opp.getStepZ()),
                        dir)
                : 0;
    }

    public boolean isRedstonePowered() {
        Level world = this.getWorld();
        return world != null
                && world.hasNeighborSignal(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public void triggerBlockUpdate(boolean adjacent) {
        Level world = this.getWorld();
        if (world != null) {
            world.blockUpdated(
                    new BlockPos(pos.getX(), pos.getY(), pos.getZ()),
                    world.getBlockState(pos).getBlock());
            if (adjacent) {
                ReikaWorldHelper.causeAdjacentUpdates(
                        world, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
    }

    public void dropItem(ItemStack is) {
        this.dropItem(is, 1);
    }

    public void dropItem(ItemStack is, double vscale) {
        Level world = this.getWorld();
        if (world != null && !world.isClientSide()) {
            world.addFreshEntity(
                    is
                            .getEntityRepresentation()); // .dropItem(this.getWorld(),
                                                         // pos.getX()+rand.nextDouble(),
                                                         // pos.getY()+rand.nextDouble(),
                                                         // pos.getZ()+rand.nextDouble(), is,
                                                         // vscale); todo fix item dropping
        }
    }

    public WorldLocation move(int dx, int dy, int dz) {
        return new WorldLocation(getWorld(), pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
    }

    public WorldLocation move(Direction dir, int dist) {
        return this.move(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist);
    }

    public WorldLocation changeWorld(Level world) {
        return new WorldLocation(world, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public Level getWorld() {
        if (isRemote) {
            this.initClientWorld();
            return clientWorld;
        }
        assert Minecraft.getInstance().level != null;
        return Minecraft.getInstance().level;
    }

    private void initClientWorld() {
        if (clientWorld == null) clientWorld = Minecraft.getInstance().level;
    }

    public void saveAdditional(String tag, CompoundTag NBT) {
        CompoundTag data = new CompoundTag();
        this.writeToTag(data);
        NBT.put(tag, data);
    }

    public void writeToTag(CompoundTag data) {
        data.putString(
                "dim",
                getWorld().dimension().location().toString()); // .getRegistryName().toString());
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());
    }

    public static WorldLocation readTag(CompoundTag data) {
        int x = data.getInt("x");
        int y = data.getInt("y");
        int z = data.getInt("z");
        String dim = data.getString("dim");
        return new WorldLocation(
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dim)),
                x,
                y,
                z); // todo fix null dimension / level
    }

    public static WorldLocation load(String tag, CompoundTag NBT) {
        if (!NBT.contains(tag)) return null;
        CompoundTag data = NBT.getCompound(tag);
        if (data != null) {
            return readTag(data);
        }
        return null;
    }

    public CompoundTag writeToTag() {
        CompoundTag data = new CompoundTag();
        data.putString("dim", dimension.location().getNamespace());
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());
        return data;
    }

    public WorldLocation copy() {
        return new WorldLocation(this);
    }

    @Override
    public String toString() {
        return pos.getX()
                + ", "
                + pos.getY()
                + ", "
                + pos.getZ()
                + " in DIM"
                + getChunk().dimensionID;
    }

    @Override
    public int hashCode() {
        return coordHash(
                new BlockPos(
                        pos.getX(),
                        pos.getY(),
                        pos
                                .getZ())); // pos.getX() + (pos.getZ() << 8) + (pos.getY() << 16) +
                                           // (dimensionID << 24);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WorldLocation w) {
            return this.equals(w.dimension, w.pos);
        }
        return false;
    }

    private boolean equals(ResourceKey<Level> dim, BlockPos pos) {
        return dim == dimension && this.pos == pos;
    }

    public boolean equals(Level world, BlockPos pos) {
        return this.equals(world.dimension(), pos);
    }

    public double getDistanceTo(WorldLocation src) {
        return this.getDistanceTo(src.pos.getX(), src.pos.getY(), src.pos.getZ());
    }

    public double getCylinderDistanceTo(double x, double z) {
        return ReikaMathLibrary.py3d(x - pos.getX(), 0, z - pos.getZ());
    }

    public double getDistanceTo(double x, double y, double z) {
        return ReikaMathLibrary.py3d(x - pos.getX(), y - pos.getY(), z - pos.getZ());
    }

    public double getDistanceTo(Entity e) {
        return this.getDistanceTo(e.getX(), e.getY(), e.getZ());
    }

    public double getCylinderDistanceTo(Entity e) {
        return this.getCylinderDistanceTo(e.getX(), e.getZ());
    }

    public int getTaxicabDistanceTo(int x, int y, int z) {
        return Math.abs(x - pos.getX()) + Math.abs(y - pos.getY()) + Math.abs(z - pos.getZ());
    }

    public int getTaxicabDistanceTo(BlockPos c) {
        return this.getTaxicabDistanceTo(c.getX(), c.getY(), c.getZ());
    }

    public boolean isWithinSquare(Level world, int x, int y, int z, int d) {
        return this.isWithinSquare(world, x, y, z, d, d, d);
    }

    private boolean isWithinSquare(Level dim, int x, int y, int z, int dx, int dy, int dz) {
        return Math.abs(x - pos.getX()) <= dx
                && Math.abs(y - pos.getY()) <= dy
                && Math.abs(z - pos.getZ()) <= dz;
    }

    public BlockKey getBlockKey() {
        return new BlockKey(this.getBlock().defaultBlockState());
    }

    public BlockKey getBlockKey(Level world) {
        return new BlockKey(this.getBlock(world).defaultBlockState());
    }

    public boolean isWithinDistOnAllCoords(WorldLocation loc, int radius) {
        return Math.abs(loc.pos.getX() - pos.getX()) <= radius
                && Math.abs(loc.pos.getY() - pos.getY()) <= radius
                && Math.abs(loc.pos.getZ() - pos.getZ()) <= radius;
    }

    public double getSquaredDistance(double x, double y, double z) {
        double dx = x - pos.getX();
        double dy = y - pos.getY();
        double dz = z - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean canSeeTheSky() {
        return this.getWorld().canSeeSky(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()));
    }

    public WorldLocation to2D() {
        return this.move(0, -pos.getY(), 0);
    }

    public AABB asAABB() {
        return ReikaAABBHelper.getBlockAABB(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    //    public boolean isChunkLoaded() {
    //        return ReikaWorldHelper.isWorldLoaded(dimensionID) &&
    // this.getWorld().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    //    }

    @Override
    public int compareTo(WorldLocation o) {
        int ret = Integer.compare(this.hashCode(), o.hashCode());
        if (ret != 0) return ret;
        ret = Integer.compare(pos.getX(), o.pos.getX());
        if (ret != 0) return ret;
        ret = Integer.compare(pos.getY(), o.pos.getY());
        if (ret != 0) return ret;
        return Integer.compare(pos.getZ(), o.pos.getZ());
    }

    public DoubleWorldLocation decimalOffset(double dx, double dy, double dz) {
        return new DoubleWorldLocation(this, dx, dy, dz);
    }

    public HitResult asMovingPosition(Direction s, Vec3 vec) {
        // return new HitResult(new BlockPos(pos.getX(), pos.getY(), pos.getZ()), s, vec);
        return new BlockHitResult(vec, s, pos, false);
    }

    public String toSerialString() {
        return String.format("%d:" + dimension.location())
                + String.format("%d:%d:%d", pos.getX(), pos.getY(), pos.getZ());
    }

    public static WorldLocation fromSerialString(String s) {
        String[] parts = s.split(":");
        return new WorldLocation(
                ResourceKey.create(
                        Registries.DIMENSION, ResourceLocation.tryParse(String.valueOf(parts[0]))),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }

    public static final class DoubleWorldLocation extends WorldLocation {

        public final double offsetX;
        public final double offsetY;
        public final double offsetZ;

        private DoubleWorldLocation(WorldLocation loc, double dx, double dy, double dz) {
            super(loc);
            offsetX = dx;
            offsetY = dy;
            offsetZ = dz;
        }
    }
}
