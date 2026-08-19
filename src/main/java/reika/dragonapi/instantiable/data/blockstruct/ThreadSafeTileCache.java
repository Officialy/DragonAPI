package reika.dragonapi.instantiable.data.blockstruct;

import java.util.Iterator;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.instantiable.data.collections.ThreadSafeSet;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * A thread-safe set of world locations that are expected to hold a block entity of a given class, with
 * the iteration helpers that keep it honest.
 *
 * <p>The point of the class is that the set outlives the block entities in it. A location whose tile
 * has been broken, replaced, or never loaded is stale, and every traversal here drops such an entry as
 * it walks rather than leaving it to accumulate — which is why the iteration is done under the set's own
 * lock with an explicit iterator instead of through {@code forEach}.
 *
 * <p>{@link #tileClass} may be null, which means "any tile, even none, is acceptable"; that is not the
 * same as the default of {@code BlockEntity.class}, which still requires something to be there.
 *
 * <p>A location in another dimension can only be resolved through the server, so each traversal takes a
 * {@code skipOtherDimension} flag: passing true is how a caller that only cares about its own level
 * avoids paying for cross-level lookups.
 */
public final class ThreadSafeTileCache extends ThreadSafeSet<WorldLocation> {

	/**
	 * The class a location's tile must be assignable to for that location to be valid. Null means even
	 * an absent tile is acceptable.
	 */
	public Class<?> tileClass = BlockEntity.class;

	public ThreadSafeTileCache setTileClass(Class<? extends BlockEntity> c) {
		tileClass = c;
		return this;
	}

	/** Drops every location whose tile is missing or of the wrong class. Server-side only. */
	public void filterInvalidTiles(Level world, boolean skipOtherDimension) {
		if (world.isClientSide() || tileClass == null)
			return;
		this.filterElements(location -> {
			boolean other = !location.getDimension().equals(world.dimension());
			if (skipOtherDimension && other)
				return false;
			BlockEntity te = other ? location.getBlockEntity() : location.getBlockEntity(world);
			return te == null || !tileClass.isAssignableFrom(te.getClass());
		});
	}

	public boolean lookForMatch(Level world, boolean skipOtherDimension, TileEntityMatchCheck check) {
		return this.lookForMatch(world, skipOtherDimension, check, null);
	}

	public boolean lookForMatch(Level world, boolean skipOtherDimension, TileEntityMatchCheck check,
			ValidityFailHandler errorHandle) {
		// Deliberately not `returnMatch(...) != null`: with a null tileClass a match may legitimately
		// have no tile, and deriving the boolean from the tile would call that no match.
		return this.walk(world, skipOtherDimension, errorHandle, check, null);
	}

	public void applyToMatches(Level world, boolean skipOtherDimension, TileEntityMatchEffect check) {
		this.applyToMatches(world, skipOtherDimension, check, null);
	}

	public void applyToMatches(Level world, boolean skipOtherDimension, TileEntityMatchEffect check,
			ValidityFailHandler errorHandle) {
		this.walk(world, skipOtherDimension, errorHandle, (location, te) -> {
			check.handle(location, te);
			return false;
		}, null);
	}

	public BlockEntity returnMatch(Level world, boolean skipOtherDimension, TileEntityMatchCheck check) {
		return this.returnMatch(world, skipOtherDimension, check, null);
	}

	public BlockEntity returnMatch(Level world, boolean skipOtherDimension, TileEntityMatchCheck check,
			ValidityFailHandler errorHandle) {
		BlockEntity[] found = new BlockEntity[1];
		this.walk(world, skipOtherDimension, errorHandle, check, found);
		return found[0];
	}

	/**
	 * Walks the live entries under the set's own lock, dropping the stale ones as it goes, and stops at
	 * the first entry the visitor accepts.
	 *
	 * <p>Skipping is decided here rather than signalled back through a return value, because a null
	 * tile is not a single case: it is legitimately skipped on the client, and legitimately a match when
	 * {@link #tileClass} is null. A sentinel of type {@code BlockEntity} cannot tell those apart from a
	 * genuine null match, so the decision never leaves this method.
	 *
	 * <p>Upstream dereferences the tile before null-checking it when a tile class is set, which throws
	 * on a location whose block has gone. Guarding it is a fix rather than a change of behaviour — the
	 * entry was always meant to be dropped, and upstream only survives because its client-side branch
	 * usually catches the case first.
	 *
	 * @param accepted if non-null, receives the tile the visitor accepted; needed because that tile may
	 *                 itself be null when {@link #tileClass} is, so the return value cannot carry it
	 * @return whether the visitor accepted an entry
	 */
	private boolean walk(Level world, boolean skipOtherDimension, ValidityFailHandler errorHandle,
			TileEntityMatchCheck visitor, BlockEntity[] accepted) {
		synchronized (data) {
			// The iterator must be taken inside the lock, not before it.
			Iterator<WorldLocation> it = data.iterator();
			while (it.hasNext()) {
				WorldLocation location = it.next();
				boolean other = !location.getDimension().equals(world.dimension());
				if (skipOtherDimension && other)
					continue;
				BlockEntity te = other ? location.getBlockEntity() : location.getBlockEntity(world);
				if (te == null && world.isClientSide())
					continue;
				if (tileClass != null && (te == null || !tileClass.isAssignableFrom(te.getClass()))) {
					it.remove();
					if (errorHandle != null)
						errorHandle.handle(location, te);
					continue;
				}
				if (visitor.handle(location, te)) {
					if (accepted != null)
						accepted[0] = te;
					return true;
				}
			}
		}
		return false;
	}

	@FunctionalInterface
	public interface TileEntityMatchCheck {
		boolean handle(WorldLocation loc, BlockEntity te);
	}

	@FunctionalInterface
	public interface TileEntityMatchEffect {
		void handle(WorldLocation loc, BlockEntity te);
	}

	@FunctionalInterface
	public interface ValidityFailHandler {
		void handle(WorldLocation loc, BlockEntity te);
	}
}
