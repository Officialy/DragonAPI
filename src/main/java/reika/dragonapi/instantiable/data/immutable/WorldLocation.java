package reika.dragonapi.instantiable.data.immutable;

import net.neoforged.fml.loading.FMLEnvironment;
import reika.dragonapi.client.ClientEnvironment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import reika.dragonapi.interfaces.Location;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.Objects;

public class WorldLocation implements Location, Comparable<WorldLocation> {

    public final BlockPos pos;

    private boolean isRemote = false;

    private final ResourceKey<Level> dimension;

    public WorldLocation(Level level, BlockPos position) {
        this.pos = position.immutable();
        this.dimension = level.dimension();
        if (level.isClientSide()) {
            isRemote = true;
        }
    }

    private WorldLocation(WorldLocation loc) {
        this(loc, loc.pos);
    }

    private WorldLocation(WorldLocation loc, BlockPos position) {
        pos = position.immutable();
        dimension = loc.dimension;
        isRemote = loc.isRemote;
    }

    public WorldLocation(BlockEntity te) {
        this(Objects.requireNonNull(te.getLevel()), te.getBlockPos());
    }

    public WorldLocation(Entity e) {
        this(e.level(), e.blockPosition());
    }

    public WorldLocation(Level world, HitResult hit) {
        this(world, hit instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(hit.getLocation()));
    }

    public WorldLocation(Level world, Vec3 vec) {
        this(world, vec.x(), vec.y(), vec.z());
    }

    public WorldLocation(Level world, DecimalPosition d) {
        this(world, Mth.floor(d.xCoord()), Mth.floor(d.yCoord()), Mth.floor(d.zCoord()));
    }

