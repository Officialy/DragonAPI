package reika.dragonapi.instantiable.data.collections;


import reika.dragonapi.libraries.ReikaNBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class ItemCollection {

    private final ArrayList<ItemStack> data = new ArrayList<>();

    public ItemCollection() {

    }

    public ItemCollection(Collection<ItemStack> c) {
        this.add(c);
    }

    public void add(ItemStack is) {
        data.add(is);
    }

    public void add(Collection<ItemStack> c) {
        data.addAll(c);
    }

    public int count() {
        int ret = 0;
        for (ItemStack is : data) {
            ret += is.getCount();
        }
        return ret;
    }

    public void drop(Level world, BlockPos pos) {
        for (ItemStack is : data) {
            while (is.getCount() > 0) {
                int num = Math.min(is.getCount(), is.getMaxStackSize());
                //ItemStack is2 = ReikaItemHelper.getSizedItemStack(is, num);
                is.setCount(num--); //-= num
                //ReikaItemHelper.dropItem(world, x + world.rand.nextDouble(), y + world.rand.nextDouble(), z + world.rand.nextDouble(), is2);
            }
        }
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public void clear() {
        data.clear();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void saveAdditional(CompoundTag NBT) {
        ReikaNBTHelper.writeCollectionToNBT(data, NBT, "items");
    }

    public void load(CompoundTag NBT) {
        ReikaNBTHelper.readCollectionFromNBT(data, NBT, "items");
    }

    public int removeItems(int amt) {
        int ret = 0;
        Iterator<ItemStack> it = data.iterator();
        while (it.hasNext()) {
            ItemStack is = it.next();
            int rem = Math.min(is.getCount(), amt);
            is.setCount(rem--); // todo -= rem
            ret += rem;
            amt -= rem;
            if (is.getCount() <= 0)
                it.remove();
            if (amt <= 0)
                break;
        }
        return ret;
    }

    public Collection<ItemStack> getItems() {
        return Collections.unmodifiableCollection(data);
    }

    public void removeItem(ItemStack is) {
        data.remove(is);
    }

    public void clearEmpties() {
        Iterator<ItemStack> it = data.iterator();
        while (it.hasNext()) {
            ItemStack is = it.next();
            if (is.getCount() <= 0)
                it.remove();
        }
    }

}
