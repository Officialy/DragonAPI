//package reika.dragonapi.instantiable.data.blockstruct;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import reika.dragonapi.instantiable.data.immutable.BlockKey;
//import reika.dragonapi.interfaces.BlockCheck;
//import reika.dragonapi.interfaces.registry.TileEnum;
//import reika.dragonapi.libraries.ReikaNBTHelper;
//import reika.dragonapi.libraries.java.ReikaJavaLibrary;
//
//import java.lang.ref.WeakReference;
//import java.util.HashMap;
//
//public class FilledBlockArray extends StructuredBlockArray {
//
//    private final HashMap<BlockPos, BlockCheck> data = new HashMap();
//    private final HashMap<BlockPos, BlockKey> placementOverrides = new HashMap();
//
//    public static boolean logMismatches;
//
//    public FilledBlockArray(Level world) {
//        super(world);
//    }
//
//    @Override
//    public void copyTo(BlockArray copy) {
//        super.copyTo(copy);
//        if (copy instanceof FilledBlockArray) {
//            ((FilledBlockArray)copy).data.putAll(data);
//            ((FilledBlockArray)copy).placementOverrides.putAll(placementOverrides);
//        }
//    }
//
//    public void loadBlock(int x, int y, int z) {
//        this.setBlock(x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
//    }
//
//    public void loadBlockTo(int x, int y, int z, int xt, int yt, int zt) {
//        this.setBlock(xt, yt, zt, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
//    }
//
//    public void setBlock(int x, int y, int z, Block id) {
//        this.setBlock(x, y, z , new BlockKey(id));
//    }
//
//    public void setBlock(int x, int y, int z, Block id, int meta) {
//        this.setBlock(x, y, z , new BlockKey(id, meta));
//    }
//
//    public void setTile(int x, int y, int z, Block id, int meta, BlockEntity te, String... tags) {
//        this.setBlock(x, y, z , new BasicBlockEntityCheck(id, meta, te, tags));
//    }
//
//	/*
//	public void setTile(int x, int y, int z, TileEnum tile, String... tags) {
//		this.setBlock(x, y, z , new BasicBlockEntityCheck(tile, tags));
//	}
//	 */
//
//    public void setFluid(int x, int y, int z, Fluid f) {
//        this.setFluid(x, y, z, f, true, true);
//    }
//
//    public void setFluid(int x, int y, int z, Fluid f, boolean needSource, boolean allowSource) {
//        super.addBlockCoordinate(x, y, z);
//        FluidCheck fc = new FluidCheck(f);
//        fc.needsSourceBlock = needSource;
//        fc.allowSourceBlock = allowSource;
//        data.put(new BlockPos(x, y, z), fc);
//    }
//
//    public void setBlock(int x, int y, int z, BlockCheck bk) {
//        super.addBlockCoordinate(x, y, z);
//        data.put(new BlockPos(x, y, z), bk);
//    }
//
//    public void setEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
//        super.addBlockCoordinate(x, y, z);
//        data.put(new BlockPos(x, y, z), new EmptyCheck(soft, nonsolid, exceptions));
//    }
//
//    public void addEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
//        super.addBlockCoordinate(x, y, z);
//        this.addBlockToCoord(new BlockPos(x, y, z), new EmptyCheck(soft, nonsolid, exceptions));
//    }
//
//    public void addBlock(int x, int y, int z, Block id) {
//        this.addBlock(x, y, z , new BlockKey(id));
//    }
//
//    public void addBlock(int x, int y, int z, Block id, int meta) {
//        this.addBlock(x, y, z , new BlockKey(id, meta));
//    }
//
//    public void addBlock(int x, int y, int z, BlockCheck b) {
//        super.addBlockCoordinate(x, y, z);
//        this.addBlockToCoord(new BlockPos(x, y, z), b);
//    }
//
//    private void addBlock(int x, int y, int z, BlockKey bk) {
//        super.addBlockCoordinate(x, y, z);
//        this.addBlockToCoord(new BlockPos(x, y, z), bk);
//    }
//
//    private void addBlockToCoord(BlockPos c, BlockCheck bk) {
//        BlockCheck bc = data.get(c);
//        if (bc == null || bc instanceof EmptyCheck) {
//            MultiKey mk = new MultiKey();
//            if (bc != null)
//                mk.add(bc);
//            mk.add(bk);
//            data.put(c, mk);
//            bc = mk;
//        }
//        else if (bc instanceof BlockKey) {
//            MultiKey mk = new MultiKey();
//            mk.add(bc);
//            mk.add(bk);
//            data.put(c, mk);
//        }
//        else {
//            ((MultiKey)bc).add(bk);
//        }
//    }
//
//    public void setPlacementOverride(int x, int y, int z, Block id, int meta) {
//        placementOverrides.put(new BlockPos(x, y, z), new BlockKey(id, meta));
//    }
//
//    private BlockCheck getBlockKey(int x, int y, int z) {
//        return data.get(new BlockPos(x, y, z));
//    }
//
//	/*
//	public Block getBlock(int x, int y, int z) {
//		return this.getBlockKey(x, y, z).blockID;
//	}
//	public int getBlockMetadata(int x, int y, int z) {
//		return Math.max(0, this.getBlockKey(x, y, z).metadata);
//	}
//	 */
//
//    @Override
//    public boolean addBlockCoordinate(int x, int y, int z) {
//        if (super.addBlockCoordinate(x, y, z)) {
//            data.put(new BlockPos(x, y, z), BlockKey.getAt(world, x, y, z));
//            return true;
//        }
//        return false;
//    }
//
//    public void place() {
//        this.placeExcept(null, 3);
//    }
//
//    public void place(int flags) {
//        this.placeExcept(null, flags);
//    }
//
//    public void placeExcept(BlockPos e, int flags) {
//        for (Entry<BlockPos, BlockCheck> et : data.entrySet()) {
//            //Block b = this.getBlock(x, y, z);
//            //int meta = this.getBlockMetadata(x, y, z);
//            //world.setBlock(x, y, z, b, meta, 3);
//            BlockPos c = et.getKey();
//            if (!c.equals(e)) {
//                BlockKey po = placementOverrides.get(c);
//                if (po != null) {
//                    po.place(world, c.xCoord, c.yCoord, c.zCoord, flags);
//                }
//                else {
//                    et.getValue().place(world, c.xCoord, c.yCoord, c.zCoord, flags);
//                }
//            }
//        }
//    }
//
//    public void placeExcept(int flags, PlacementExclusionHook h) {
//        for (Entry<BlockPos, BlockCheck> et : data.entrySet()) {
//            BlockPos c = et.getKey();
//            BlockCheck bc = et.getValue();
//            if (!h.skipPlacement(c, bc)) {
//                BlockKey po = placementOverrides.get(c);
//                if (po != null) {
//                    po.place(world, c.xCoord, c.yCoord, c.zCoord, flags);
//                }
//                else {
//                    bc.place(world, c.xCoord, c.yCoord, c.zCoord, flags);
//                }
//            }
//        }
//    }
//
//    public ItemStack getDisplayAt(int x, int y, int z) {
//        BlockCheck bk = this.getBlockKey(x, y, z);
//        return bk != null ? bk.getDisplay() : null;
//    }
//
//    public boolean hasBlockAt(int x, int y, int z, Block b) {
//        return this.hasBlockAt(x, y, z, b, -1);
//    }
//
//    public boolean hasBlockAt(int x, int y, int z, Block b, int meta) {
//        BlockCheck bc = this.getBlockKey(x, y, z);
//        return bc != null ? bc.match(b, meta) : false;
//    }
//
//    public boolean matchInWorld() {
//        return this.matchInWorld(null);
//    }
//
//    public boolean matchInWorld(BlockMatchFailCallback call) {
//        if (world.isClientSide())
//            return true;
//        for (BlockPos c : data.keySet()) {
//            int x = c.xCoord;
//            int y = c.yCoord;
//            int z = c.zCoord;
//            BlockCheck bk = this.getBlockKey(x, y, z);
//            if (!bk.matchInWorld(world, x, y, z)) {
//                if (logMismatches)
//                    ReikaJavaLibrary.pConsole(x+","+y+","+z+" > Wanted ["+bk.getClass().getSimpleName()+"] "+bk.asBlockKey().blockID.getLocalizedName()+":"+bk.asBlockKey().metadata+", found "+world.getBlock(x, y, z).getLocalizedName()+":"+world.getBlockMetadata(x, y, z));
//                //bk.place(world, x, y, z, 3);
//                //world.setBlock(x, y, z, Blocks.brick_block);
//                if (call != null)
//                    call.onBlockFailure(world, x, y, z, bk);
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public int countErrors() {
//        if (world.isClientSide())
//            return 0;
//        int ret = 0;
//        for (BlockPos c : data.keySet()) {
//            int x = c.xCoord;
//            int y = c.yCoord;
//            int z = c.zCoord;
//            BlockCheck bk = this.getBlockKey(x, y, z);
//            if (!bk.matchInWorld(world, x, y, z)) {
//                ret++;
//            }
//        }
//        return ret;
//    }
//
//    @Override
//    public BlockKey getBlockKeyAt(int x, int y, int z) {
//        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).asBlockKey() : null;
//    }
//
//    public boolean isMultiKey(int x, int y, int z) {
//        return data.get(new BlockPos(x, y, z)) instanceof MultiKey;
//    }
//
//    public MultiKey getMultiKeyAt(int x, int y, int z) {
//        if (!this.hasBlock(x, y, z))
//            return null;
//        BlockCheck b = data.get(new BlockPos(x, y, z));
//        return b instanceof MultiKey ? (MultiKey)b : null;
//    }
//
//    public ArrayList<BlockKey> getMultiListAt(int x, int y, int z) {
//        if (!this.hasBlock(x, y, z))
//            return null;
//        BlockCheck b = data.get(new BlockPos(x, y, z));
//        if (b instanceof MultiKey) {
//            ArrayList<BlockKey> li = new ArrayList<>();
//            for (BlockCheck bc : ((MultiKey)b).keys) {
//                li.add(bc.asBlockKey());
//            }
//            return li;
//        }
//        else {
//            return ReikaJavaLibrary.makeListFrom(b.asBlockKey());
//        }
//    }
//
//    @Override
//    public Block getBlockAt(int x, int y, int z) {
//        return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).asBlockKey().blockID : null;
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    public BlockEntity getBlockEntityAt(int x, int y, int z) {
//        if (!this.hasBlock(x, y, z))
//            return null;
//        BlockCheck b = data.get(new BlockPos(x, y, z));
//        return b instanceof BlockEntityCheck ? ((BlockEntityCheck)b).getBlockEntity() : null;
//    }
//
//    public ItemHashMap<Integer> tally() {
//        ItemHashMap<Integer> map = new ItemHashMap();
//        for (BlockCheck bc : data.values()) {
//            ItemStack key = bc.asItemStack();
//            if (this.count(key)) {
//                if (Block.getBlockFromItem(key.getItem()) instanceof BlockStairs)
//                    key.setItemDamage(0);
//                Integer get = map.get(key);
//                int has = get != null ? get.intValue() : 0;
//                map.put(key, has+1);
//            }
//        }
//        return map;
//    }
//
//    private boolean count(ItemStack is) {
//        if (is == null)
//            return false;
//        Item it = is.getItem();
//        if (it == null)
//            return false;
//        Block b = Block.getBlockFromItem(it);
//        if (ReikaBlockHelper.isLiquid(b)) {
//            if (is.getItemDamage() != (b instanceof BlockFluidFinite ? /*((BlockFluidFinite)b).quantaPerBlock*/8 : 0))
//                return false;
//        }
//        if (it instanceof BlockItem && b != null && b.getMaterial() == Material.air)
//            return false;
//        return true;
//    }
//
//    @Override
//    public void remove(int x, int y, int z) {
//        super.remove(x, y, z);
//        data.remove(new BlockPos(x, y, z));
//    }
//
//    @Override
//    public StructuredBlockArray offset(int x, int y, int z) {
//        super.offset(x, y, z);
//        HashMap map = new HashMap();
//        for (BlockPos key : data.keySet()) {
//            int dx = key.xCoord;
//            int dy = key.yCoord;
//            int dz = key.zCoord;
//            dx += x;
//            dy += y;
//            dz += z;
//            map.put(new BlockPos(dx, dy, dz), data.get(key));
//        }
//        data.clear();
//        data.putAll(map);
//        this.recalcLimits();
//        return this;
//    }
//
//    public void populateBlockData() {
//        for (int i = 0; i < this.getSize(); i++) {
//            BlockPos c = this.getNthBlock(i);
//            int x = c.xCoord;
//            int y = c.yCoord;
//            int z = c.zCoord;
//            Block b = world.getBlock(x, y, z);
//            int meta = world.getBlockMetadata(x, y, z);
//            this.setBlock(x, y, z, b, meta);
//        }
//    }
//
//    @Override
//    public String toString() {
//        return data.toString();
//    }
//
//    @Override
//    protected BlockArray instantiate() {
//        return new FilledBlockArray(world);
//    }
//
//    @Override
//    public void addAll(BlockArray arr) {
//        super.addAll(arr);
//        if (arr instanceof FilledBlockArray) {
//            data.putAll(((FilledBlockArray)arr).data);
//        }
//    }
//
//
//    public void fillFrom(SlicedBlockBlueprint sbb, int x, int y, int z, Direction dir) {
//        sbb.putInto(this, x, y, z, dir);
//    }
//
//    @Override
//    public BlockArray rotate90Degrees(int ox, int oz, boolean left) {
//        FilledBlockArray b = (FilledBlockArray)super.rotate90Degrees(ox, oz, left);
//        for (BlockPos c : data.keySet()) {
//            BlockCheck bc = data.get(c);
//            BlockPos c2 = c.rotate90About(ox, oz, left);
//            b.data.put(c2, bc);
//        }
//        return b;
//    }
//
//    @Override
//    public BlockArray rotate180Degrees(int ox, int oz) {
//        FilledBlockArray b = (FilledBlockArray)super.rotate180Degrees(ox, oz);
//        for (BlockPos c : data.keySet()) {
//            BlockCheck bc = data.get(c);
//            BlockPos c2 = c.rotate180About(ox, oz);
//            b.data.put(c2, bc);
//        }
//        return b;
//    }
//
//    @Override
//    public void clear() {
//        super.clear();
//        data.clear();
//    }
//
//    @Override
//    public BlockArray flipX() {
//        FilledBlockArray b = (FilledBlockArray)super.flipX();
//        for (BlockPos c : data.keySet()) {
//            BlockCheck bc = data.get(c);
//            BlockPos c2 = new BlockPos(-c.xCoord, c.yCoord, c.zCoord);
//            b.data.put(c2, bc);
//        }
//        return b;
//    }
//
//    @Override
//    public BlockArray flipZ() {
//        FilledBlockArray b = (FilledBlockArray)super.flipZ();
//        for (BlockPos c : data.keySet()) {
//            BlockCheck bc = data.get(c);
//            BlockPos c2 = new BlockPos(c.xCoord, c.yCoord, -c.zCoord);
//            b.data.put(c2, bc);
//        }
//        return b;
//    }
//
//    public Collection<BlockPos> getAllLocationsOf(BlockCheck key) {
//        HashSet<BlockPos> set = new HashSet();
//        for (BlockPos c : data.keySet()) {
//            BlockCheck bc = data.get(c);
//            if (bc.match(key)) {
//                set.add(c);
//            }
//        }
//        return set;
//    }
//
//    public boolean isSpaceEmpty(Level world, boolean allowSoft) {
//        for (BlockPos c : this.keySet()) {
//            Block b = c.getBlock(world);
//            if (b.isAir(world, c.xCoord, c.yCoord, c.zCoord) || (allowSoft && ReikaWorldHelper.softBlocks(world, c.xCoord, c.yCoord, c.zCoord))) {
//
//            }
//            else {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public void cutToQuarter() {
//        int x = this.getMidX();
//        int z = this.getMidZ();
//        List<BlockPos> li = new ArrayList(this.keySet());
//        for (BlockPos c : li) {
//            if (c.xCoord > x || c.zCoord > z) {
//                this.remove(c.xCoord, c.yCoord, c.zCoord);
//            }
//        }
//    }
//
//    public void cutToCenter() {
//        int x = this.getMidX();
//        int z = this.getMidZ();
//        List<BlockPos> li = new ArrayList(this.keySet());
//        for (BlockPos c : li) {
//            if (c.xCoord != x || c.zCoord != z) {
//                this.remove(c.xCoord, c.yCoord, c.zCoord);
//            }
//        }
//    }
//
//    public void cutTo(Function<BlockPos, Boolean> func) {
//        List<BlockPos> li = new ArrayList(this.keySet());
//        for (BlockPos c : li) {
//            if (!func.apply(c)) {
//                this.remove(c.xCoord, c.yCoord, c.zCoord);
//            }
//        }
//    }
//
//    public static class MultiKey implements BlockCheck {
//
//        private ArrayList<BlockCheck> keys = new ArrayList<>();
//
//        public void add(BlockCheck key) {
//            if (!keys.contains(key))
//                keys.add(key);
//        }
//
//        @Override
//        public boolean matchInWorld(Level world, int x, int y, int z) {
//            for (BlockCheck b : keys) {
//                if (b.matchInWorld(world, x, y, z))
//                    return true;
//            }
//            return false;
//        }
//
//        @Override
//        public boolean match(Block b, int meta) {
//            for (BlockCheck c : keys) {
//                if (c.match(b, meta))
//                    return true;
//            }
//            return false;
//        }
//
//        public void place(Level world, int x, int y, int z, int flags) {
//            keys.get(0).place(world, x, y, z, flags);
//        }
//
//        @Override
//        public String toString() {
//            return keys.toString();
//        }
//
//        @Override
//        public ItemStack asItemStack() {
//            return keys.get(0).asItemStack();
//        }
//
//        public BlockKey asBlockKey() {
//            return keys.get(0).asBlockKey();
//        }
//
//        public ItemStack getDisplay() {
//            return this.asItemStack();
//        }
//
//        @Override
//        public boolean match(BlockCheck bc) {
//            return bc instanceof MultiKey && ((MultiKey)bc).keys.equals(keys);
//        }
//
//        public List<BlockCheck> viewKeys() {
//            return Collections.unmodifiableList(keys);
//        }
//
//    }
//
//    private static class FluidCheck implements BlockCheck {
//
//        public final Fluid fluid;
//        public boolean needsSourceBlock = true;
//        public boolean allowSourceBlock = true;
//
//        private FluidCheck(Fluid f) {
//            if (!f.canBePlacedInWorld())
//                throw new MisuseException("You cannot require non-placeable fluids!");
//            fluid = f;
//        }
//
//        @Override
//        public boolean matchInWorld(Level world, int x, int y, int z) {
//            return this.match(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
//        }
//
//        @Override
//        public boolean match(Block b, int meta) {
//            boolean fmatch = ReikaFluidHelper.lookupFluidForBlock(b) == fluid;
//            if (!fmatch)
//                return false;
//            if (allowSourceBlock) {
//                if (needsSourceBlock)
//                    return this.isSource(b, meta);
//                else
//                    return true;
//            }
//            else {
//                return !this.isSource(b, meta);
//            }
//        }
//
//        private boolean isSource(Block b, int meta) {
//            return b instanceof BlockFluidFinite ? meta == 7 : meta == 0;
//        }
//
//        @Override
//        public void place(Level world, int x, int y, int z, int flags) {
//            world.setBlock(x, y, z, this.getBlock(), allowSourceBlock ? 0 : 1, flags);
//        }
//
//        private Block getBlock() {
//            return fluid.getBlock();
//        }
//
//        @Override
//        public ItemStack asItemStack() {
//            ItemStack is = ReikaItemHelper.getContainerForFluid(fluid);
//            return is != null ? is : new ItemStack(this.getBlock());
//        }
//
//        public BlockKey asBlockKey() {
//            return new BlockKey(this.getBlock(), 0);
//        }
//
//        public ItemStack getDisplay() {
//            return new ItemStack(this.getBlock());
//        }
//
//        @Override
//        public boolean match(BlockCheck bc) {
//            return bc instanceof FluidCheck && ((FluidCheck)bc).fluid == fluid;
//        }
//
//    }
//
//    public static class EmptyCheck implements BlockCheck {
//
//        public final boolean allowNonSolid;
//        public final boolean allowSoft;
//        private final Collection<Block> exceptions;
//
//        public EmptyCheck(boolean soft, boolean nonsolid, Block... exc) {
//            allowNonSolid = nonsolid;
//            allowSoft = soft;
//            exceptions = ReikaJavaLibrary.makeListFromArray(exc);
//        }
//
//        @Override
//        public boolean matchInWorld(Level world, int x, int y, int z) {
//            Block b = world.getBlock(x, y, z);
//            if (exceptions.contains(b))
//                return false;
//            if (b == Blocks.air || b.isAir(world, x, y, z))
//                return true;
//            if (allowSoft && ReikaWorldHelper.softBlocks(world, x, y, z))
//                return true;
//            if (allowNonSolid && b.getCollisionBoundingBoxFromPool(world, x, y, z) == null)
//                return true;
//            return false;
//        }
//
//        @Override
//        public boolean match(Block b, int meta) {
//            if (exceptions.contains(b))
//                return false;
//            if (b == Blocks.air || b instanceof BlockAir)
//                return true;
//            if (allowSoft && ReikaWorldHelper.softBlocks(b))
//                return true;
//            if (allowNonSolid && b.getMaterial().blocksMovement())
//                return true;
//            return false;
//        }
//
//        @Override
//        public void place(Level world, int x, int y, int z, int flags) {
//            world.setBlock(x, y, z, Blocks.air);
//        }
//
//        @Override
//        public String toString() {
//            return "[Empty]";
//        }
//
//        @Override
//        public ItemStack asItemStack() {
//            return null;
//        }
//
//        public BlockKey asBlockKey() {
//            return new BlockKey(Blocks.air);
//        }
//
//        public ItemStack getDisplay() {
//            return null;
//        }
//
//        @Override
//        public boolean match(BlockCheck bc) {
//            if (bc instanceof EmptyCheck) {
//                EmptyCheck ec = (EmptyCheck)bc;
//                return ec.allowNonSolid == allowNonSolid && ec.allowSoft == allowSoft && ec.exceptions.equals(exceptions);
//            }
//            return false;
//        }
//
//    }
//
//    private static class BasicBlockEntityCheck implements BlockCheck.BlockEntityCheck {
//
//        private final BlockKey block;
//        private final Class tileClass;
//        private final CompoundTag matchTag;
//        private WeakReference<BlockEntity> tileRef;
//
//        private BasicBlockEntityCheck(TileEnum te, String... tags) {
//            this(te.getBlockState().getBlock(), te.getTEClass(), tags);
//        }
//
//        private BasicBlockEntityCheck(Block b, Class<? extends BlockEntity> c, String... tags) {
//            block = new BlockKey(b);
//            matchTag = new CompoundTag();
//            tileClass = c;
//        }
//
//        private BasicBlockEntityCheck(Block b, BlockEntity te, String... tags) {
//            this(b, te.getClass(), tags);
//            CompoundTag tag = new CompoundTag();
//            te.save(tag);
//            for (int i = 0; i < tags.length; i++) {
//                NBTBase nbt = tag.getTag(tags[i]);
//                if (nbt != null)
//                    matchTag.put(tags[i], nbt);
//            }
//            tileRef = new WeakReference<>(te);
//        }
//
//        public ItemStack asItemStack() {
//            return new ItemStack(block.blockID.getBlock().asItem(), 1);
//        }
//
//        public ItemStack getDisplay() {
//            return this.asItemStack();
//        }
//
//        public boolean match(BlockState b) {
//            return b == block.blockID;
//        }
//
//        public boolean matchInWorld(Level world, BlockPos pos) {
//            return this.match(world.getBlockState(pos)) && this.matchTile(world.getBlockEntity(pos));
//        }
//
//        private boolean matchTile(BlockEntity te) {
//            if (te == null || te.getClass() != tileClass)
//                return false;
//            CompoundTag tag = new CompoundTag();
//            te.save(tag);
//            return ReikaNBTHelper.tagContains(tag, matchTag);
//        }
//
//        @Override
//        public void place(Level world, BlockPos pos, int flags) {
//            world.setBlock(pos, block.blockID, flags);
//            BlockEntity te = world.getBlockEntity(pos);
//            if (te != null && te.getClass() == tileClass) {
//                CompoundTag NBT = new CompoundTag();
//                te.save(NBT);
//                ReikaNBTHelper.overwriteNBT(NBT, matchTag);
//                te.load(NBT);
//            }
//        }
//
//        @Override
//        public BlockKey asBlockKey() {
//            return block;
//        }
//
//        @Override
//        public BlockEntity getBlockEntity() {
//            return tileRef.get();
//        }
//
//        @Override
//        public String toString() {
//            return block.toString()+"; NBT "+matchTag;
//        }
//
//        @Override
//        public boolean match(BlockCheck bc) {
//            if (bc instanceof BasicBlockEntityCheck) {
//                BasicBlockEntityCheck bt = (BasicBlockEntityCheck)bc;
//                return bt.block.equals(block) && bt.tileClass == tileClass && bt.matchTag.equals(matchTag);
//            }
//            return false;
//        }
//    }
//
//    public static interface BlockMatchFailCallback {
//
//        public void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek);
//
//    }
//
//    public static interface PlacementExclusionHook {
//
//        public boolean skipPlacement(BlockPos c, BlockCheck bc);
//
//    }
//}
