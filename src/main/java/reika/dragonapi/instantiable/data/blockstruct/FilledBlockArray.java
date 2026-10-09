package reika.dragonapi.instantiable.data.blockstruct;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;
import reika.dragonapi.interfaces.registry.TileEnum;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

/**
 * A declared 26.3 multiblock layout: coordinates, exact states, alternatives, fluid/empty
 * constraints, selected block-entity NBT, placement overrides and material requirements.
 * Coordinate transforms preserve the original state/check orientation; they do not rotate
 * facing properties or reinterpret selected NBT.
 */
public class FilledBlockArray extends StructuredBlockArray {

	private final HashMap<BlockPos, BlockCheck> data = new HashMap<>();
	private final HashMap<BlockPos, BlockKey> placementOverrides = new HashMap<>();

	public static boolean logMismatches;

	public FilledBlockArray(Level world) {
		super(world);
	}

    @Override
    public void copyTo(BlockArray copy) {
        if (copy == this)
            return;
        super.copyTo(copy);
        if (copy instanceof FilledBlockArray target) {
            target.data.clear();
            data.forEach((position, check) -> target.data.put(position, copyCheck(check)));
            target.placementOverrides.clear();
            target.placementOverrides.putAll(placementOverrides);
        }
    }

    private static BlockCheck copyCheck(BlockCheck check) {
        if (check instanceof MultiKey alternatives) {
            MultiKey result = new MultiKey();
            alternatives.keys.forEach(key -> result.add(copyCheck(key)));
            return result;
        }
        return check;
    }

	public void loadBlock(int x, int y, int z) {
		this.setBlock(x, y, z, world.getBlockState(new BlockPos(x, y, z)));
	}

	public void loadBlockTo(int x, int y, int z, int xt, int yt, int zt) {
		this.setBlock(xt, yt, zt, world.getBlockState(new BlockPos(x, y, z)));
	}

	public void setBlock(int x, int y, int z, Block id) {
		this.setBlock(x, y, z, new BlockKey(id));
	}

	public void setBlock(int x, int y, int z, BlockState state) {
		this.setBlock(x, y, z, new BlockKey(state));
	}

    public void setBlock(int x, int y, int z, BlockCheck check) {
        BlockPos position = new BlockPos(x, y, z);
        if (super.putBlockKey(position, check.asBlockKey()))
            data.put(position, check);
    }

    public void setTile(int x, int y, int z, Block block, BlockEntity tile, String... tags) {
        this.setTile(x, y, z, block.defaultBlockState(), tile, tags);
    }

    public void setTile(int x, int y, int z, BlockState state, BlockEntity tile, String... tags) {
        HolderLookup.Provider registries = tile.getLevel() != null ? tile.getLevel().registryAccess()
                : world != null ? world.registryAccess() : RegistryAccess.EMPTY;
        this.setTile(x, y, z, state, tile, registries, tags);
    }

    /** Explicit registry context supports detached prototypes and world-free layout construction. */
    public void setTile(int x, int y, int z, BlockState state, BlockEntity tile, HolderLookup.Provider registries, String... tags) {
        this.setBlock(x, y, z, new BasicBlockEntityCheck(state, tile, registries, tags));
    }

    public void setTile(int x, int y, int z, TileEnum tile) {
        this.setBlock(x, y, z, new BasicBlockEntityCheck(tile.getBlockState(), tile.getTEClass()));
    }

    public void setTile(int x, int y, int z, BlockState state, Class<? extends BlockEntity> tileClass) {
        this.setBlock(x, y, z, new BasicBlockEntityCheck(state, tileClass));
    }

    public void setFluid(int x, int y, int z, Fluid fluid) {
        this.setFluid(x, y, z, fluid, true, true);
    }

    public void setFluid(int x, int y, int z, Fluid fluid, boolean needSource, boolean allowSource) {
        this.setBlock(x, y, z, new FluidCheck(fluid, needSource, allowSource));
    }

