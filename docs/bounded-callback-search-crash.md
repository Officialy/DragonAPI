# Bounded callback flood-fill crash — 2026-10-11

The supplied 17:06:02 report describes an exception in the server tick loop.
`BlockArray.recursiveAddCallbackWithBounds` exhausted the Java stack. While handling
that overflow, `throwOverflow` attempted to initialize a string-concatenation call
site on the exhausted stack. The visible `BootstrapMethodError` and
`StringConcatException` were secondary failures; the cause chain ends in
`StackOverflowError`. The report was emitted on the render thread, but the failing
operation is a world search. Its truncated stack does not identify the initiating
item. No shader fault is established by this report.

## Implementation and compatibility

Both public bounded callback search methods retain their signatures and now use
one queue-based flood fill. Work consumes heap space proportional to the admitted
positions and constant Java call-stack space. Each accepted position is enqueued
once, independently of existing array membership, and is processed in breadth-first
order. The six-axis neighbor order remains east, west, up, down, south, north;
`extraSpread` admits the other 26 neighbors, excluding the current position.

The two callback contracts remain distinct:

- `recursiveAddCallbackWithBounds`: `from` is always the fixed search origin.
- `iterativeAddCallbackWithBounds`: `from` is the actual predecessor, including the
  root's initial self-check. Rejection from one predecessor does not mark a candidate
  visited; another accepted edge may still admit it.

Depth starts at zero and includes `maxDepth`; breadth-first admission reaches a
position by its shortest accepted sequence of hops. Requested inclusive bounds,
array bounds, native world bounds, optional Manhattan-distance limits and the
loaded-chunk clamp are checked before the callback. Inverted requested bounds return
without visiting positions. A clamped search never force-loads a chunk. The old
iterative variant did not consistently apply depth/distance settings and relied on
array membership for termination; the shared implementation corrects those defects.

Pre-existing array entries do not prevent traversal through those positions.
Callbacks retain their exceptions rather than disguising them as successful or
partial overflow recovery. Searches do not write to the world or change scheduling.
`throwOverflow` uses parameterized logging for the other, still-recursive APIs.

Insertion order changes from recursive depth-first admission to deterministic
breadth-first admission. The accepted Subterranean Route Finder consumes the minimum
height and performs a separate path search, so its behavior does not depend on the
old array insertion order. The Transition Wand and Reactor CPU source callers remain
legacy/excluded consumers; their future ports must account for this ordering contract.

## Regression coverage

Three native DragonAPI fixtures in RotaryCraft cover a 20,000-cell connected line,
inclusive shortest-hop depth, existing membership, six/26-neighbor spread, array
and requested bounds, Manhattan distance, unloaded chunks, native world height,
inverted bounds, exception identity, fixed-origin versus predecessor semantics,
and later admission through an edge after rejection from another predecessor.

The real registered ChromatiCraft Subterranean Route Finder has a native sneaking
regression. A sealed chamber has 14x14x11 = 2,156 reachable cells below the starting
height. The test verifies successful use, actual trail blocks at the start and
deepest point, a trail block entity, and the unchanged supporting stone floor.

Validation logs and final counts are recorded in
`TestInstance/docs/callback-search-crash-2026-10-11.md` in the parent repository.

## Remaining limits

This change addresses the bounded callback APIs from the supplied crash. Other
recursive block/fluid/spherical searches remain for separate audit. Removing Java
recursion does not impose a new work budget: large open connected spaces can still
consume significant heap and server-tick time. The route finder's original range,
traversable-block rules, depth settings and synchronous use behavior are retained.
An incremental traversal would be a separate behavior/scheduling change.
