package reika.dragonapi.instantiable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.level.ReikaBlockHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

import java.util.*;

public class RayTracer {


    private double originX;
    private double originY;
    private double originZ;
    private double targetX;
    private double targetY;
    private double targetZ;

    public boolean airOnly = false;
    public boolean softBlocksOnly = false;
    public boolean allowFluids = true;
    public boolean uniDirectionalChecks = false;

    public boolean cacheBlockRay = false;

    private final ArrayList<BlockKey> forbiddenBlocks = new ArrayList<>();
    private final ArrayList<BlockKey> allowedBlocks = new ArrayList<>();
    private final ArrayList<BlockKey> allowedOneTimeBlocks = new ArrayList<>();

    private final HashSet<BlockPos> blockRay = new HashSet<>();

    private static final Collection<BlockKey> visuallyTransparent = new HashSet<>();
    private static boolean loadedTransparent = false;

    public RayTracer(double x1, double y1, double z1, double x2, double y2, double z2) {
        originX = x1;
        originY = y1;
        originZ = z1;
        targetX = x2;
        targetY = y2;
        targetZ = z2;
    }

    public RayTracer setOrigins(double x1, double y1, double z1, double x2, double y2, double z2) {
        originX = x1;
        originY = y1;
        originZ = z1;
        targetX = x2;
        targetY = y2;
        targetZ = z2;
        blockRay.clear();
        return this;
    }

    public RayTracer offset(double dx, double dy, double dz) {
        return this.offset(dx, dy, dz, dx, dy, dz);
    }

    public RayTracer offset(double dx1, double dy1, double dz1, double dx2, double dy2, double dz2) {
        originX += dx1;
        originY += dy1;
        originZ += dz1;
        targetX += dx2;
        targetY += dy2;
        targetZ += dz2;
        blockRay.clear();
        return this;
    }

    public RayTracer addOpaqueBlock(Block b) {
        forbiddenBlocks.add(new BlockKey(b));
        return this;
    }

    public RayTracer addTransparentBlock(Block b) {
        allowedBlocks.add(new BlockKey(b));
        return this;
    }

    public RayTracer addOneTimeIgnoredBlock(Block b) {
        allowedOneTimeBlocks.add(new BlockKey(b));
        return this;
    }

    public boolean isClearLineOfSight(Level world) {
        Vec3 vec1 = new Vec3(originX, originY, originZ);
        Vec3 vec2 = new Vec3(targetX, targetY, targetZ);
        if (uniDirectionalChecks && new DecimalPosition(vec1).hashCode() < new DecimalPosition(vec2).hashCode()) {
            Vec3 vec = vec1;
            vec1 = vec2;
            vec2 = vec;
        }
        Vec3 ray = ReikaVectorHelper.subtract(vec1, vec2);
        double dx = vec2.x-vec1.x;
        double dy = vec2.y-vec1.y;
        double dz = vec2.z-vec1.z;
        double dd = ReikaMathLibrary.py3d(dx, dy, dz);
        for (double d = 0.25; d <= dd; d += 0.25) {
            Vec3 vec0 = ReikaVectorHelper.scaleVector(ray, d);
            Vec3 vec = ReikaVectorHelper.scaleVector(ray, d-0.25);

            vec0.add(vec1);
            vec.add(vec1);

            if (cacheBlockRay) {
                blockRay.add(BlockPos.containing(vec));
                blockRay.add(BlockPos.containing(vec0)); //todo test
            }

            HitResult mov = world.clip(new ClipContext(vec, vec0, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null));
            if (mov != null) {
                if (mov.getType() == HitResult.Type.BLOCK) {
                    int bx = (int) mov.getLocation().x();
                    int by = (int) mov.getLocation().y();
                    int bz = (int) mov.getLocation().z();
                    if (this.isNonTerminal(bx, by, bz)) {
                        if (this.isDisallowedBlock(world, bx, by, bz)) {
                            //ReikaJavaLibrary.pConsole(mov+":"+world.getBlock(bx, by, bz), Dist.DEDICATED_SERVER);
                            allowedOneTimeBlocks.clear();
                            return false;
                        }
                    }
                }
            }
        }
        allowedOneTimeBlocks.clear();
        return true;
    }

    public Set<BlockPos> getRayBlocks() {
        return Collections.unmodifiableSet(blockRay);
    }

    private boolean isNonTerminal(int x, int y, int z) {
        if (x == Mth.floor(originX) && y == Mth.floor(originY) && z == Mth.floor(originZ))
            return false;
        return x != Mth.floor(targetX) || y != Mth.floor(targetY) || z != Mth.floor(targetZ);
    }

    private boolean isDisallowedBlock(Level world, int x, int y, int z) {
        BlockState b = world.getBlockState(new BlockPos(x, y, z));
        BlockKey key = new BlockKey(b);
        if (airOnly && b != Blocks.AIR.defaultBlockState())
            return true;
        if (allowedBlocks.contains(key))
            return false;
        if (allowedOneTimeBlocks.contains(key))
            return false;
        if (forbiddenBlocks.contains(key))
            return true;
        if (!allowFluids && ReikaBlockHelper.isLiquid(b))
            return true;
        return !ReikaWorldHelper.softBlocks(world, new BlockPos(x, y, z)) || (softBlocksOnly && ReikaBlockHelper.isCollideable(world, new BlockPos(x, y, z)));
    }

