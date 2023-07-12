package reika.dragonapi.auxiliary;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.base.BlockTieredResource;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.collections.RelativePositionList;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.interfaces.block.MachineRegistryBlock;
import reika.dragonapi.interfaces.registry.TreeType;
import reika.dragonapi.libraries.ReikaInventoryHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.modregistry.ModWoodList;

import java.util.*;

@Mod.EventBusSubscriber(modid = DragonAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProgressiveRecursiveBreaker implements TickRegistry.TickHandler {

    public static final ProgressiveRecursiveBreaker instance = new ProgressiveRecursiveBreaker();

    private static final int MAX_DEPTH = 4;
    private static final int MAX_SIZE = 32000;
    private static final Direction[] dirs = Direction.values();
    private final MultiMap<ResourceKey<Level>, ProgressiveBreaker> breakers = new MultiMap<net.minecraft.resources.ResourceKey<Level>, ProgressiveBreaker>();

    private ProgressiveRecursiveBreaker() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void unloadWorld(LevelEvent.Unload evt) {
        breakers.clear();
    }

    public void addBlockPos(Level world, BlockPos pos) {
        if (world.isClientSide())
            return;
        this.addBlockPos(world, pos, Integer.MAX_VALUE);
    }

    public void addBlockPos(Level world, ProgressiveBreaker b) {
        if (world.isClientSide())
            return;
        breakers.addValue(world.dimension(), b);
    }

    public void addBlockPos(Level world, BlockPos pos, TreeType tree) {
        if (world.isClientSide())
            return;
        ProgressiveBreaker b = this.getTreeBreaker(world, pos, tree);
        breakers.addValue(world.dimension(), b);
    }

    public ProgressiveBreaker getTreeBreaker(Level world, BlockPos pos, TreeType tree) {
        if (world.isClientSide())
            return null;
        Block log = tree.getLogID();
        Block leaf = tree.getLeafID();

        ArrayList<BlockKey> ids = new ArrayList<>();

        ids.add(new BlockKey(log));
        ids.add(new BlockKey(leaf));
        int depth = 30;
        if (tree == ModWoodList.SEQUOIA)
            depth = 350;
        if (tree == ModWoodList.TWILIGHTOAK)
            depth = 200;
        if (tree == ModWoodList.DARKWOOD)
            depth = 32;
        if (tree == ModWoodList.GIANTPINKTREE)
            depth = 180;
        ProgressiveBreaker b = new ProgressiveBreaker(world, pos, depth, ids);
        b.extraSpread = true;
        b.bounds = tree.getTypicalMaximumSize().offset(pos);
        return b;
    }

    public void addBlockPos(Level world, BlockPos pos, List<BlockKey> ids) {
        if (world.isClientSide())
            return;
        breakers.addValue(world.dimension(), new ProgressiveBreaker(world, pos, Integer.MAX_VALUE, ids));
    }

    public void addBlockPos(Level world, BlockPos pos, int maxDepth) {
        if (world.isClientSide())
            return;
        breakers.addValue(world.dimension(), new ProgressiveBreaker(world, pos, maxDepth));
    }

    public ProgressiveBreaker addBlockPosWithReturn(Level world, BlockPos pos, int maxDepth) {
        if (world.isClientSide())
            return null;
        ProgressiveBreaker b = new ProgressiveBreaker(world, pos, maxDepth);
        breakers.addValue(world.dimension(), b);
        return b;
    }

    @Override
    public void tick(TickRegistry.TickType type, Object... tickData) {
        Level world = (Level) tickData[0];
        Collection<ProgressiveBreaker> li = breakers.get(world.dimension());
        if (li != null) {
            if (!world.isClientSide()) {
                Iterator<ProgressiveBreaker> it = li.iterator();
                while (it.hasNext()) {
                    ProgressiveBreaker b = it.next();
                    if (b.isDone) {
                        it.remove();
                    } else {
                        b.tick();
                    }
                }
            } else {
                li.clear();
            }
        }
    }

    @Override
    public EnumSet<TickRegistry.TickType> getType() {
        return EnumSet.of(TickRegistry.TickType.WORLD);
    }

    @Override
    public String getLabel() {
        return "Progressive Recursive Breaker";
    }

    @Override
    public boolean canFire(TickEvent.Phase p) {
        return p == TickEvent.Phase.START;
    }

    public void clearBreakers() {
        breakers.clear();
    }

    public interface BreakerCallback {

        boolean canBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id);

        void onPreBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id);

        void onPostBreak(ProgressiveBreaker b, Level world, BlockPos pos, Block id);

        void onFinish(ProgressiveBreaker b);

    }

    public static final class ProgressiveBreaker {
        public final HashSet<BlockKey> passThrough = new HashSet<>();
        //public final BlockMap<BlockKey> looseMatches = new BlockMap();
        public final int originX;
        public final int originY;
        public final int originZ;
        private final BlockArray start = new BlockArray();
        private final Level world;
        private final int maxDepth;
        private final HashSet<BlockKey> ids = new HashSet<>();
        private final Collection<BlockPos> path = new HashSet<>();
        private final Collection<BlockPos> excluded = new HashSet<>();
        public boolean extraSpread = false;
        public int tickRate = 1;
        public int fortune = 0;
        public boolean silkTouch = false;
        public boolean drops = true;
        public ItemStackHandler dropInventory = new ItemStackHandler();
        public Player player;
        public float hungerFactor = 1;
        public BlockBox bounds = BlockBox.infinity();
        public BreakerCallback call;
        public boolean isOmni = false;
        public boolean pathTracking = false;
        public boolean dropFluids = true;
        public boolean breakAir = false;
        public boolean taxiCabDistance = false;
        public boolean causeUpdates = true;
        public boolean doBreak = true;
        private int depth = 0;
        private boolean isDone = false;
        private int tick;
        private boolean isBlacklist = false;

        private ProgressiveBreaker(Level world, BlockPos pos, int depth, List<BlockKey> ids) {
            this.world = world;
            start.addBlockCoordinate(pos);
            maxDepth = depth;
            this.ids.addAll(ids);
            originX = pos.getX();
            originY = pos.getY();
            originZ = pos.getZ();
        }

        private ProgressiveBreaker(Level world, BlockPos pos, int depth, BlockKey... ids) {
            this.world = world;
            start.addBlockCoordinate(pos);
            maxDepth = depth;
            Collections.addAll(this.ids, ids);
            originX = pos.getX();
            originY = pos.getY();
            originZ = pos.getZ();
        }

        private ProgressiveBreaker(Level world, BlockPos pos, Block id, int depth) {
            this.world = world;
            start.addBlockCoordinate(pos);
            maxDepth = depth;
            ids.add(new BlockKey(id));
            originX = pos.getX();
            originY = pos.getY();
            originZ = pos.getZ();
        }

        private ProgressiveBreaker(Level world, BlockPos pos, int depth) {
            this(world, pos, world.getBlockState(pos).getBlock(), depth);
        }

        public void addBlock(BlockKey bk) {
            ids.add(bk);
        }

        public void setBlacklist(BlockKey... keys) {
            ids.clear();
            isBlacklist = true;
            Collections.addAll(ids, keys);
        }

        public void exclude(int x, int y, int z) {
            excluded.add(new BlockPos(x, y, z));
        }

        public void exclude(BlockPos c) {
            excluded.add(c);
        }

        private void tick() {
            tick++;
            if (tick < tickRate)
                return;
            tick = 0;
            if (depth < maxDepth) {
                BlockArray next = new BlockArray();
                for (int i = 0; i < start.getSize() && !isDone; i++) {
                    BlockPos c = start.getNthBlock(i);
                    if (excluded.contains(c))
                        continue;
                    Block b = world.getBlockState(c).getBlock();
                    if (b == Blocks.AIR && !breakAir)
                        continue;
                    if (call != null && !call.canBreak(this, world, c, b))
                        continue;
                    for (int k = 0; k < 6; k++) {
                        Direction dir = dirs[k];
                        int dx = c.getX() + dir.getStepX();
                        int dy = c.getY() + dir.getStepY();
                        int dz = c.getZ() + dir.getStepZ();
                        if (this.canSpreadTo(world, new BlockPos(dx, dy, dz))) {
                            next.addBlockCoordinate(new BlockPos(dx, dy, dz));
                        }
                    }
                    if (extraSpread) {
                        for (int n = 0; n < RelativePositionList.cornerDirections.getSize(); n++) {
                            BlockPos d = RelativePositionList.cornerDirections.getNthPosition(c, n);
                            int dx = d.getX();
                            int dy = d.getY();
                            int dz = d.getZ();
                            if (this.canSpreadTo(world, new BlockPos(dx, dy, dz))) {
                                next.addBlockCoordinate(new BlockPos(dx, dy, dz));
                            }
                        }
                    }
                    if (pathTracking)
                        path.add(c);
                    this.dropBlock(world, c);
                }
                start.clear();
                for (int i = 0; i < next.getSize() && i < MAX_SIZE; i++) {
                    BlockPos c = next.getNthBlock(i);
                    int x = c.getX();
                    int y = c.getY();
                    int z = c.getZ();
                    start.addBlockCoordinate(new BlockPos(x, y, z));
                }
                depth++;
                if (start.isEmpty())
                    this.finish();
            } else {
                this.finish();
            }
        }

        private void finish() {
            isDone = true;
            if (call != null) {
                call.onFinish(this);
            }
        }

        public void terminate() {
            this.finish();
        }

        private boolean canSpreadTo(Level world, BlockPos pos) {
            if (taxiCabDistance && Math.abs(pos.getX() - originX) + Math.abs(pos.getY() - originY) + Math.abs(pos.getZ() - originZ) > maxDepth)
                return false;
            BlockPos c = new BlockPos(pos);
            if (!excluded.isEmpty() && excluded.contains(c))
                return false;
            if (pathTracking && path.contains(c))
                return false;
            if (!bounds.isBlockInside(pos))
                return false;
            Block id = world.getBlockState(pos).getBlock();

            if (id == Blocks.AIR && !breakAir)
                return false;
            if (!isOmni) {
                BlockKey bk = new BlockKey(id);
                return ids.contains(bk) || passThrough.contains(bk);
            }
            return player == null || (!world.isClientSide() && ReikaPlayerAPI.playerCanBreakAt((ServerLevel) world, pos, (ServerPlayer) player));
        }

        private void dropBlock(Level world, BlockPos pos) {
            Block id = world.getBlockState(pos).getBlock();

            boolean pass = !doBreak || passThrough.contains(new BlockKey(id));
            DragonAPI.LOGGER.info("pass=" + pass);
            if (!pass && id != Blocks.AIR) {
                if (drops) {
                    ArrayList<ItemStack> drops = new ArrayList<>();
                    if (id instanceof BlockTieredResource bt) {
                        if (player != null) {
                            if (bt.isPlayerSufficientTier(world, pos, player)) {
                                drops.addAll(bt.getHarvestResources(world, pos, fortune, player));
                            } else {
                                drops.addAll(bt.getNoHarvestResources(world, pos, fortune, player));
                            }
                        }
                    } else if (id instanceof MachineRegistryBlock) {
                        drops.add(((MachineRegistryBlock) id).getMachine(world, pos).getBlockState().getBlock().asItem().getDefaultInstance());
                    } else {
                        if (silkTouch && id.canHarvestBlock(id.defaultBlockState(), world, pos, player)) {
                            ItemStack silk = ReikaBlockHelper.getSilkTouch(world, pos, id, player, dropFluids);
                            if (silk != null)
                                drops.add(silk);
                            else
                                drops.addAll(ReikaWorldHelper.getDropsAt(world, pos, fortune, player));
                        } else
                            drops.addAll(ReikaWorldHelper.getDropsAt(world, pos, fortune, player));
                    }
                    for (ItemStack is : drops) {
                        boolean flag = false;
                        if (dropInventory != null) {
                            if (MinecraftForge.EVENT_BUS.post(new EntityItemPickupEvent(Minecraft.getInstance().player, new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, is)))) {
                                continue;
                            }
                            flag = ReikaInventoryHelper.addToIInv(is, dropInventory);
                        }
                        if (!flag) {
                            ReikaItemHelper.dropItem(world, pos.getX(), pos.getY(), pos.getZ(), is);
                        }
                    }
                }
                if (ReikaBlockHelper.isLiquid(id.defaultBlockState())) {
                    if (id.defaultBlockState().getMaterial() == Material.WATER) {
                        world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 1, 1, false);
                    } else {
                        world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 1, 1, false);
                    }
                } else {
                    ReikaSoundHelper.playBreakSound(world, pos.getX(), pos.getY(), pos.getZ(), id);
                }
            }
            if (call != null)
                call.onPreBreak(this, world, pos, id);
            if (!pass && id != Blocks.AIR) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 0, causeUpdates ? 3 : 2);
            }
            if (!pass && causeUpdates)
                world.sendBlockUpdated(pos, id.defaultBlockState(), id.defaultBlockState(), 3); //todo check
//                world.markBlockForUpdate(pos);
            if (!pass && player != null) {
                player.awardStat(Stats.BLOCK_MINED.get(id), 1);
                player.causeFoodExhaustion(0.025F * hungerFactor);
            }
            if (call != null)
                call.onPostBreak(this, world, pos, id);
        }
    }
}
