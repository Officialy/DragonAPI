package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.registry.TreeType;
import reika.dragonapi.libraries.registry.ReikaTreeHelper;
import reika.dragonapi.modregistry.ModWoodList;

import java.util.HashSet;
import java.util.Iterator;

public class TreeReader  extends BlockArray {

    private int leafCount;
    private int logCount;

    private TreeType tree;
//    private ReikaDyeHelper dyeTree;

    private final Block dyeLeafID;
    private final Block rainbowLeafID;
    private final Block rainbowSaplingID;
    private final Block dyeSaplingID;

    private boolean isDyeTree = false;
    private boolean isRainbowTree = false;
    private boolean stopIfValid = false;

    public TreeReader() {
        super();
//        if (ModList.CHROMATICRAFT.isLoaded()) {
//            DyeTreeAPI api = ChromatiAPI.getAPI().trees();
//            dyeLeafID = api.getDyeLeaf(true);
//            rainbowLeafID = api.getRainbowLeaf();
//            rainbowSaplingID = api.getRainbowSapling();
//            dyeSaplingID = api.getDyeSapling();
//        }
//        else {
            dyeLeafID = null;
            rainbowLeafID = null;
            rainbowSaplingID = null;
            dyeSaplingID = null;
//        }
    }

    public TreeReader setStopIfValid() {
        stopIfValid = true;
        return this;
    }

    public boolean isDyeTree() {
        return isDyeTree;
    }

    public boolean isRainbowTree() {
        return isRainbowTree;
    }

    public void addTree(BlockGetter world, BlockPos pos) {
        HashSet<BlockPos> search = new HashSet<>();
        HashSet<BlockPos> failed = new HashSet<>();
        HashSet<BlockPos> next = new HashSet<>();
        int iterations = 0;
        this.validateAndAdd(world, pos, search, failed);
        while (!search.isEmpty() && iterations < maxDepth) {
            iterations++;
            Iterator<BlockPos> it = search.iterator();
            while (it.hasNext()) {
                BlockPos c = it.next();
                if (bounds.isBlockInside(c)) {
                    this.addBlockCoordinate(c);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dy == 0 && dz == 0)
                                    continue;
                                BlockPos c2 = c.offset(dx, dy, dz);
                                if (!search.contains(c2) && !next.contains(c2) && !this.containsKey(c2) && !failed.contains(c2)) {
                                    this.validateAndAdd(world, c2, next, failed);
                                }
                            }
                        }
                    }
                }
                it.remove();
            }

            if (stopIfValid && this.isValidTree())
                return;
            search.addAll(next);
            next.clear();
        }
    }

    private boolean isTree(BlockGetter world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        if (tree != null && tree.getLogID() == b) {
            logCount++;
            return true;
        }
        else if (!isRainbowTree && b == dyeLeafID) {
            isDyeTree = true;
            leafCount++;
            maxDepth = 12;
            return true;
        }
        else if (!isDyeTree && b == rainbowLeafID) {
            isRainbowTree = true;
            leafCount++;
            maxDepth = 36;
            return true;
        }
        else if (tree != null && !isDyeTree && !isRainbowTree && b == tree.getLeafID()) {
            leafCount++;
            return true;
        }
        return false;
    }

    private void validateAndAdd(BlockGetter world, BlockPos pos, HashSet<BlockPos> search, HashSet<BlockPos> failed) {
        BlockPos c = new BlockPos(pos);
        if (this.isTree(world, pos)) {
            search.add(c);
        }
        else {
            failed.add(c);
        }
    }

    public int getNumberLeaves() {
        return leafCount;
    }

    public int getNumberLogs() {
        return logCount;
    }

    public void reset() {
        logCount = 0;
        leafCount = 0;
        tree = null;
        isDyeTree = false;
    }

    public void setTree(TreeType tree) {
        this.tree = tree;
        if (tree != null) {
            maxDepth = getMaxDepthFromTreeType(tree);
        }
    }

    public static int getMaxDepthFromTreeType(TreeType tree) {
        if (tree instanceof ReikaTreeHelper) {
            return switch ((ReikaTreeHelper) tree) {
                case ACACIA -> 12;
                case BIRCH -> 6;
                case DARKOAK -> 12;
                case JUNGLE -> 36;
                case OAK -> 18;
                case SPRUCE -> 48;
                default -> 12;
            };
        }
        if (tree instanceof ModWoodList) {
            return switch ((ModWoodList) tree) {
                case IRONWOOD -> 60;
                case SEQUOIA -> 200;
                case REDWOOD -> 130;
                case DARKWOOD -> 80;
                case SACRED -> 160;
                case CANOPY -> 40;
                case TWILIGHTOAK -> 200;
                case MANGROVE -> 30;
                case SAKURA -> 24;
                case GREATWOOD, SILVERWOOD -> 28;
                case MFRRUBBER -> 80;
                case LIGHTED -> 15;
                case FIR -> 50;
                case PINKBIRCH -> 18;
                case GIANTPINKTREE -> 160;
                case REDJUNGLE -> 24;
                default -> 12;
            };
        }
        return 12;
    }

    public BlockKey getSapling() {
        if (isDyeTree)
            return new BlockKey(dyeSaplingID);
        if (isRainbowTree)
            return new BlockKey(rainbowSaplingID);
        return tree != null && tree.getSaplingID() != null ? new BlockKey(tree.getSaplingID()) : null;
    }

    @Override
    public BlockPos getNextAndMoveOn() {
        BlockPos next = super.getNextAndMoveOn();
        if (this.isEmpty());
//         this.reset();
        return next;
    }

    public boolean isValidTree() {
        if (tree == ModWoodList.SEQUOIA)
            return true;
        return this.getNumberLeaves() >= ReikaTreeHelper.TREE_MIN_LEAF && this.getNumberLogs() >= ReikaTreeHelper.TREE_MIN_LOG;
    }

    public TreeType getTreeType() {
        return tree;
    }

    @Override
    protected BlockArray instantiate() {
        return new TreeReader();
    }

    @Override
    public void copyTo(BlockArray cp) {
        TreeReader copy = (TreeReader)cp;

        copy.leafCount = leafCount;
        copy.logCount = logCount;

        copy.tree = tree;

        copy.isDyeTree = isDyeTree;
        copy.isRainbowTree = isRainbowTree;
    }

}