    public void setEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
        this.setBlock(x, y, z, new EmptyCheck(soft, nonsolid, exceptions));
    }

    public void addEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
        this.addBlock(x, y, z, new EmptyCheck(soft, nonsolid, exceptions));
    }

    public void addBlock(int x, int y, int z, Block block) {
        this.addBlock(x, y, z, new BlockKey(block));
    }

    public void addBlock(int x, int y, int z, BlockState state) {
        this.addBlock(x, y, z, new BlockKey(state));
    }

    public void addBlock(int x, int y, int z, BlockCheck check) {
        BlockPos position = new BlockPos(x, y, z);
        if (!this.hasBlock(position) && !super.putBlockKey(position, check.asBlockKey()))
            return;
        this.addBlockToCoord(position, check);
        super.putBlockKey(position, data.get(position).asBlockKey());
    }

	private void addBlockToCoord(BlockPos c, BlockCheck bk) {
		BlockCheck bc = data.get(c);
		if (bc == null || bc instanceof EmptyCheck) {
			MultiKey mk = new MultiKey();
			if (bc != null)
				mk.add(bc);
			mk.add(bk);
			data.put(c, mk);
		}
		else if (bc instanceof MultiKey) {
			((MultiKey)bc).add(bk);
		}
		else {
			MultiKey mk = new MultiKey();
			mk.add(bc);
			mk.add(bk);
			data.put(c, mk);
		}
	}

	public void setPlacementOverride(int x, int y, int z, BlockState state) {
		placementOverrides.put(new BlockPos(x, y, z), new BlockKey(state));
	}

	private BlockCheck getBlockKey(int x, int y, int z) {
		return data.get(new BlockPos(x, y, z));
	}

	@Override
	public boolean addBlockCoordinate(BlockPos pos) {
		if (super.addBlockCoordinate(pos)) {
			data.put(pos.immutable(), BlockKey.getAt(world, pos));
			return true;
		}
		return false;
	}

	public void place() {
		this.placeExcept(null, 3);
	}

	public void place(int flags) {
		this.placeExcept(null, flags);
	}

	public void placeExcept(BlockPos e, int flags) {
		for (Entry<BlockPos, BlockCheck> et : data.entrySet()) {
			BlockPos c = et.getKey();
			if (!c.equals(e)) {
				BlockKey po = placementOverrides.get(c);
				if (po != null)
					po.place(world, c, flags);
				else
					et.getValue().place(world, c, flags);
			}
		}
	}

	public void placeExcept(int flags, PlacementExclusionHook h) {
		for (Entry<BlockPos, BlockCheck> et : data.entrySet()) {
			BlockPos c = et.getKey();
			BlockCheck bc = et.getValue();
			if (!h.skipPlacement(c, bc)) {
				BlockKey po = placementOverrides.get(c);
				if (po != null)
					po.place(world, c, flags);
				else
					bc.place(world, c, flags);
			}
		}
	}

	public ItemStack getDisplayAt(int x, int y, int z) {
		BlockCheck bk = this.getBlockKey(x, y, z);
		return bk != null ? bk.getDisplay() : ItemStack.EMPTY;
	}

	public boolean hasBlockAt(int x, int y, int z, BlockState b) {
		BlockCheck bc = this.getBlockKey(x, y, z);
		return bc != null && bc.match(b);
	}

	public boolean matchInWorld() {
		return this.matchInWorld(null);
	}

	public boolean matchInWorld(BlockMatchFailCallback call) {
		if (world.isClientSide())
			return true;
		for (BlockPos c : data.keySet()) {
			BlockCheck bk = this.getBlockKey(c.getX(), c.getY(), c.getZ());
			if (!bk.matchInWorld(world, c)) {
				if (logMismatches)
					DragonAPI.LOGGER.info(c + " > Wanted [" + bk.getClass().getSimpleName() + "] " + bk.asBlockKey() + ", found " + world.getBlockState(c));
				if (call != null)
					call.onBlockFailure(world, c.getX(), c.getY(), c.getZ(), bk);
				return false;
			}
		}
		return true;
	}

	public int countErrors() {
		if (world.isClientSide())
			return 0;
		int ret = 0;
		for (BlockPos c : data.keySet()) {
			BlockCheck bk = this.getBlockKey(c.getX(), c.getY(), c.getZ());
			if (!bk.matchInWorld(world, c))
				ret++;
		}
		return ret;
	}

	@Override
	public BlockKey getBlockKeyAt(int x, int y, int z) {
		return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).asBlockKey() : null;
	}

	public boolean isMultiKey(int x, int y, int z) {
		return data.get(new BlockPos(x, y, z)) instanceof MultiKey;
	}

	public MultiKey getMultiKeyAt(int x, int y, int z) {
		if (!this.hasBlock(x, y, z))
			return null;
		BlockCheck b = data.get(new BlockPos(x, y, z));
		return b instanceof MultiKey ? (MultiKey)b : null;
	}

	public ArrayList<BlockKey> getMultiListAt(int x, int y, int z) {
		if (!this.hasBlock(x, y, z))
			return null;
		BlockCheck b = data.get(new BlockPos(x, y, z));
		ArrayList<BlockKey> li = new ArrayList<>();
		if (b instanceof MultiKey) {
			for (BlockCheck bc : ((MultiKey)b).keys)
				li.add(bc.asBlockKey());
		}
		else {
			li.add(b.asBlockKey());
		}
		return li;
	}

	@Override
	public Block getBlockAt(int x, int y, int z) {
		return this.hasBlock(x, y, z) ? data.get(new BlockPos(x, y, z)).asBlockKey().blockID.getBlock() : null;
	}

	public net.minecraft.world.level.block.entity.BlockEntity getBlockEntityAt(int x, int y, int z) {
		if (!this.hasBlock(x, y, z))
			return null;
		BlockCheck b = data.get(new BlockPos(x, y, z));
		return b instanceof BlockCheck.BlockEntityCheck ? ((BlockCheck.BlockEntityCheck)b).getBlockEntity() : null;
	}

    /** One required item per admitted coordinate; alternatives use their original first representative. */
    public ItemHashMap<Integer> tally() {
        return this.tally(null);
    }

    public ItemHashMap<Integer> tally(Function<BlockPos, Boolean> validity) {
        ItemHashMap<Integer> result = new ItemHashMap<Integer>().enableNBT();
        for (Entry<BlockPos, BlockCheck> entry : data.entrySet()) {
            if (validity != null && !validity.apply(entry.getKey()))
                continue;
            ItemStack stack = entry.getValue().asItemStack();
            if (!count(stack, entry.getValue().asBlockKey().blockID))
                continue;
            ItemStack key = stack.copyWithCount(1);
            Integer previous = result.get(key);
            result.put(key, previous == null ? 1 : previous + 1);
        }
        return result;
    }

    private static boolean count(ItemStack stack, BlockState state) {
        if (stack == null || stack.isEmpty())
            return false;
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem))
            return true;
        if (blockItem.getBlock().defaultBlockState().isAir())
            return false;
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty() || fluid.createLegacyBlock().getBlock() != state.getBlock() || fluid.isSource();
    }

    public void fillFrom(SlicedBlockBlueprint blueprint, int x, int y, int z, Direction direction) {
        blueprint.putInto(this, x, y, z, direction);
    }

	@Override
	public void remove(BlockPos pos) {
		super.remove(pos);
		data.remove(pos);
        placementOverrides.remove(pos);
	}

	@Override
    public FilledBlockArray offset(int x, int y, int z) {
        return this.offset(new BlockPos(x, y, z));
    }

    @Override
    public FilledBlockArray offset(BlockPos offset) {
        super.offset(offset);
        HashMap<BlockPos, BlockCheck> shifted = new HashMap<>();
        data.forEach((position, check) -> shifted.put(position.offset(offset), check));
        data.clear(); data.putAll(shifted);
        HashMap<BlockPos, BlockKey> shiftedOverrides = new HashMap<>();
        placementOverrides.forEach((position, key) -> shiftedOverrides.put(position.offset(offset), key));
        placementOverrides.clear(); placementOverrides.putAll(shiftedOverrides);
        return this;
    }

	public void populateBlockData() {
		for (int i = 0; i < this.getSize(); i++) {
			BlockPos c = this.getNthBlock(i);
			this.setBlock(c.getX(), c.getY(), c.getZ(), world.getBlockState(c));
		}
	}

	@Override
	public String toString() {
		return data.toString();
	}

	@Override
	protected BlockArray instantiate() {
		return new FilledBlockArray(world);
	}

    @Override
    public void addAll(BlockArray arr) {
        super.addAll(arr);
        for (BlockPos position : arr) {
            if (!this.hasBlock(position))
                continue;
            BlockCheck check = arr instanceof FilledBlockArray source ? source.data.get(position)
                    : super.getBlockKeyAt(position.getX(), position.getY(), position.getZ());
            data.put(position, copyCheck(check));
            if (arr instanceof FilledBlockArray source && source.placementOverrides.containsKey(position))
                placementOverrides.put(position, source.placementOverrides.get(position));
        }
    }

    @Override
    protected FilledBlockArray transform(Function<BlockPos, BlockPos> transform) {
        FilledBlockArray result = (FilledBlockArray)super.transform(transform);
        data.forEach((position, check) -> result.data.put(transform.apply(position).immutable(), copyCheck(check)));
        placementOverrides.forEach((position, key) -> result.placementOverrides.put(transform.apply(position).immutable(), key));
        return result;
    }

    @Override
    public FilledBlockArray rotate90Degrees(int ox, int oz, boolean left) {
        return (FilledBlockArray)super.rotate90Degrees(ox, oz, left);
    }

    @Override
    public FilledBlockArray rotate180Degrees(int ox, int oz) {
        return (FilledBlockArray)super.rotate180Degrees(ox, oz);
    }

    @Override
    public FilledBlockArray flipX() {
        return (FilledBlockArray)super.flipX();
    }

    @Override
    public FilledBlockArray flipZ() {
        return (FilledBlockArray)super.flipZ();
    }

	@Override
	public void clear() {
		super.clear();
		data.clear();
        placementOverrides.clear();
	}

	public Collection<BlockPos> getAllLocationsOf(BlockCheck key) {
		HashSet<BlockPos> set = new HashSet<>();
		for (Entry<BlockPos, BlockCheck> e : data.entrySet()) {
			if (e.getValue().match(key))
				set.add(e.getKey());
		}
		return set;
	}

	public boolean isSpaceEmpty(Level world, boolean allowSoft) {
		for (BlockPos c : this.keySet()) {
			if (!world.getBlockState(c).isAir() && !(allowSoft && ReikaWorldHelper.softBlocks(world, c)))
				return false;
		}
		return true;
	}

	public void cutToQuarter() {
		int x = this.getMidX();
		int z = this.getMidZ();
		for (BlockPos c : new ArrayList<>(this.keySet())) {
			if (c.getX() > x || c.getZ() > z)
				this.remove(c);
		}
	}

	public void cutToCenter() {
		int x = this.getMidX();
		int z = this.getMidZ();
		for (BlockPos c : new ArrayList<>(this.keySet())) {
			if (c.getX() != x || c.getZ() != z)
				this.remove(c);
		}
	}

	public void cutTo(Function<BlockPos, Boolean> func) {
		for (BlockPos c : new ArrayList<>(this.keySet())) {
			if (!func.apply(c))
				this.remove(c);
		}
	}

	/** A coordinate that will match (and place as) any one of several {@link BlockCheck}s. */
	public static class MultiKey implements BlockCheck {

		private final ArrayList<BlockCheck> keys = new ArrayList<>();

		public void add(BlockCheck key) {
			if (!keys.contains(key))
				keys.add(key);
		}

		@Override
		public boolean matchInWorld(Level world, BlockPos pos) {
			for (BlockCheck b : keys) {
				if (b.matchInWorld(world, pos))
					return true;
			}
			return false;
		}

		@Override
		public boolean match(BlockState b) {
			for (BlockCheck c : keys) {
				if (c.match(b))
					return true;
			}
			return false;
		}

		@Override
		public void place(Level world, BlockPos pos, int flags) {
			keys.get(0).place(world, pos, flags);
		}

		@Override
		public String toString() {
			return keys.toString();
		}

		@Override
		public ItemStack asItemStack() {
			return keys.get(0).asItemStack();
		}

		@Override
		public BlockKey asBlockKey() {
			return keys.get(0).asBlockKey();
		}

		@Override
		public ItemStack getDisplay() {
			return this.asItemStack();
		}

		@Override
		public boolean match(BlockCheck bc) {
			return bc instanceof MultiKey && ((MultiKey)bc).keys.equals(keys);
		}

		public List<BlockCheck> viewKeys() {
			return java.util.Collections.unmodifiableList(keys);
		}
	}

	/** A coordinate that must be empty (air, optionally non-solid), except for the given blocks. */
	public static class EmptyCheck implements BlockCheck {

		public final boolean allowNonSolid;
		public final boolean allowSoft;
		private final Collection<Block> exceptions;

		public EmptyCheck(boolean soft, boolean nonsolid, Block... exc) {
			allowNonSolid = nonsolid;
			allowSoft = soft;
			exceptions = new ArrayList<>();
            Collections.addAll(exceptions, exc);
		}

		@Override
		public boolean matchInWorld(Level world, BlockPos pos) {
			return this.match(world.getBlockState(pos), world, pos);
		}

		@Override
		public boolean match(BlockState b) {
			return this.match(b, null, null);
		}

		private boolean match(BlockState b, Level world, BlockPos pos) {
			if (exceptions.contains(b.getBlock()))
				return false;
			if (b.isAir())
				return true;
			if (allowSoft && (world != null ? ReikaWorldHelper.softBlocks(world, pos)
                    : b.canBeReplaced() || !b.getFluidState().isEmpty() || ReikaWorldHelper.softBlocks(b.getBlock())))
                return true;
            return allowNonSolid && b.getCollisionShape(world != null ? world : net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                    pos != null ? pos : BlockPos.ZERO).isEmpty();
        }

		@Override
		public void place(Level world, BlockPos pos, int flags) {
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
		}

		@Override
		public String toString() {
			return "[Empty]";
		}

		@Override
		public ItemStack asItemStack() {
			return ItemStack.EMPTY;
		}

		@Override
		public BlockKey asBlockKey() {
			return BlockKey.AIR;
		}

		@Override
		public ItemStack getDisplay() {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean match(BlockCheck bc) {
			if (bc instanceof EmptyCheck ec) {
                return ec.allowNonSolid == allowNonSolid && ec.allowSoft == allowSoft && ec.exceptions.equals(exceptions);
			}
			return false;
		}
	}

    /** Standalone fluid blocks only: waterlogged solids are not substitutes for an authored fluid cell. */
    private static final class FluidCheck implements BlockCheck {
        private final Fluid fluid;
        private final boolean needsSourceBlock;
        private final boolean allowSourceBlock;
        private final BlockState placement;

        private FluidCheck(Fluid fluid, boolean needSource, boolean allowSource) {
            this.fluid = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
            needsSourceBlock = needSource;
            allowSourceBlock = allowSource;
            FluidState state = this.fluid.defaultFluidState();
            if (!allowSource) {
                if (!(this.fluid instanceof FlowingFluid flowing))
                    throw new MisuseException("This fluid does not expose a flowing state: " + fluid);
                state = flowing.getFlowing(7, false);
            }
            placement = state.createLegacyBlock();
            if (state.isEmpty() || placement.isAir() || (allowSource && needSource && !state.isSource()))
                throw new MisuseException("You cannot require a fluid without a placeable matching state: " + fluid);
        }

        public boolean match(BlockState state) {
            FluidState actual = state.getFluidState();
            return !actual.isEmpty() && actual.getType().isSame(fluid)
                    && actual.createLegacyBlock().getBlock() == state.getBlock()
                    && (allowSourceBlock ? !needsSourceBlock || actual.isSource() : !actual.isSource());
        }
        public boolean matchInWorld(Level world, BlockPos position) { return this.match(world.getBlockState(position)); }
        public void place(Level world, BlockPos position, int flags) { world.setBlock(position, placement, flags); }
        public ItemStack asItemStack() {
            ItemStack bucket = new ItemStack(fluid.getBucket());
            return bucket.isEmpty() ? this.asBlockKey().asItemStack() : bucket;
        }
        public ItemStack getDisplay() { return this.asItemStack(); }
        public BlockKey asBlockKey() { return new BlockKey(placement); }
        public boolean match(BlockCheck check) {
            return check instanceof FluidCheck other && fluid.isSame(other.fluid)
                    && needsSourceBlock == other.needsSourceBlock && allowSourceBlock == other.allowSourceBlock;
        }
    }

    /** Exact entity class plus equality of the selected top-level NBT fields, as in the original API. */
    public static final class BasicBlockEntityCheck implements BlockCheck.BlockEntityCheck {
        private final BlockKey block;
        private final Class<? extends BlockEntity> tileClass;
        private final CompoundTag matchTag = new CompoundTag();
        private WeakReference<BlockEntity> tileRef = new WeakReference<>(null);

        public BasicBlockEntityCheck(BlockState state, Class<? extends BlockEntity> tileClass) {
            block = new BlockKey(state);
            this.tileClass = Objects.requireNonNull(tileClass);
        }

        public BasicBlockEntityCheck(BlockState state, BlockEntity tile, HolderLookup.Provider registries, String... tags) {
            this(state, tile.getClass());
            CompoundTag source = tile.saveWithFullMetadata(registries);
            for (String key : tags) {
                var value = source.get(key);
                if (value != null)
                    matchTag.put(key, value.copy());
            }
            tileRef = new WeakReference<>(tile);
        }

        public boolean match(BlockState state) { return block.match(state); }
        public boolean matchInWorld(Level world, BlockPos position) {
            if (!block.matchInWorld(world, position))
                return false;
            BlockEntity tile = world.getBlockEntity(position);
            return tile != null && tile.getClass() == tileClass
                    && ReikaNBTHelper.tagContains(tile.saveWithFullMetadata(world.registryAccess()), matchTag);
        }
        public void place(Level world, BlockPos position, int flags) {
            block.place(world, position, flags);
            BlockEntity tile = world.getBlockEntity(position);
            if (tile != null && tile.getClass() == tileClass) {
                CompoundTag existing = tile.saveWithoutMetadata(world.registryAccess());
                for (String key : matchTag.keySet())
                    existing.put(key, matchTag.get(key).copy());
                try (var reporter = new ProblemReporter.ScopedCollector(tile.problemPath(), com.mojang.logging.LogUtils.getLogger())) {
                    tile.loadWithComponents(TagValueInput.create(reporter, world.registryAccess(), existing));
                }
                tile.setChanged();
            }
        }
        public ItemStack asItemStack() { return block.asItemStack(); }
        public ItemStack getDisplay() { return this.asItemStack(); }
        public BlockKey asBlockKey() { return block; }
        public BlockEntity getBlockEntity() { return tileRef.get(); }
        public boolean match(BlockCheck check) {
            return check instanceof BasicBlockEntityCheck other && block.equals(other.block)
                    && tileClass == other.tileClass && matchTag.equals(other.matchTag);
        }
        public String toString() { return block + "; " + tileClass.getName() + "; NBT " + matchTag; }
    }

	public interface BlockMatchFailCallback {

		void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek);

	}

	public interface PlacementExclusionHook {

		boolean skipPlacement(BlockPos c, BlockCheck bc);

	}
}