    public WorldLocation(Level world, double x, double y, double z) {
        this(world, new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    public WorldLocation(ResourceKey<Level> world, double x, double y, double z) {
        this.pos = BlockPos.containing(x, y, z);
        this.dimension = world;
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
        return new WorldChunk(dimension, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public Block getBlock() {
        BlockState state = getBlockState(getWorld());
        return state != null ? state.getBlock() : null;
    }

    /** Loaded-state lookup; missing dimensions/chunks are unresolved, not air. */
    public BlockState getBlockState(Level context) {
        Level world = getWorld(context);
        return world != null && world.hasChunkAt(pos) ? world.getBlockState(pos) : null;
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
        BlockState state = world instanceof Level level ? getBlockState(level) : world != null ? world.getBlockState(pos) : null;
        return state != null ? state.getBlock() : null;
    }

    public boolean isEmpty() {
        return this.getBlock() == Blocks.AIR;
    }

    public BlockEntity getBlockEntity() {
        return getBlockEntity(getWorld());
    }

    public BlockEntity getBlockEntity(BlockGetter view) {
        if (view == null) return getBlockEntity();
        return view instanceof Level level ? getBlockEntity(level) : view.getBlockEntity(pos);
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
            world.updateNeighborsAt(pos, world.getBlockState(pos).getBlock());
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
            ReikaItemHelper.dropItem(world, pos.getX() + world.getRandom().nextDouble(),
                    pos.getY() + world.getRandom().nextDouble(), pos.getZ() + world.getRandom().nextDouble(), is, vscale);
        }
    }

    public WorldLocation move(int dx, int dy, int dz) {
        return new WorldLocation(this, pos.offset(dx, dy, dz));
    }

    public WorldLocation move(Direction dir, int dist) {
        return this.move(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist);
    }

    public WorldLocation changeWorld(Level world) {
        return new WorldLocation(world, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    /** Resolve using an authoritative logical-side context. A missing dimension never falls back to another world. */
    public Level getWorld(Level context) {
        if (context == null) return null;
        return context.isClientSide() ? (context.dimension().equals(dimension) ? context : null) : getWorld(context.getServer());
    }

    public Level getWorld(net.minecraft.server.MinecraftServer server) {
        return server == null ? null : server.getLevel(dimension);
    }

    /** Compatibility entry point for older callers. New code should supply its level or server. */
    public Level getWorld() {
        if (isRemote) {
            Level current = clientLevel();
            return current != null && current.dimension().equals(dimension) ? current : null;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) return getWorld(server);
        Level current = clientLevel();
        return current != null && current.dimension().equals(dimension) ? current : null;
    }

    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(Level context) {
        Level world = getWorld(context);
        return world != null && world.hasChunkAt(pos) ? world.getBlockEntity(pos) : null;
    }

    /** Naming ClientLevel here would make the dedicated server fail to load WorldLocation itself,
     *  which is on nearly every gameplay path; go through the client holder instead. */
    private static Level clientLevel() {
        return FMLEnvironment.getDist().isClient() ? ClientEnvironment.level() : null;
    }

    public void saveAdditional(String tag, CompoundTag NBT) {
        CompoundTag data = new CompoundTag();
        this.writeToTag(data);
        NBT.put(tag, data);
    }

    public void writeToTag(CompoundTag data) {
        data.putString("dim", dimension.identifier().toString());
        data.putInt("x", pos.getX());
        data.putInt("y", pos.getY());
        data.putInt("z", pos.getZ());
    }

    public static WorldLocation readTag(CompoundTag data) {
        int x = NBTCompat.getInt(data, "x", 0);
        int y = NBTCompat.getInt(data, "y", 0);
        int z = NBTCompat.getInt(data, "z", 0);
        ResourceKey<Level> dimension;
        if (data.get("dim") instanceof NumericTag) {
            dimension = WorldChunk.dimensionKeyFromLegacyId(NBTCompat.getInt(data, "dim", 0));
        } else {
            String name = NBTCompat.getString(data, "dim", "minecraft:overworld");
            Identifier id = Identifier.tryParse(name);
            if (id == null) throw new IllegalArgumentException("Invalid location dimension: " + name);
            dimension = ResourceKey.create(Registries.DIMENSION, id);
        }
        return new WorldLocation(dimension, x, y, z);
    }

    public static WorldLocation load(String tag, CompoundTag NBT) {
        if (!NBT.contains(tag)) return null;
        CompoundTag data = NBTCompat.getCompound(NBT, tag);
        if (data != null) {
            return readTag(data);
        }
        return null;
    }

    public CompoundTag writeToTag() {
        CompoundTag data = new CompoundTag();
        data.putString("dim", dimension.identifier().toString());
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
                + " in "
                + dimension.identifier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WorldLocation w) {
            return this.equals(w.dimension, w.pos);
        }
        return false;
    }

    private boolean equals(ResourceKey<Level> dim, BlockPos pos) {
        return dimension.equals(dim) && this.pos.equals(pos);
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
        return dim != null && dimension.equals(dim.dimension()) && Math.abs(x - pos.getX()) <= dx
                && Math.abs(y - pos.getY()) <= dy
                && Math.abs(z - pos.getZ()) <= dz;
    }

    public BlockKey getBlockKey() {
        BlockState state = getBlockState(getWorld());
        return state != null ? new BlockKey(state) : null;
    }

    public BlockKey getBlockKey(Level world) {
        BlockState state = getBlockState(world);
        return state != null ? new BlockKey(state) : null;
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
        return canSeeTheSky(getWorld());
    }

    public boolean canSeeTheSky(Level context) {
        Level world = getWorld(context);
        return world != null && world.hasChunkAt(pos) && world.canSeeSky(pos.above());
    }

    public boolean isChunkLoaded() {
        return isChunkLoaded(getWorld());
    }

    public boolean isChunkLoaded(Level context) {
        Level world = getWorld(context);
        return world != null && world.hasChunkAt(pos);
    }

    public WorldLocation to2D() {
        return this.move(0, -pos.getY(), 0);
    }

    public AABB asAABB() {
        return ReikaAABBHelper.getBlockAABB(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public int compareTo(WorldLocation o) {
        int ret = dimension.identifier().toString().compareTo(o.dimension.identifier().toString());
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
        return dimension.identifier() + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ();
    }

    public static WorldLocation fromSerialString(String value) {
        String[] parts = value.split(";");
        if (parts.length == 4) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(parts[0]));
            return new WorldLocation(dimension, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }

        // V33a used numeric dimension:x:y:z strings. Also accept namespace:path:x:y:z from
        // early 26.2 worktrees so no in-progress save data is stranded.
        parts = value.split(":");
        if (parts.length == 4) {
            ResourceKey<Level> dimension = WorldChunk.dimensionKeyFromLegacyId(Integer.parseInt(parts[0]));
            return new WorldLocation(dimension, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }
        if (parts.length == 5) {
            Identifier id = Identifier.parse(parts[0] + ":" + parts[1]);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
            return new WorldLocation(dimension, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        }
        throw new IllegalArgumentException("Invalid world-location string: " + value);
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
