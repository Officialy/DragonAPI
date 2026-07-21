package reika.dragonapi.instantiable.data.blockstruct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

/**
 * A {@link StructuredBlockArray} that additionally remembers a required {@link BlockCheck} at each
 * coordinate — the backbone of every multiblock structure definition (match a layout against the
 * world, place it, tally it). Ported to 26.2 on the fully-BlockState {@link BlockKey}/{@link BlockCheck}
 * APIs (the 1.7.10 original was block+metadata based).
 *
 * <p><b>Port note (deferred):</b> a few methods depend on helpers/features not yet ported and are left
 * out until they are — the fluid-check overloads ({@code ReikaFluidHelper}/{@code BlockFluidFinite}),
 * {@code tally()}/{@code count()} (old ItemStack-damage + BlockStairs logic), {@code rotate90Degrees}/
 * {@code rotate180Degrees} (the parent {@link StructuredBlockArray} still stubs its rotate methods),
 * {@code fillFrom} (SlicedBlockBlueprint), and the NBT-matching {@code BasicBlockEntityCheck} +
 * {@code setTile} overloads (26.2 BlockEntity save/load API; not used by the crystal-network/casting
 * structures, which place plain variant blocks). The core set/add/match/place/offset/flip/cut surface —
 * everything the structures on the current port path use — is fully ported.
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
		super.copyTo(copy);
		if (copy instanceof FilledBlockArray) {
			((FilledBlockArray)copy).data.putAll(data);
			((FilledBlockArray)copy).placementOverrides.putAll(placementOverrides);
		}
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

	public void setBlock(int x, int y, int z, BlockCheck bk) {
		super.addBlockCoordinate(new BlockPos(x, y, z));
		data.put(new BlockPos(x, y, z), bk);
	}

	public void setEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
		super.addBlockCoordinate(new BlockPos(x, y, z));
		data.put(new BlockPos(x, y, z), new EmptyCheck(soft, nonsolid, exceptions));
	}

	public void addEmpty(int x, int y, int z, boolean soft, boolean nonsolid, Block... exceptions) {
		super.addBlockCoordinate(new BlockPos(x, y, z));
		this.addBlockToCoord(new BlockPos(x, y, z), new EmptyCheck(soft, nonsolid, exceptions));
	}

	public void addBlock(int x, int y, int z, Block id) {
		this.addBlock(x, y, z, new BlockKey(id));
	}

	public void addBlock(int x, int y, int z, BlockState state) {
		this.addBlock(x, y, z, new BlockKey(state));
	}

	public void addBlock(int x, int y, int z, BlockCheck b) {
		super.addBlockCoordinate(new BlockPos(x, y, z));
		this.addBlockToCoord(new BlockPos(x, y, z), b);
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
			data.put(pos, BlockKey.getAt(world, pos));
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

	@Override
	public void remove(BlockPos pos) {
		super.remove(pos);
		data.remove(pos);
	}

	@Override
	public StructuredBlockArray offset(int x, int y, int z) {
		super.offset(x, y, z);
		HashMap<BlockPos, BlockCheck> map = new HashMap<>();
		for (Entry<BlockPos, BlockCheck> e : data.entrySet()) {
			BlockPos key = e.getKey();
			map.put(new BlockPos(key.getX() + x, key.getY() + y, key.getZ() + z), e.getValue());
		}
		data.clear();
		data.putAll(map);
		this.recalcLimits();
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
		if (arr instanceof FilledBlockArray)
			data.putAll(((FilledBlockArray)arr).data);
	}

	@Override
	public BlockArray flipX() {
		FilledBlockArray b = (FilledBlockArray)super.flipX();
		for (Entry<BlockPos, BlockCheck> e : data.entrySet()) {
			BlockPos c = e.getKey();
			b.data.put(new BlockPos(-c.getX(), c.getY(), c.getZ()), e.getValue());
		}
		return b;
	}

	@Override
	public BlockArray flipZ() {
		FilledBlockArray b = (FilledBlockArray)super.flipZ();
		for (Entry<BlockPos, BlockCheck> e : data.entrySet()) {
			BlockPos c = e.getKey();
			b.data.put(new BlockPos(c.getX(), c.getY(), -c.getZ()), e.getValue());
		}
		return b;
	}

	@Override
	public void clear() {
		super.clear();
		data.clear();
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
			if (!world.getBlockState(c).isAir())
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
			for (Block b : exc)
				exceptions.add(b);
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
			// The 1.7.10 "soft blocks" allowance is deferred (ReikaWorldHelper.softBlocks unported).
			if (allowNonSolid && world != null && b.getCollisionShape(world, pos).isEmpty())
				return true;
			return false;
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
			if (bc instanceof EmptyCheck) {
				EmptyCheck ec = (EmptyCheck)bc;
				return ec.allowNonSolid == allowNonSolid && ec.allowSoft == allowSoft && ec.exceptions.equals(exceptions);
			}
			return false;
		}
	}

	public interface BlockMatchFailCallback {

		void onBlockFailure(Level world, int x, int y, int z, BlockCheck seek);

	}

	public interface PlacementExclusionHook {

		boolean skipPlacement(BlockPos c, BlockCheck bc);

	}
}
