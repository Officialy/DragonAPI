package reika.dragonapi.instantiable.data.immutable;

import com.google.common.base.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.interfaces.BlockCheck;
import reika.dragonapi.interfaces.registry.TileEnum;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.Locale;
import java.util.Objects;

public final class BlockKey implements BlockCheck, Comparable<BlockKey> {

    public static final BlockKey AIR = new BlockKey(Blocks.AIR.defaultBlockState());
    public BlockState blockID;

    public BlockKey(Block b) {
        this(b.defaultBlockState());
    }

    public BlockKey(BlockState b) {
        blockID = b;
        if (b == null)
            throw new MisuseException("Cannot create a BlockKey from a null block!");
    }

    public BlockKey(ItemStack is) {
        this(Block.byItem(is.getItem()).defaultBlockState());
        if (is.getItem() == null)
            throw new MisuseException("Cannot create a BlockKey from a null item!");
        Block b = Block.byItem(is.getItem());
        if (b == null)
            throw new MisuseException("Cannot create a BlockKey with an item with no block!");
    }

    public BlockKey(TileEnum m) {
        blockID = m.getBlockState();
    }

    public static BlockKey getAt(BlockGetter world, BlockPos pos) {
        return new BlockKey(world.getBlockState(pos));
    }

    @Override
    public int hashCode() {
        return blockID.hashCode()/* + metadata << 24*/;
    }

    @Override
    public boolean equals(Object o) {
//        ReikaJavaLibrary.pConsole(this+" & "+o);
        if (o instanceof BlockKey b) {
            return b.blockID == blockID;
        }
        return false;
    }

    @Override
    public String toString() {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(blockID.getBlock()).getNamespace()).toLowerCase(Locale.ROOT);
    }

    public ItemStack asItemStack() {
        return new ItemStack(blockID.getBlock().asItem(), 1);
    }

    public ItemStack getDisplay() {
        return this.asItemStack();
    }

    public boolean match(BlockState b) {
        return b == blockID;
    }

    public boolean matchInWorld(Level world, BlockPos pos) {
        return this.match(world.getBlockState(pos));
    }

    public void place(Level world, BlockPos pos) {
        this.place(world, pos, 3);
    }

    @Override
    public void place(Level world, BlockPos pos, int flags) {
        world.setBlock(pos, blockID, flags);
    }

    @Override
    public BlockKey asBlockKey() {
        return this;
    }


/*  todo  public void saveAdditional(String tag, CompoundTag NBT) {
        CompoundTag dat = new CompoundTag();
        dat.putString("id", Block.blockRegistry.getNameForObject(blockID));
        NBT.put(tag, dat);
    }

    public static BlockKey load(String s, CompoundTag tag) {
        CompoundTag dat = tag.getCompound(s);
        String id = dat.getString("id");
        BlockState b = Strings.isNullOrEmpty(id) ? null : Block.getBlockFromName(id);
        return b != null ? new BlockKey(b) : null;
    }*/

    @Override
    public boolean match(BlockCheck bc) {
        return bc instanceof BlockKey && bc.equals(this);
    }

    public int getBlockID() {
        return Block.getId(blockID);
    }

    @Override
    public int compareTo(BlockKey o) {
        return 100000 * Integer.compare(this.getBlockID(), o.getBlockID());
    }

}
