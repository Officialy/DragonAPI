package reika.dragonapi.libraries.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import reika.dragonapi.base.BlockCustomLeaf;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.maps.BlockMap;
import reika.dragonapi.interfaces.registry.TreeType;
import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.modregistry.ModWoodList;

import java.util.ArrayList;
import java.util.Arrays;

public enum ReikaTreeHelper implements TreeType {

    OAK(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING),
    SPRUCE(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING),
    BIRCH(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING),
    JUNGLE(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING),
    ACACIA(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING),
    DARKOAK(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING);

    public static final ReikaTreeHelper[] treeList = ReikaTreeHelper.values();
    public static final int TREE_MIN_LOG = 2;
    public static final int TREE_MIN_LEAF = 5;
    private static final BlockMap<ReikaTreeHelper> logMappings = new BlockMap<>();
    private static final BlockMap<ReikaTreeHelper> leafMappings = new BlockMap<>();
    private static final BlockMap<ReikaTreeHelper> saplingMappings = new BlockMap<>();

    static {
        for (ReikaTreeHelper w : treeList) {
            Block id = w.log;
            Block leaf = w.leaf;
            Block sapling = w.sapling;
            logMappings.put(id, w);
            leafMappings.put(leaf, w);
            saplingMappings.put(sapling, w);
        }
    }

    private Block leaf;
    private Block log;
    private Block sapling;

    ReikaTreeHelper(Block wood, Block leaves, Block tree) {
        log = wood;
        leaf = leaves;
        sapling = tree;
    }

    public static ReikaTreeHelper getTree(Block id) {
        return logMappings.get(id);
    }

    public static ReikaTreeHelper getTree(ItemStack wood) {
        return getTree(Block.byItem(wood.getItem()));
    }

    public static ReikaTreeHelper getTreeFromLeaf(Block id) {
        return leafMappings.get(id);
    }

    public static ReikaTreeHelper getTreeFromLeaf(ItemStack leaf) {
        return getTreeFromLeaf(Block.byItem(leaf.getItem()));
    }

    public static ReikaTreeHelper getTreeFromSapling(Block id) {
        return saplingMappings.get(id);
    }

    public static ReikaTreeHelper getTreeFromSapling(ItemStack sapling) {
        return getTreeFromSapling(Block.byItem(sapling.getItem()));
    }

    public static boolean isNaturalLeaf(Level world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();

        if (b instanceof BlockCustomLeaf)
            return ((BlockCustomLeaf) b).isNatural();
        ModWoodList mod = ModWoodList.getModWoodFromLeaf(b);
        if (mod != null) {
            return mod.isNaturalLeaf(world, pos);
        }
        if (b instanceof LeavesBlock)
            return true;
        return true;
    }

    public boolean isTree(ItemStack wood) {
        return getTree(wood) != null;
    }

    public boolean isTree(Block id) {
        return getTree(id) != null;
    }

    public boolean isTreeLeaf(ItemStack leaf) {
        return getTreeFromLeaf(leaf) != null;
    }

    public boolean isTreeLeaf(Block id) {
        return getTreeFromLeaf(id) != null;
    }

    public boolean isTreeSapling(ItemStack sapling) {
        return getTreeFromSapling(sapling) != null;
    }

    public boolean isTreeSapling(Block id) {
        return getTreeFromSapling(id) != null;
    }

    public BlockKey getLog() {
        return new BlockKey(log);
    }

    public BlockKey getLeaf() {
        return new BlockKey(leaf);
    }

    public BlockKey getSapling() {
        return new BlockKey(sapling);
    }

    public BlockKey getDamagedLog(int dmg) {
        return new BlockKey(log);
    }

    public BlockKey getDamagedLeaf(int dmg) {
        return new BlockKey(leaf);
    }

    public String getName() {
        return ReikaStringParser.capFirstChar(this.name());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(" (LOG " + log + ";");
        sb.append(" ");
        sb.append("LEAF " + leaf + ";");
        sb.append(" ");
        sb.append("SAPLING " + sapling);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public BlockKey getItem() {
        return new BlockKey(log);
    }

    @Override
    public BlockKey getBasicLeaf() {
        return new BlockKey(leaf);
    }

    @Override
    public Block getLogID() {
        return log;
    }

    @Override
    public Block getLeafID() {
        return leaf;
    }

    @Override
    public Block getSaplingID() {
        return sapling;
    }


    @Override
    public boolean canBePlacedSideways() {
        return true;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public BlockBox getTypicalMaximumSize() {
        return switch (this) {
            case ACACIA -> BlockBox.origin().expand(9, 12, 9);
            case BIRCH -> BlockBox.origin().expand(5, 9, 5);
            case DARKOAK -> BlockBox.origin().expand(6, 11, 6);
            case JUNGLE -> BlockBox.origin().expand(10, 50, 10);
            case OAK -> BlockBox.origin().expand(15, 25, 15);
            case SPRUCE -> BlockBox.origin().expand(9, 40, 9);
        };
    }

}
