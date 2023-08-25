package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.BlockArrayComputer;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.block.SemiTransparent;
import reika.dragonapi.libraries.ReikaDirectionHelper;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.*;

public class BlockArray implements Iterable<BlockPos> {

    protected static final Random rand = new Random();
    private static final int DEPTH_LIMIT = getMaxDepth();
    private static final Comparator<BlockPos> heightComparator = new HeightComparator(false);
    private static final Comparator<BlockPos> heightComparator2 = new HeightComparator(true);
    private final ArrayList<BlockPos> blocks = new ArrayList<>();
    private final HashSet<BlockPos> keys = new HashSet<>();
    private final BlockArrayComputer computer;
    public int maxDepth = DEPTH_LIMIT;
    public boolean clampToChunkLoad = false;
    public boolean extraSpread = false;
    public boolean taxiCabDistance = false;
    public BlockBox bounds = BlockBox.infinity();
    protected boolean overflow = false;
    protected Level refWorld;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public BlockArray() {
        this(null);
    }

    public BlockArray(Collection<BlockPos> li) {
        computer = new BlockArrayComputer(this);
        if (li != null) {
            for (BlockPos c : li) {
                this.addBlockCoordinate(c);
            }
        }
    }

    private static int getMaxDepth() {
        int get = ReikaJavaLibrary.getMaximumRecursiveDepth();
        return get > 1000 ? get - 250 : Integer.MAX_VALUE;
    }

    public static BlockArray getXORBox(BlockArray b1, BlockArray b2) {
        BlockArray b = b1.instantiate();
        HashSet<BlockPos> set = new HashSet<>();
        set.addAll(b1.blocks);
        set.addAll(b2.blocks);
        for (BlockPos c : set) {
            if (b2.keys.contains(c) ^ b1.keys.contains(c)) {
                b.addKey(c);
            }
        }
        return b;
    }

    public static BlockArray getIntersectedBox(BlockArray b1, BlockArray b2) {
        BlockArray b = b1.instantiate();
        for (BlockPos c : b1.blocks) {
            if (b2.keys.contains(c)) {
                b.addKey(c);
            }
        }
        return b;
    }

    public static BlockArray getUnifiedBox(BlockArray b1, BlockArray b2) {
        BlockArray b = b1.instantiate();
        for (BlockPos c : b1.blocks) {
            b.addKey(c);
        }
        for (BlockPos c : b2.blocks) {
            b.addKey(c);
        }
        return b;
    }

