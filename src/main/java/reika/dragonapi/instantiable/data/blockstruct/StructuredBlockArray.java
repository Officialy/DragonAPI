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
        super.addBlockCoordinate(pos);
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
        super.copyTo(copy);
        if (copy instanceof StructuredBlockArray) {
            ((StructuredBlockArray)copy).data.putAll(data);
        }
    }

    @Override
    public void addAll(BlockArray arr) {
        super.addAll(arr);
        if (arr instanceof StructuredBlockArray) {
            data.putAll(((StructuredBlockArray)arr).data);
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

//   todo @Override
//    public BlockArray rotate90Degrees(int ox, int oz, boolean left) {
//        StructuredBlockArray b = (StructuredBlockArray)super.rotate90Degrees(ox, oz, left);
//        for (BlockPos c : data.keySet()) {
//            BlockKey bc = data.get(c);
//            BlockPos c2 = c.rotate90About(ox, oz, left);
//            b.addBlockCoordinate(c2, bc);
//        }
//        return b;
//    }
//
// todo   @Override
//    public BlockArray rotate180Degrees(int ox, int oz) {
//        StructuredBlockArray b = (StructuredBlockArray)super.rotate180Degrees(ox, oz);
//        for (BlockPos c : data.keySet()) {
//            BlockKey bc = data.get(c);
//            BlockPos c2 = c.rotate180About(ox, oz);
//
//            b.addBlockCoordinate(c2, bc);
//        }
//        return b;
//    }

    @Override
    public void clear() {
        super.clear();
        data.clear();
    }

    @Override
    public BlockArray flipX() {
        StructuredBlockArray b = (StructuredBlockArray)this.instantiate();
        for (BlockPos c : data.keySet()) {
            BlockKey bc = data.get(c);
            BlockPos c2 = new BlockPos(-c.getX(), c.getY(), c.getZ());
            b.addBlockCoordinate(c2, bc);
        }
        return b;
    }

    @Override
    public BlockArray flipZ() {
        StructuredBlockArray b = (StructuredBlockArray)this.instantiate();
        for (BlockPos c : data.keySet()) {
            BlockKey bc = data.get(c);
            BlockPos c2 = new BlockPos(c.getX(), c.getY(), -c.getZ());
            b.addBlockCoordinate(c2, bc);
        }
        return b;
    }
}
