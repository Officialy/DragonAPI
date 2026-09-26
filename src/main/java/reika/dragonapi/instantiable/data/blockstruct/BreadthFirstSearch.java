package reika.dragonapi.instantiable.data.blockstruct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import reika.dragonapi.instantiable.data.blockstruct.OpenPathFinder.PassRules;
import reika.dragonapi.instantiable.data.immutable.BlockBox;

/**
 * V33a {@code BreadthFirstSearch}: grows every search head one block per cycle from the root until a head steps onto a
 * terminus (that head's path, plus the terminus, becomes the result) or every head is exhausted. The port's
 * {@link AbstractSearch} takes its propagation and termination conditions per call rather than holding them, and its
 * result is the bare path, so V33a's {@code FoundPath} wrapper collapses to the returned list (empty when none was found).
 */
public class BreadthFirstSearch extends AbstractSearch {

	private final Collection<SearchHead> activeSearches = new ArrayList();
	private final Collection<SearchHead> exhaustedSearches = new ArrayList();
	private final ArrayList<SearchHead> currentlyCalculating = new ArrayList();

	public int perCycleCalcLimit = Integer.MAX_VALUE;

	public BreadthFirstSearch(BlockPos pos) {
		super(pos);
		activeSearches.add(new SearchHead(root));
	}

	private boolean calculateCurrentQueue(Level world, PropagationCondition propagation, TerminationCondition termination) {
		int cycles = 0;
		while (!currentlyCalculating.isEmpty() && cycles < perCycleCalcLimit) {
			SearchHead s = currentlyCalculating.remove(0);
			s.isExhausted = true;
			Collection<BlockPos> li = this.getNextSearchCoordsFor(world, s.headLocation);
			for (BlockPos c : li) {
				if (!world.isOutsideBuildHeight(c) && !searchedCoords.contains(c)
						&& this.isValidLocation(world, c, s.headLocation, propagation, termination)
						&& s.length() < depthLimit && limit.isBlockInside(c)) {
					s.isExhausted = false;
					if (termination != null && termination.isValidTerminus(world, c)) {
						activeSearches.clear();
						this.getResult().addAll(s.path);
						this.getResult().add(c);
						return true;
					}
					else {
						searchedCoords.add(c);
						activeSearches.add(s.extendTo(c));
					}
				}
			}
			if (s.isExhausted) {
				exhaustedSearches.add(s);
			}
			cycles++;
		}
		return false;
	}

	/** Note that the propagation condition must include the termination condition, or it will never be moved into! */
	@Override
	public boolean tick(Level world, PropagationCondition propagation, TerminationCondition terminate) {
		if (currentlyCalculating.isEmpty()) {
			currentlyCalculating.addAll(activeSearches);
			activeSearches.clear();
		}
		if (this.calculateCurrentQueue(world, propagation, terminate)) {
			return true;
		}
		return this.isDone();
	}

	@Override
	public boolean isDone() {
		return activeSearches.isEmpty();
	}

	@Override
	public void clear() {
		searchedCoords.clear();
		activeSearches.clear();
		exhaustedSearches.clear();
		currentlyCalculating.clear();
		this.getResult().clear();
	}

	public Collection<ArrayList<BlockPos>> getPathsTried() {
		Collection<ArrayList<BlockPos>> ret = new ArrayList();
		for (SearchHead s : exhaustedSearches) {
			ret.add(new ArrayList(s.path));
		}
		return ret;
	}

	public static LinkedList<BlockPos> getPath(Level world, double x, double y, double z, TerminationCondition t, PropagationCondition c) {
		return getPath(world, x, y, z, t, c, null);
	}

	public static LinkedList<BlockPos> getPath(Level world, double x, double y, double z, TerminationCondition t, PropagationCondition c,
			BlockBox bounds) {
		BreadthFirstSearch s = new BreadthFirstSearch(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
		if (bounds != null) {
			s.limit = bounds;
		}
		s.complete(world, c, t);
		return new LinkedList(s.getResult());
	}

	public static LinkedList<BlockPos> getOpenPathBetween(Level world, double x1, double y1, double z1, double x2, double y2,
			double z2, int r, Collection<PassRules> rules) {
		return getOpenPathBetween(world, BlockPos.containing(x1, y1, z1), BlockPos.containing(x2, y2, z2), r, rules);
	}

	public static LinkedList<BlockPos> getOpenPathBetween(Level world, BlockPos start, BlockPos end, int r, Collection<PassRules> rules) {
		return getOpenPathBetween(world, start, end, r, null, rules);
	}

	public static LinkedList<BlockPos> getOpenPathBetween(Level world, BlockPos start, BlockPos end, int r, BlockBox bounds,
			Collection<PassRules> rules) {
		OpenPathFinder f = new OpenPathFinder(start, end, r);
		f.rules.addAll(rules);
		TerminationCondition t = new LocationTerminus(end);
		return getPath(world, start.getX(), start.getY(), start.getZ(), t, f, bounds);
	}

	private static class SearchHead {

		private LinkedList<BlockPos> path = new LinkedList();
		private BlockPos headLocation;
		private boolean isExhausted = false;

		private SearchHead(BlockPos c) {
			headLocation = c;
			path.add(c);
		}

		public int length() {
			return path.size();
		}

		public SearchHead extendTo(BlockPos c) {
			SearchHead s = new SearchHead(headLocation);
			s.path = new LinkedList(path);
			s.path.add(c);
			s.headLocation = c;
			return s;
		}

	}

}