    public boolean isBlockPassable(Level world, int x, int y, int z) {
        return !this.isDisallowedBlock(world, x, y, z);
    }

    public static void addVisuallyTransparentBlock(Block b) {
        visuallyTransparent.add(new BlockKey(b));
    }

    static {
        addVisuallyTransparentBlock(Blocks.GLASS);
        addVisuallyTransparentBlock(Blocks.ICE);
        addVisuallyTransparentBlock(Blocks.GLASS_PANE);
        addVisuallyTransparentBlock(Blocks.IRON_BARS);
//    todo    addVisuallyTransparentBlock(Blocks.FENCE);
        addVisuallyTransparentBlock(Blocks.NETHER_BRICK_FENCE);
        addVisuallyTransparentBlock(Blocks.SPAWNER);
//   todo     addVisuallyTransparentBlock(Blocks.LEAVES);
//        addVisuallyTransparentBlock(Blocks.LEAVES2);
//        addVisuallyTransparentBlock(Blocks.TALLGRASS);
    }

    private static void loadLastTransparent() {
        if (loadedTransparent)
            return;
        loadedTransparent = true;
//todo        if (ModList.EXTRAUTILS.isLoaded()) {
//            if (ExtraUtilsHandler.getInstance().deco2ID != null) {
//                addVisuallyTransparentBlock(ExtraUtilsHandler.getInstance().deco2ID, 1);
//                addVisuallyTransparentBlock(ExtraUtilsHandler.getInstance().deco2ID, 2);
//                addVisuallyTransparentBlock(ExtraUtilsHandler.getInstance().deco2ID, 4);
//            }
//        }
//        if (ModList.TINKERER.isLoaded() && TinkerBlockHandler.getInstance().clearGlassID != null) {
//            addVisuallyTransparentBlock(TinkerBlockHandler.getInstance().clearGlassID);
//        }
    }

    public static Collection<BlockKey> getTransparentBlocks() {
        return Collections.unmodifiableCollection(visuallyTransparent);
    }

    public static RayTracer getVisualLOS() {
        RayTracer trace = new RayTracer(0, 0, 0, 0, 0, 0);
        loadLastTransparent();

        for (BlockKey bk : visuallyTransparent) {
                trace.addTransparentBlock(bk.blockID.getBlock());
        }
        trace.allowFluids = true;

        return trace;
    }

    public static RayTracerWithCache<?> getVisualLOSForRenderCulling() {
        RayTracer ret = getVisualLOS();
        return new RayTracerWithCache<>(ret);
    }

    public static <V> RayTracerWithCache<?> getMultipointVisualLOSForRenderCulling(MultipointChecker<V> mc) {
        RayTracer ret = getVisualLOS();
        return new MultipointRayTracerWithCache<>(ret, mc);
    }

    public double getLength() {
        return ReikaMathLibrary.py3d(originX-targetX, originY-targetY, originZ-targetZ);
    }

    private static class MultipointRayTracerWithCache<V> extends RayTracerWithCache<V> {

        private final MultipointChecker<V> checker;

        private MultipointRayTracerWithCache(RayTracer ret, MultipointChecker<V> mc) {
            super(ret);
            checker = mc;
        }

        @Override
        protected boolean getLOS(V focus, Level world) {
            return checker.isClearLineOfSight(focus, trace, world);
        }

    }

    public interface MultipointChecker<V> {

        boolean isClearLineOfSight(V focus, RayTracer trace, Level world);

    }

    public static class RayTracerWithCache<V> {

        protected final RayTracer trace;

        private Boolean cachedRaytrace;
        private long lastTraceTick;
        private int lastTraceTileHash;

        private RayTracerWithCache(RayTracer ret) {
            trace = ret;
        }

        public final void setOrigins(double x1, double y1, double z1, double x2, double y2, double z2) {
            trace.setOrigins(x1, y1, z1, x2, y2, z2);
        }

        public final boolean isClearLineOfSight(Entity e) {
            return this.isClearLineOfSight((V)e, e.level());
        }

        public final boolean isClearLineOfSight(BlockEntity e) {
            return this.isClearLineOfSight((V)e, e.getLevel());
        }

        public final boolean isClearLineOfSight(V focus, Level world) {
            this.update(focus, world);
            if (cachedRaytrace == null)
                cachedRaytrace = this.getLOS(focus, world);
            return cachedRaytrace.booleanValue();
        }

        protected boolean getLOS(V focus, Level world) {
            return trace.isClearLineOfSight(world);
        }
        /*
        public final void update(BlockEntity te) {
            this.update(te, te.worldObj);
        }
        public final void update(Entity te) {
            this.update(te, te.worldObj);
        }
         */
        private void update(Object focus, Level world) {
            if (cachedRaytrace == null)
                return;
            long time = world.getGameTime();
            int hash = System.identityHashCode(focus);
            if (time-lastTraceTick > 5 || hash != lastTraceTileHash) {
                cachedRaytrace = null;
                lastTraceTileHash = hash;
                lastTraceTick = time;
            }
        }

    }

}