    public static BlockArray fromBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        BlockArray b = new BlockArray();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    b.addBlockCoordinate(new BlockPos(x, y, z));
                }
            }
        }
        return b;
    }


    public BlockArray setWorld(Level world) {
        refWorld = world;
        return this;
    }

    public void addBlockCoordinate(int x, int y, int z) {
        addBlockCoordinate(new BlockPos(x, y, z));
    }

    public boolean addBlockCoordinate(BlockPos pos) {
        if (overflow)
            return false;
        if (this.hasBlock(pos))
            return false;
        if (!bounds.isBlockInside(pos))
            return false;
        BlockPos c = new BlockPos(pos);
        this.addKey(c);
        this.setLimits(pos);
//        DragonAPI.LOGGER.info("Adding "+pos);
        return true;
    }

    protected void addKey(BlockPos c) {
        blocks.add(c);
        keys.add(c);
//        DragonAPI.LOGGER.info(blocks.size()+" blocks in array. And " + keys.size()+" keys");
    }

    public boolean addBlockCoordinateIf(Level world, BlockPos pos, Block b) {
        return this.addBlockCoordinateIf(world, pos, new BlockKey(b));
    }

    public boolean addBlockCoordinateIf(Level world, BlockPos pos, BlockKey bk) {
        if (bk.matchInWorld(world, pos)) {
            return this.addBlockCoordinate(pos);
        }
        return false;
    }

    public boolean addBlockCoordinateIf(Level world, BlockPos pos, Collection<BlockKey> bk) {
        if (bk.contains(BlockKey.getAt(world, pos))) {
            return this.addBlockCoordinate(pos);
        }
        return false;
    }

    public void remove(BlockPos pos) {
        BlockPos c = new BlockPos(pos);
        this.removeKey(c);
        if (this.isEdge(pos)) {
            this.recalcLimits();
        }
    }

    protected void removeKey(BlockPos c) {
        blocks.remove(c);
        keys.remove(c);
    }

    protected boolean containsKey(BlockPos c) {
        return keys.contains(c);
    }

    public void recalcLimits() {
        this.resetLimits();
    }

    private void resetLimits() {
        //DragonAPI.LOGGER.info(minX+","+minY+","+minZ+" > "+maxX+","+maxY+","+maxZ, Dist.DEDICATED_SERVER);
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        minZ = Integer.MAX_VALUE;
        maxZ = Integer.MIN_VALUE;
        for (BlockPos c : blocks) {
            this.setLimits(c);
        }
        //DragonAPI.LOGGER.info(minX+","+minY+","+minZ+" > "+maxX+","+maxY+","+maxZ, Dist.DEDICATED_SERVER);
    }

    public final boolean isEdge(BlockPos pos) {
        return this.isEdgeX(pos) || this.isEdgeY(pos) || this.isEdgeZ(pos);
    }

    public final boolean isEdgeX(BlockPos pos) {
        return pos.getX() == minX || pos.getX() == maxX;
    }

    public final boolean isEdgeY(BlockPos pos) {
        return pos.getY() == minY || pos.getY() == maxY;
    }

    public final boolean isEdgeZ(BlockPos pos) {
        return pos.getZ() == minZ || pos.getZ() == maxZ;
    }

    public final int getMinX() {
        return minX;
    }

    public final int getMaxX() {
        return maxX;
    }

    public final int getMinY() {
        return minY;
    }

    public final int getMaxY() {
        return maxY;
    }

    public final int getMinZ() {
        return minZ;
    }

    public final int getMaxZ() {
        return maxZ;
    }

    public final int getSizeX() {
        return this.isEmpty() ? 0 : maxX - minX + 1;
    }

    public final int getSizeY() {
        return this.isEmpty() ? 0 : maxY - minY + 1;
    }

    public final int getSizeZ() {
        return this.isEmpty() ? 0 : maxZ - minZ + 1;
    }

    public final int getVolume() {
        return this.getSizeX() * this.getSizeY() * this.getSizeZ();
    }

    private final void setLimits(BlockPos pos) {
        if (pos.getX() < minX)
            minX = pos.getX();
        if (pos.getX() > maxX)
            maxX = pos.getX();
        if (pos.getY() < minY)
            minY = pos.getY();
        if (pos.getY() > maxY)
            maxY = pos.getY();
        if (pos.getZ() < minZ)
            minZ = pos.getZ();
        if (pos.getZ() > maxZ)
            maxZ = pos.getZ();
    }

    public BlockPos getNextBlock() {
        if (this.isEmpty())
            return null;
        return blocks.get(0);
    }

    public BlockPos getNthBlock(int n) {
        if (this.isEmpty())
            return null;
        return blocks.get(n);
    }

    public Set<BlockPos> keySet() {
        return Collections.unmodifiableSet(keys);
    }

    public List<BlockPos> list() {
        return Collections.unmodifiableList(blocks);
    }

    public BlockPos getNextAndMoveOn() {
        if (this.isEmpty())
            return null;
        BlockPos next = this.getNextBlock();
        this.remove(0);
        if (this.isEmpty())
            overflow = false;
        return next;
    }

    public final int getBottomBlockAtXZ(BlockPos pos) {
        int minY = Integer.MAX_VALUE;
        for (BlockPos c : this.keySet()) {
            if (c.getY() < minY)
                minY = c.getY();
        }
        return minY;
    }

    public final int getSize() {
        return blocks.size();
    }

    public void clear() {
        blocks.clear();
        keys.clear();
        overflow = false;
    }

    public final boolean isEmpty() {
        return blocks.isEmpty();
    }

    public final boolean hasBlock(BlockPos c) {
        return this.containsKey(c);
    }

    public final boolean hasBlock(int x, int y, int z) {
        return this.containsKey(new BlockPos(x,y,z));
    }
    /**
     * Recursively adds a contiguous area of one block type, akin to a fill tool.
     * Args: Level, start pos.getX(), start pos.getY(), start pos.getZ(), id to follow
     */
    public void recursiveAdd(BlockGetter world, BlockPos pos, Block id) {
        this.recursiveAdd(world, pos, pos, id, 0, new HashMap());
    }

    private void recursiveAdd(BlockGetter world, BlockPos pos0, BlockPos pos2, Block id, int depth, HashMap<BlockPos, Integer> map) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(pos2.getX() - pos0.getX()) + Math.abs(pos2.getY() - pos0.getY()) + Math.abs(pos2.getZ() - pos0.getZ()) > maxDepth)
            return;
        if (world.getBlockState(pos2).getBlock() != id)
            return;
        BlockPos c = new BlockPos(pos2);
        if (map.containsKey(c) && depth >= map.get(c))
            return;
        this.addBlockCoordinate(pos2);
        map.put(c, depth);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAdd(world, pos0, new BlockPos(pos0.getX() + i, pos0.getY() + j, pos0.getZ() + k), id, depth + 1, map);
            } else {
                this.recursiveAdd(world, pos0, pos0.east(), id, depth + 1, map);
                this.recursiveAdd(world, pos0, pos0.west(), id, depth + 1, map);
                this.recursiveAdd(world, pos0, pos0.above(), id, depth + 1, map);
                this.recursiveAdd(world, pos0, pos0.below(), id, depth + 1, map);
                this.recursiveAdd(world, pos0, pos2.south(), id, depth + 1, map);
                this.recursiveAdd(world, pos0, pos2.north(), id, depth + 1, map);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    /**
     * Like the ordinary recursive add but with a bounded volume. Args: Level, x, y, z,
     * id to replace, min x,y,z, max x,y,z
     */
    public void recursiveAddWithBounds(BlockGetter world, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.recursiveAddWithBounds(world, x, y, z, x, y, z, id, x1, y1, z1, x2, y2, z2, 0);
    }

    private void recursiveAddWithBounds(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2, int depth) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        if (world.getBlockState(new BlockPos(x, y, z)).getBlock() != id) {
            return;
        }
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddWithBounds(world, x0, y0, z0, x + i, y + j, z + k, id, x1, y1, z1, x2, y2, z2, depth + 1);
            } else {
                this.recursiveAddWithBounds(world, x0, y0, z0, x + 1, y, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBounds(world, x0, y0, z0, x - 1, y, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBounds(world, x0, y0, z0, x, y + 1, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBounds(world, x0, y0, z0, x, y - 1, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBounds(world, x0, y0, z0, x, y, z + 1, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBounds(world, x0, y0, z0, x, y, z - 1, id, x1, y1, z1, x2, y2, z2, depth + 1);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    /**
     * Like the ordinary recursive add but with a bounded volume; specifically excludes fluid source (meta == 0) blocks. Args: Level, x, y, z,
     * id to replace, min x,y,z, max x,y,z
     */
    public void recursiveAddWithBoundsNoFluidSource(BlockGetter world, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.recursiveAddWithBoundsNoFluidSource(world, x, y, z, x, y, z, id, x1, y1, z1, x2, y2, z2, 0);
    }

    private void recursiveAddWithBoundsNoFluidSource(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2, int depth) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        if (world.getBlockState(new BlockPos(x, y, z)).getBlock() != id) {
            return;
        }
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x + i, y + j, z + k, id, x1, y1, z1, x2, y2, z2, depth + 1);
            } else {
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x + 1, y, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x - 1, y, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x, y + 1, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x, y - 1, z, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x, y, z + 1, id, x1, y1, z1, x2, y2, z2, depth + 1);
                this.recursiveAddWithBoundsNoFluidSource(world, x0, y0, z0, x, y, z - 1, id, x1, y1, z1, x2, y2, z2, depth + 1);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    /**
     * Like the ordinary recursive add but with a bounded volume. Args: Level, x, y, z,
     * id to replace, min x,y,z, max x,y,z
     */
    public void recursiveAddWithBoundsRanged(BlockGetter world, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2, int r) {
        this.recursiveAddWithBoundsRanged(world, x, y, z, x, y, z, id, x1, y1, z1, x2, y2, z2, r, 0);
    }

    private void recursiveAddWithBoundsRanged(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, Block id, int x1, int y1, int z1, int x2, int y2, int z2, int r, int depth) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        if (world.getBlockState(new BlockPos(x, y, z)).getBlock() != id) {
            return;
        }
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    for (int k = -r; k <= r; k++) {
                        this.recursiveAddWithBoundsRanged(world, x0, y0, z0, x + i, y + j, z + k, id, x1, y1, z1, x2, y2, z2, r, depth + 1);
                    }
                }
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    public void recursiveAddMultipleWithBounds(BlockGetter world, int x, int y, int z, Set<BlockKey> ids, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.recursiveAddMultipleWithBounds(world, x, y, z, x, y, z, ids, x1, y1, z1, x2, y2, z2, 0, new HashMap<>());
    }

    private void recursiveAddMultipleWithBounds(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, Set<BlockKey> ids, int x1, int y1, int z1, int x2, int y2, int z2, int depth, HashMap<BlockPos, Integer> map) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        boolean flag = false;
        BlockKey bk = BlockKey.getAt(world, new BlockPos(x, y, z));
        if (ids.contains(bk)) {
            flag = true;
        }
        if (!flag)
            return;
        if (this.hasBlock(new BlockPos(x, y, z)))
            ;//return;
        BlockPos c = new BlockPos(x, y, z);
        if (map.containsKey(c) && depth >= map.get(c)) {
            return;
        }
        this.addBlockCoordinate(x, y, z);
        map.put(c, depth);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x + i, y + j, z + k, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
            } else {
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x + 1, y, z, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x - 1, y, z, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x, y + 1, z, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x, y - 1, z, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x, y, z + 1, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
                this.recursiveAddMultipleWithBounds(world, x0, y0, z0, x, y, z - 1, ids, x1, y1, z1, x2, y2, z2, depth + 1, map);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    public void recursiveMultiAddWithBounds(BlockGetter world, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, Block... ids) {
        this.recursiveMultiAddWithBounds(world, x, y, z, x, y, z, x1, y1, z1, x2, y2, z2, 0, ids);
    }

    /**
     * Like the ordinary recursive add but with a bounded volume and tolerance for multiple IDs. Args: Level, x, y, z,
     * id to replace, min x,y,z, max x,y,z
     */
    private void recursiveMultiAddWithBounds(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, int depth, Block... ids) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        boolean flag = false;
        for (int i = 0; i < ids.length; i++) {
            if (world.getBlockState(new BlockPos(x, y, z)).getBlock() == ids[i]) {
                flag = true;
            }
        }
        if (!flag)
            return;
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveMultiAddWithBounds(world, x0, y0, z0, x + i, y + j, z + k, x1, y1, z1, x2, y2, z2, depth + 1, ids);
            } else {
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x + 1, y, z, x1, y1, z1, x2, y2, z2, depth + 1, ids);
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x - 1, y, z, x1, y1, z1, x2, y2, z2, depth + 1, ids);
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x, y + 1, z, x1, y1, z1, x2, y2, z2, depth + 1, ids);
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x, y - 1, z, x1, y1, z1, x2, y2, z2, depth + 1, ids);
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x, y, z + 1, x1, y1, z1, x2, y2, z2, depth + 1, ids);
                this.recursiveMultiAddWithBounds(world, x0, y0, z0, x, y, z - 1, x1, y1, z1, x2, y2, z2, depth + 1, ids);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }


    public void recursiveAddCallbackWithBounds(Level world, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, AbstractSearch.PropagationCondition f) {
        this.recursiveAddCallbackWithBounds(world, x, y, z, x, y, z, x1, y1, z1, x2, y2, z2, f, 0, new HashMap<>());
    }

    private void recursiveAddCallbackWithBounds(Level world, int x0, int y0, int z0, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, AbstractSearch.PropagationCondition f, int depth, HashMap<BlockPos, Integer> map) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        if (!f.isValidLocation(world, new BlockPos(x, y, z), new BlockPos(x0, y0, z0))) {
            return;
        }
        if (this.hasBlock(new BlockPos(x, y, z))) ;//return;
        BlockPos c = new BlockPos(x, y, z);
        if (map.containsKey(c) && depth >= map.get(c)) {
            return;
        }
        this.addBlockCoordinate(x, y, z);
        map.put(c, depth);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x + i, y + j, z + k, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
            } else {
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x + 1, y, z, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x - 1, y, z, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x, y + 1, z, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x, y - 1, z, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x, y, z + 1, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
                this.recursiveAddCallbackWithBounds(world, x0, y0, z0, x, y, z - 1, x1, y1, z1, x2, y2, z2, f, depth + 1, map);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    public void iterativeAddCallbackWithBounds(Level world, int x0, int y0, int z0, int x1, int y1, int z1, int x2, int y2, int z2, AbstractSearch.PropagationCondition f) {
        BlockPos root = new BlockPos(x0, y0, z0);
        if (!f.isValidLocation(world, new BlockPos(x0, y0, z0), root))
            return;
        HashSet<BlockPos> next = new HashSet<>();
        next.add(root);
        while (!next.isEmpty()) {
            HashSet<BlockPos> toNext = new HashSet<>();
            for (BlockPos c : next) {
                if (c.getX() < x1 || c.getY() < y1 || c.getZ() < z1 || c.getX() > x2 || c.getY() > y2 || c.getZ() > z2)
                    continue;
                this.addBlockCoordinate(c.getX(), c.getY(), c.getZ());
                if (extraSpread) {
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            for (int k = -1; k <= 1; k++) {
                                BlockPos c2 = c.offset(i, j, k);
                                if (!keys.contains(c2) && f.isValidLocation(world, c2, c)) {
                                    toNext.add(c2);
                                }
                            }
                        }
                    }
                } else {
                    for (BlockPos c2 : CoordHelper.getAdjacentCoordinates(c)) {
                        if (!keys.contains(c2) && f.isValidLocation(world, c2, c)) {
                            toNext.add(c2);
                        }
                    }
                }
            }
            next = toNext;
        }
    }

    public void recursiveAddLiquidWithBounds(BlockGetter world, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, Fluid liquid) {
        this.recursiveAddLiquidWithBounds(world, x, y, z, x, y, z, x1, y1, z1, x2, y2, z2, 0, liquid);
    }

    /**
     * Like the ordinary recursive add but with a bounded volume. Args: Level, x, y, z,
     * id to replace, min x,y,z, max x,y,z
     */
    private void recursiveAddLiquidWithBounds(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2, int depth, Fluid liquid) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        //DragonAPI.LOGGER.info(liquidID+" and "+world.getBlock(x, y, z));;
        if (x < x1 || y < y1 || z < z1 || x > x2 || y > y2 || z > z2)
            return;
        Fluid f2 = ReikaWorldHelper.getFluidState(world, x, y, z).getType();
        if (f2 == null)
            return;
        if (liquid != null && f2 != liquid) {
            //DragonAPI.LOGGER.info("Could not match id "+world.getBlock(x, y, z)+" to "+liquidID);
            return;
        }
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x + i, y + j, z + k, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
            } else {
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x + 1, y, z, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x - 1, y, z, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x, y + 1, z, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x, y - 1, z, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x, y, z + 1, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
                this.recursiveAddLiquidWithBounds(world, x0, y0, z0, x, y, z - 1, x1, y1, z1, x2, y2, z2, depth + 1, liquid);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    /**
     * Like the ordinary recursive add but with a spherical bounded volume. Args: Level, x, y, z,
     * id to replace, origin x,y,z, max radius
     */
    private void recursiveAddWithinSphere(BlockGetter world, int x0, int y0, int z0, int x, int y, int z, Block id, int dx, int dy, int dz, double r, int depth) {
        if (overflow)
            return;
        if (depth > maxDepth)
            return;
        if (taxiCabDistance && Math.abs(x - x0) + Math.abs(y - y0) + Math.abs(z - z0) > maxDepth)
            return;
        if (world.getBlockState(new BlockPos(x, y, z)).getBlock() != id)
            return;
        if (this.hasBlock(new BlockPos(x, y, z)))
            return;
        if (ReikaMathLibrary.py3d(x - x0, y - y0, z - z0) > r)
            return;
        this.addBlockCoordinate(x, y, z);
        try {
            if (extraSpread) {
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++)
                        for (int k = -1; k <= 1; k++)
                            this.recursiveAddWithinSphere(world, x0, y0, z0, x + i, y + j, z + k, id, x0, y0, z0, r, depth + 1);
            } else {
                this.recursiveAddWithinSphere(world, x0, y0, z0, x + 1, y, z, id, dx, dy, dz, r, depth + 1);
                this.recursiveAddWithinSphere(world, x0, y0, z0, x - 1, y, z, id, dx, dy, dz, r, depth + 1);
                this.recursiveAddWithinSphere(world, x0, y0, z0, x, y + 1, z, id, dx, dy, dz, r, depth + 1);
                this.recursiveAddWithinSphere(world, x0, y0, z0, x, y - 1, z, id, dx, dy, dz, r, depth + 1);
                this.recursiveAddWithinSphere(world, x0, y0, z0, x, y, z + 1, id, dx, dy, dz, r, depth + 1);
                this.recursiveAddWithinSphere(world, x0, y0, z0, x, y, z - 1, id, dx, dy, dz, r, depth + 1);
            }
        } catch (StackOverflowError e) {
            this.throwOverflow(depth);
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        if (this.isEmpty())
            return "Empty[]";
        StringBuilder list = new StringBuilder();
        list.append(this.getSize() + ": ");
        for (int i = 0; i < this.getSize(); i++) {
            BlockPos c = this.getNthBlock(i);
            if (refWorld != null) {
                Block id = refWorld.getBlockState(c).getBlock();
                list.append(id);
            }
            list.append(c.toString());
            if (i != this.getSize() - 1)
                list.append(";");
        }
        return list.toString();
    }

    public void addLineOfClear(Level world, BlockPos pos, int range, int stepx, int stepy, int stepz) {
        if (stepx == 0 && stepy == 0 && stepz == 0)
            throw new MisuseException("The addLineOfClear() method requires a specified direction!");
        if (stepx != 0) {
            if (stepy != 0 || stepz != 0)
                throw new MisuseException("The addLineOfClear() method is only designed for 1D lines!");
            if (stepx != -1 && stepx != 1)
                throw new MisuseException("The addLineOfClear() method is only designed for solid lines!");
            if (stepx == 1) {
                for (int i = pos.getX() + 1; i <= pos.getX() + range; i++) {
                    if (!this.addIfClear(world, new BlockPos(i, pos.getY(), pos.getZ())))
                        return;
                }
            } else {
                for (int i = pos.getX() - 1; i >= pos.getX() - range; i--) {
                    if (!this.addIfClear(world, new BlockPos(i, pos.getY(), pos.getZ())))
                        return;
                }
            }
        } else if (stepy != 0) {
            if (stepx != 0 || stepz != 0)
                throw new MisuseException("The addLineOfClear() method is only designed for 1D lines!");
            if (stepy != -1 && stepy != 1)
                throw new MisuseException("The addLineOfClear() method is only designed for solid lines!");
            if (stepy == 1) {
                for (int i = pos.getY() + 1; i <= pos.getY() + range; i++) {
                    if (!this.addIfClear(world, new BlockPos(pos.getX(), i, pos.getZ())))
                        return;
                }
            } else {
                for (int i = pos.getY() - 1; i >= pos.getY() - range; i--) {
                    if (!this.addIfClear(world, new BlockPos(pos.getX(), i, pos.getZ())))
                        return;
                }
            }
        } else if (stepz != 0) {
            if (stepy != 0 || stepx != 0)
                throw new MisuseException("The addLineOfClear() method is only designed for 1D lines!");
            if (stepz != -1 && stepz != 1)
                throw new MisuseException("The addLineOfClear() method is only designed for solid lines!");
            if (stepz == 1) {
                for (int i = pos.getZ() + 1; i <= pos.getZ() + range; i++) {
                    if (!this.addIfClear(world, new BlockPos(pos.getX(), pos.getY(), i)))
                        return;
                }
            } else {
                for (int i = pos.getZ() - 1; i >= pos.getZ() - range; i--) {
                    if (!this.addIfClear(world, new BlockPos(pos.getX(), pos.getY(), i)))
                        return;
                }
            }
        }
    }

    public void addSphere(Level world, int x, int y, int z, Block id, double r) {
        if (r == 0)
            return;
        try {
            this.recursiveAddWithinSphere(world, x, y, z, x + 1, y, z, id, x, y, z, r, 0);
            this.recursiveAddWithinSphere(world, x, y, z, x, y + 1, z, id, x, y, z, r, 0);
            this.recursiveAddWithinSphere(world, x, y, z, x, y, z + 1, id, x, y, z, r, 0);
            this.recursiveAddWithinSphere(world, x, y, z, x - 1, y, z, id, x, y, z, r, 0);
            this.recursiveAddWithinSphere(world, x, y, z, x, y - 1, z, id, x, y, z, r, 0);
            this.recursiveAddWithinSphere(world, x, y, z, x, y, z - 1, id, x, y, z, r, 0);
        } catch (StackOverflowError e) {
            this.throwOverflow(0);
            e.printStackTrace();
        }
    }

    public StructuredBlockArray offset(int x, int y, int z) {
        this.offset(new BlockPos(x, y, z));
        return null;
    }

    public final int sink(Level world) {
        boolean canSink = true;
        int n = 0;
        while (canSink) {
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos c = this.getNthBlock(i);
                if (!ReikaWorldHelper.softBlocks(world, c.below())) {
                    canSink = false;
                }
            }
            if (canSink) {
                this.offset(0, -1, 0);
                n++;
            }
        }
        return n;
    }

    public final int sink(Level world, Blocks... overrides) {
        boolean canSink = true;
        int n = 0;
        while (canSink) {
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos c = this.getNthBlock(i);
                Block idy = world.getBlockState(c.below()).getBlock();
                if (!ReikaWorldHelper.softBlocks(world, c.below()) && !ReikaArrayHelper.contains(overrides, idy)) {
                    canSink = false;
                }
            }
            if (canSink) {
                this.offset(0, -1, 0);
                n++;
            }
        }
        return n;
    }

    public final BlockArray copy() {
        BlockArray copy = this.instantiate();
        this.copyTo(copy);
        return copy;
    }

    protected BlockArray instantiate() {
        return new BlockArray();
    }

    public void copyTo(BlockArray copy) {
        copy.refWorld = refWorld;
        copy.overflow = overflow;
        copy.blocks.clear();
        copy.blocks.addAll(blocks);
        copy.keys.clear();
        copy.keys.addAll(keys);
        copy.recalcLimits();
		/*
		copy.minX = minX;
		copy.minY = minY;
		copy.minZ = minZ;
		copy.maxX = maxX;
		copy.maxY = maxY;
		copy.maxZ = maxZ;*/
    }

    public void addAll(BlockArray arr) {
        for (BlockPos c : arr.blocks) {
            if (!keys.contains(c)) {
                this.addBlockCoordinate(c.getX(), c.getY(), c.getZ());
            }
        }
    }

    public void addAll(BlockBox box) {
        for (int x = box.minX; x <= box.maxX; x++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                for (int y = box.minY; y <= box.maxY; y++) {
                    this.addBlockCoordinate(x, y, z);
                }
            }
        }
    }

    public void clearArea() {
        this.setTo(Blocks.AIR);
    }

    public boolean addIfClear(Level world, BlockPos pos) {
        Block id = world.getBlockState(pos).getBlock();
        if (id == Blocks.AIR) {
            this.addBlockCoordinate(pos);
            return true;
        }
//        if (!id.canCollideCheck(false) && !LiquidBlock.class.isAssignableFrom(id.getClass())) {
//            this.addBlockCoordinate(pos);
//            return true;
//        }
        if (id instanceof SemiTransparent b) {
            if (b.isOpaque())
                return false;
        }
        //if (!id.isOpaqueCube()) //do not block but do not add
        //    return true;
        return false;
    }

    protected void throwOverflow(int depth) {
        overflow = true;
        DragonAPI.LOGGER.error("Stack overflow at depth " + depth + "/" + maxDepth + "!");
    }

    public BlockPos getRandomBlock() {
        return this.isEmpty() ? null : this.getNthBlock(rand.nextInt(this.getSize()));
    }

    private void remove(int index) {
        BlockPos c = blocks.remove(index);
        keys.remove(c);
    }

    public boolean isOverflowing() {
        return overflow;
    }

    public boolean hasWorldReference() {
        return refWorld != null;
    }

    public final BlockArray offset(Direction dir, int dist) {
        return this.offset(new BlockPos(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist));
    }

    public BlockArray offset(BlockPos pos) {
        Collection<BlockPos> temp = new ArrayList(blocks);
        keys.clear();
        blocks.clear();
        for (BlockPos c : temp) {
            BlockPos c2 = c.offset(pos);
            blocks.add(c2);
            keys.add(c2);
        }

		/*
		minX += pos.getX();
		maxX += pos.getX();
		minY += pos.getY();
		maxY += pos.getY();
		minZ += pos.getZ();
		maxZ += pos.getZ();
		 */
        this.resetLimits();

        return this;
    }

    public final boolean isAtLeastXPercent(Level world, double percent, Block id) {
        double s = this.getSize();
        int ct = 0;
        for (int i = 0; i < this.getSize(); i++) {
            BlockPos c = this.getNthBlock(i);

            Block id2 = world.getBlockState(c).getBlock();
            if (id2 == id) {
                ct++;
            }
        }
        return ct / s * 100D >= percent;
    }

    public void setTo(Block b) {
        if (refWorld != null) {
            for (int i = 0; i < this.getSize(); i++) {
                BlockPos c = this.getNthBlock(i);
                refWorld.getBlockState(c).getBlock();
            }
        } else {
            throw new MisuseException("Cannot apply operations to a null world!");
        }
    }

    public void saveAdditional(String label, CompoundTag NBT) {
        ListTag li = new ListTag();
        for (int i = 0; i < this.getSize(); i++) {
            CompoundTag tag = new CompoundTag();
            BlockPos c = this.getNthBlock(i);
            tag.putInt("pos.getX()", c.getX());
            tag.putInt("pos.getY()", c.getY());
            tag.putInt("pos.getZ()", c.getZ());
            li.add(tag);
        }
        NBT.put(label, li);
        CompoundTag limit = new CompoundTag();
        limit.putInt("minx", minX);
        limit.putInt("miny", minY);
        limit.putInt("minz", minZ);
        limit.putInt("maxx", maxX);
        limit.putInt("maxy", maxY);
        limit.putInt("maxz", maxZ);
        NBT.put(label + "_lim", limit);
    }

    public void load(String label, CompoundTag NBT) {
        this.clear();
        ListTag tag = NBT.getList(label, Tag.TAG_COMPOUND);
        if (tag == null || tag.size() == 0)
            return;
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag coord = tag.getCompound(i);
            int x = coord.getInt("x");
            int y = coord.getInt("y");
            int z = coord.getInt("z");
            this.addBlockCoordinate(new BlockPos(x, y, z));
        }
        CompoundTag limit = NBT.getCompound(label + "_lim");
        minX = limit.getInt("minx");
        minY = limit.getInt("miny");
        minZ = limit.getInt("minz");
        maxX = limit.getInt("maxx");
        maxY = limit.getInt("maxy");
        maxZ = limit.getInt("maxz");
    }

    public void shaveToCube() {
        if (this.isEmpty())
            return;
        boolean changed = false;
        do {
            int s1 = this.getSize();

            Collection<BlockPos> set = new HashSet(blocks);
            for (BlockPos c : set) {
                int n = this.countNeighbors(c);
                if (n < 11) {
                    this.removeKey(c);
                }
            }

            changed = this.getSize() != s1;
        } while (changed);

        this.resetLimits();
    }

    private int countNeighbors(BlockPos c) {
        int n = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (keys.contains(c.offset(i, j, k)))
                        n++;
                }
            }
        }
        return n;
    }

    public void XORWith(BlockArray b) {
        HashSet<BlockPos> set = new HashSet();
        set.addAll(blocks);
        set.addAll(b.blocks);
        this.clear();
        for (BlockPos c : set) {
            if (keys.contains(c) ^ b.keys.contains(c)) {
                this.addKey(c);
            }
        }
    }

    public void intersectWith(BlockArray b) {
        Iterator<BlockPos> it = blocks.iterator();
        while (it.hasNext()) {
            BlockPos c = it.next();
            if (!b.keys.contains(c)) {
                it.remove();
                keys.remove(c);
            }
        }
        this.resetLimits();
    }

    public void unifyWith(BlockArray b) {
        for (BlockPos c : b.blocks) {
            this.addKey(c);
        }
        this.resetLimits();
    }

