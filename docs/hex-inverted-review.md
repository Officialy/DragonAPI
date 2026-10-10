# HexGrid and inverted weighted selection — D28/D29

Reference: DragonAPI de1bcf1f Instantiable/Math/HexGrid.java and
Instantiable/Data/WeightedRandom.java. Current target is Minecraft 26.3.

HexGrid line drawing interpolates only cube coordinates. FractionalHex.lerp takes the
explicit output-size argument and never reads either endpoint's size field. The suspected
b_nudge a.size reuse has no effect on coordinates or returned sizes, including mixed-size
endpoints. The original explicit size contract is retained and documented rather than
inventing interpolation between physical grids. Three regressions exercise mixed sizes,
explicit result size, reverse/zero-length paths, endpoints, adjacency and the cube sum.
There are no active lineDraw consumers; legacy Chroma lore rendering remains deferred.

The separate InvertedWeightedRandom uses cumulative interval thresholds; repeated results
retain separate append-only intervals. It now uses RandomSource with explicit seed/RNG
control, returns null for empty pools, ignores zero-mass entries instead of overwriting the
previous threshold, rejects negative/nonfinite/overflowing or unrepresentably tiny weights
before mutation, and uses half-open intervals so exact thresholds choose the following
entry. The rounded upper-bound fallback remains in the last positive interval. Clear
resets entries and total. Four regressions exercise zero/invalid/overflow/tiny weights,
RNG lower/exact/upper boundaries, duplicate results, seeded repeatability and clear/reuse.

Source/caller review found no inverted-selector persistence API in either original or port
and no consumers of the nested selector anywhere in the project. Main WeightedRandom's
NBT API and its earlier persistence regressions are separate; they are not evidence of
inverted serialization. No new format or unnecessary persistence API is invented here.
All 125 affected-module units pass (83 DragonAPI, 34 RotaryCraft, 8 ReactorCraft), including
seven new regressions; build/hex-inverted-validation.log (1m 4s). No GUI/native world behavior
changes are required for these pure algorithms. D28's historical bug claim is corrected;
D29's demonstrated selection bugs are fixed. Normal Play remains separate.
