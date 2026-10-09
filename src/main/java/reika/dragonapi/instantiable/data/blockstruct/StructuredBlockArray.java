package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.BlockCheck;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import reika.dragonapi.exception.MisuseException;

public class StructuredBlockArray extends BlockArray {

    private final HashMap<BlockPos, BlockKey> data = new HashMap<>();

    public final Level world;

    public StructuredBlockArray(Level world) {
        this.world = world;
        this.setWorld(world);
    }

    @Override
    public BlockPos getNextBlock() {
        if (data.isEmpty())
            return null;
        BlockPos li = data.keySet().iterator().next();
        return li;
    }

    @Override
    public BlockPos getNthBlock(int n) {
		/*
		if (data.isEmpty())
			return null;
		int a = 0;
		for (BlockPos li : data.keySet()) {
			if (a == n) {
				return li;
			}
			a++;
		}
		return null;
		return blocks.get(n);*/
        return super.getNthBlock(n);
    }

    @Override
    public Set<BlockPos> keySet() {
        return Collections.unmodifiableSet(data.keySet());
    }

    @Override
    public BlockPos getNextAndMoveOn() {
        if (data.isEmpty())
            return null;
        BlockPos li = data.keySet().iterator().next();
        this.remove(li);
        return li;
    }

    @Override
    public boolean addBlockCoordinate(BlockPos pos) {
        return this.addBlockCoordinate(pos, new BlockKey(world.getBlockState(pos)));
    }

    /** Declared layouts supply their state without reading a live world, including during datagen. */
    protected final boolean addBlockCoordinate(BlockPos pos, BlockKey state) {
        if (overflow || this.hasBlock(pos))
            return false;
        if (!super.addBlockCoordinate(pos))
            return false;
        data.put(pos.immutable(), state);
        return true;
    }

    /** Keeps coordinate membership and the representative state consistent on replacements. */
    protected final boolean putBlockKey(BlockPos pos, BlockKey state) {
        if (!this.hasBlock(pos) && !super.addBlockCoordinate(pos))
            return false;
        data.put(pos.immutable(), state);
        return true;
    }

    public BlockKey getBlockKeyRelativeToMinXYZ(int dx, int dy, int dz) {
        int x = dx+this.getMinX();
        int y = dy+this.getMinY();
        int z = dz+this.getMinZ();
        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)) : null;
    }

    public Block getBlockRelativeToMinXYZ(int dx, int dy, int dz) {
        int x = dx+this.getMinX();
        int y = dy+this.getMinY();
        int z = dz+this.getMinZ();
        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).blockID.getBlock() : null;
    }

    public BlockKey getBlockKeyAt(int x, int y, int z) {
        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)) : null;
    }

    public Block getBlockAt(int x, int y, int z) {
        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).blockID.getBlock() : null;
    }

    public final boolean hasNonAirBlock(int x, int y, int z) {
        Block b = this.getBlockAt(x, y, z);
        return b != null && !b.defaultBlockState().isAir();
    }

    @Override
    public BlockPos getRandomBlock() {
        BlockPos a = super.getRandomBlock();
        return a;
    }

    @Override
    public void remove(BlockPos pos) {
        super.remove(pos);
        data.remove(pos);
    }

    public int getNumberOf(Block id) {
        int count = 0;
        for (BlockPos li : data.keySet()) {
            BlockKey block = data.get(li);
            if (block.blockID.is(id))
                count++;
        }
        return count;
    }

    @Override
    public StructuredBlockArray offset(int x, int y, int z) {
        return this.offset(new BlockPos(x, y, z));
    }

    @Override
    public StructuredBlockArray offset(BlockPos offset) {
        super.offset(offset);
        HashMap<BlockPos, BlockKey> shifted = new HashMap<>();
        data.forEach((position, key) -> shifted.put(position.offset(offset), key));
        data.clear();
        data.putAll(shifted);
        return this;
    }

    public int getMidX() {
        return this.getMinX()+this.getSizeX()/2;
    }

    public int getMidY() {
        return this.getMinY()+this.getSizeY()/2;
    }

    public int getMidZ() {
        return this.getMinZ()+this.getSizeZ()/2;
    }

    @Override
    public String toString() {
        return data.size()+": "+ data;
    }

    @Override
    protected BlockArray instantiate() {
        return new StructuredBlockArray(world);
    }

    @Override
    public void copyTo(BlockArray copy) {
        if (copy == this)
            return;
        super.copyTo(copy);
        if (copy instanceof StructuredBlockArray target) {
            target.data.clear();
            target.data.putAll(data);
        }
    }

    @Override
    public void addAll(BlockArray arr) {
        for (BlockPos position : arr) {
            BlockKey key;
            if (arr instanceof StructuredBlockArray source)
                key = source.getBlockKeyAt(position.getX(), position.getY(), position.getZ());
            else if (world != null)
                key = BlockKey.getAt(world, position);
            else
                throw new MisuseException("A world-free structured array can only merge declared block states");
            this.putBlockKey(position, key);
        }
    }

    public ItemHashMap<Integer> getItems() {
        ItemHashMap<Integer> map = new ItemHashMap<>();
        for (BlockPos c : data.keySet()) {
            BlockKey bk = data.get(c);
            if (bk.blockID.getBlock() instanceof AirBlock)
                continue;
            if (Item.BY_BLOCK.get(bk.blockID.getBlock()) == null)
                continue;
            ItemStack is = bk.asItemStack();
            Integer get = map.get(is);
            int amt = get != null ? get.intValue() : 0;
            map.put(is, amt+1);
        }
        return map;
    }

    @Override
    protected StructuredBlockArray transform(Function<BlockPos, BlockPos> transform) {
        StructuredBlockArray result = (StructuredBlockArray)super.transform(transform);
        data.forEach((position, key) -> result.data.put(transform.apply(position).immutable(), key));
        return result;
    }

    @Override
    public StructuredBlockArray rotate90Degrees(int ox, int oz, boolean left) {
        return (StructuredBlockArray)super.rotate90Degrees(ox, oz, left);
    }

    @Override
    public StructuredBlockArray rotate180Degrees(int ox, int oz) {
        return (StructuredBlockArray)super.rotate180Degrees(ox, oz);
    }

    @Override
    public void clear() {
        super.clear();
        data.clear();
    }

    @Override
    public StructuredBlockArray flipX() {
        return (StructuredBlockArray)super.flipX();
    }

    @Override
    public StructuredBlockArray flipZ() {
        return (StructuredBlockArray)super.flipZ();
    }
}