//    public final AABB asAABB() {
//        return AABB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
//    }

    public final BlockBox asBlockBox() {
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void reverseBlockOrder() {
        Collections.reverse(blocks);
    }

    public void sortBlocksByHeight(boolean reverse) {
        this.sort(reverse ? heightComparator2 : heightComparator);
    }

    public void sortBlocksByDistance(BlockPos loc) {
        // this.sort(new InwardsComparator(loc));
    }

    public void sort(Comparator<BlockPos> comparator) {
        Collections.sort(blocks, comparator);
    }
//
//    public BlockArray rotate90Degrees(int ox, int oz, boolean left) {
//        BlockArray b = this.instantiate();
//        for (BlockPos c : blocks) {
//            BlockPos c2 = c.rotate90About(ox, oz, left);
//            b.addBlockCoordinate(new BlockPos(c2.getX(), c2.getY(), c2.getZ()));
//        }
//        return b;
//    }
//
//    public BlockArray rotate180Degrees(int ox, int oz) {
//        BlockArray b = this.instantiate();
//        for (BlockPos c : blocks) {
//            BlockPos c2 = c.rotate180About(ox, oz);
//            b.addBlockCoordinate(new BlockPos(c2.getX(), c2.getY(), c2.getZ()));
//        }
//        return b;
//    }

    public BlockArray flipX() {
        BlockArray b = this.instantiate();
        for (BlockPos c : blocks) {
            BlockPos c2 = new BlockPos(-c.getX(), c.getY(), c.getZ());
            b.addBlockCoordinate(new BlockPos(c2.getX(), c2.getY(), c2.getZ()));
        }
        return b;
    }

    public BlockArray flipZ() {
        BlockArray b = this.instantiate();
        for (BlockPos c : blocks) {
            BlockPos c2 = new BlockPos(c.getX(), c.getY(), -c.getZ());
            b.addBlockCoordinate(new BlockPos(c2.getX(), c2.getY(), c2.getZ()));
        }
        return b;
    }

    public void expand(int amt, boolean rounded) {
        HashSet<BlockPos> set = new HashSet();
        for (BlockPos c : blocks) {
            if (rounded) {
                for (int i = -amt; i <= amt; i++) {
                    for (int j = -amt; j <= amt; j++) {
                        for (int k = -amt; k <= amt; k++) {
                            BlockPos c2 = c.offset(i, j, k);
                            if (!keys.contains(c2)) {
                                set.add(c2);
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < 6; i++) {
                    for (int k = 1; k <= amt; k++) {
                        var dir = Direction.values()[i];
                        BlockPos c2 = c.offset(dir.getStepX()*k, dir.getStepY()*k, dir.getStepZ()*k);//c.offset(k, Direction.values()[i]);
                        if (!keys.contains(c2)) {
                            set.add(c2);
                        }
                    }
                }
            }
        }
        for (BlockPos c : set) {
            this.addKey(c);
        }
    }

    public Collection<BlockArray> splitToRectangles() {
        ArrayList<BlockArray> li = new ArrayList<>();
        HashSet<BlockPos> locs = new HashSet<>(keys);
        while (!locs.isEmpty()) {
            ArrayList<BlockPos> locList = new ArrayList<>(locs);
            int idx = rand.nextInt(locs.size());
            BlockPos c = locList.remove(idx);
            locs.remove(c);
            ArrayList<BlockPos> block = new ArrayList<>();
            block.add(c);
            ArrayList<Direction> dirs = ReikaDirectionHelper.getRandomOrderedDirections(true);
            while (!dirs.isEmpty()) {
                Direction dir = dirs.remove(0);
                int d = 1;
                boolean flag = true;
                while (flag) {
                    ArrayList<BlockPos> add = new ArrayList<>();
                    for (BlockPos in : block) {
                        BlockPos offset = c.offset(dir.getStepX()*d, dir.getStepY()*d, dir.getStepZ()*d);
                        if (!block.contains(offset)) {
                            if (!locs.contains(offset)) {
                                //ReikaJavaLibrary.pConsole("Failed to expand "+block+" "+dir+" due to bounds @ "+offset);
                                flag = false;
                                break;
                            } else {
                                add.add(offset);
                            }
                        }
                    }
                    if (flag) {
                        //ReikaJavaLibrary.pConsole("Adding "+add+" to "+block);
                        for (BlockPos in : add) {
                            block.add(in);
                            locs.remove(in);
                        }
                        //d++;
                    }
                }
            }
            li.add(new BlockArray(block));
        }
        return li;
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return new BlockArrayIterator();
    }

    private static class HeightComparator implements Comparator<BlockPos> {

        private final boolean reverse;

        private HeightComparator(boolean rev) {
            reverse = rev;
        }

        @Override
        public int compare(BlockPos o1, BlockPos o2) {
            return reverse ? o2.getY() - o1.getY() : o1.getY() - o2.getY();
        }

    }
//
//    public static abstract class BlockTypePrioritizer implements Comparator<BlockPos> {
//
//        private final Level world;
//
//        protected BlockTypePrioritizer(Level world) {
//            this.world = world;
//        }
//
//        public final int compare(BlockPos c1, BlockPos c2) {
//            BlockKey b1 = c1.getBlockKey(world);
//            BlockKey b2 = c2.getBlockKey(world);
//            return this.compare(b1, b2);
//        }
//
//        protected abstract int compare(BlockKey b1, BlockKey b2);
//
//    }
//
//    private class InwardsComparator implements Comparator<BlockPos> {
//
//        private final BlockPos location;
//
//        private InwardsComparator(BlockPos c) {
//            location = c;
//        }
//
//        @Override
//        public int compare(BlockPos o1, BlockPos o2) {
//            return (int) Math.signum(o2.getDistanceTo(location) - o1.getDistanceTo(location));
//        }
//
//    }

    private final class BlockArrayIterator implements Iterator<BlockPos> {

        private int index;

        private BlockArrayIterator() {

        }

        @Override
        public boolean hasNext() {
            return blocks.size() > index + 1;
        }

        @Override
        public BlockPos next() {
            BlockPos c = blocks.get(index);
            index++;
            return c;
        }

        @Override
        public void remove() {
            BlockArray.this.remove(index);
        }

    }
}
