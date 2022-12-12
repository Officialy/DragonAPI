package reika.dragonapi.instantiable.data.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

import java.util.*;

public final class BlockMap<V> {

    private final HashMap<BlockKey, V> data = new HashMap<>();

    public V get(BlockKey bk) {
        //ReikaJavaLibrary.pConsole(bk+" >> "+data.keySet());
        return data.get(bk);
    }

    public V get(Block b) {
        return this.get(new BlockKey(b));
    }

    public V get(BlockGetter world, BlockPos pos) {
        return this.get(BlockKey.getAt(world, pos));
    }

    public V put(BlockKey bk, V obj) {
        return data.put(bk, obj);
    }

    public V put(Block b, V obj) {
        return this.put(new BlockKey(b), obj);
    }

    public V put(BlockGetter world, BlockPos pos, V obj) {
        return this.put(BlockKey.getAt(world, pos), obj);
    }

    public boolean containsKey(BlockKey bk) {
        return data.containsKey(bk);
    }

    public boolean containsKey(Block b) {
        return this.containsKey(new BlockKey(b));
    }

    public boolean containsKey(BlockGetter world, BlockPos pos) {
        return this.containsKey(BlockKey.getAt(world, pos));
    }

    public void clear() {
        data.clear();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockMap && data.equals(((BlockMap) o).data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    public ArrayList<V> getForBlock(BlockState b) {
        ArrayList<V> li = new ArrayList<>();
        for (BlockKey bk : data.keySet()) {
            if (bk.blockID == b) {
                li.add(this.get(bk));
            }
        }
        return li;
    }

    public int size() {
        return data.size();
    }

    public Set<BlockKey> keySet() {
        return Collections.unmodifiableSet(this.data.keySet());
    }

    public Collection<V> values() {
        return Collections.unmodifiableCollection(data.values());
    }
}
