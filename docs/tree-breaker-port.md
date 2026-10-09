# Tree-breaker scan limits — D22, 26.3

Source reference: DragonAPI `de1bcf1f`, `Auxiliary/ProgressiveRecursiveBreaker.java`
(`getTreeBreaker`) and `ModRegistry/ModWoodList.java` (catalogue bounds).

The port had disabled all four special limits behind a comment claiming ModWoodList was absent.
The current catalogue is accepted and compiled. Its named identities retain these original limits:

| Tree identity | Maximum breadth-first waves |
| --- | --- |
| Natura Sequoia (`SEQUOIA`) | 350 |
| Twilight Oak (`TWILIGHTOAK`) | 200 |
| Twilight Darkwood (`DARKWOOD`) | 32 |
| Satisforestry Giant Pink Tree (`GIANTPINKTREE`) | 180 |
| All other catalogue woods and existing TreeType providers | 30 |

`TreeType.getRecursiveBreakDepth()` supplies the original default of thirty waves. ModWoodList
overrides it for exactly those four identities; `getTreeBreaker` now reads the descriptor's policy.
The new default method preserves existing providers and allows a modern provider to describe its
own scan limit without pretending to be a legacy enum entry. Natura Darkwood, BOP Redwood and
Pink Birch do not inherit the similarly named special tree's limits.

Spatial limits remain independent. The factory still offsets the descriptor's original
`getTypicalMaximumSize()` box by the origin in all three axes and retains inclusive limits.
No height-derived replacement depth, enlargement or unlimited scan is introduced. Sequoia retains
X/Z ±51 and Y ±251; Twilight Oak X/Z ±31 and Y ±181; Darkwood X/Z ±61 and Y ±21; Giant Pink Tree
X/Z ±49 and Y ±193. These values include the original BlockBox.origin() one-block margin.

The factory retains all native log and leaf states of the selected identities, diagonal spread
and the original separation between constructing a breaker and enqueueing it. The first wave
removes its origin; a depth of N admits at most N waves and leaves the next matching block intact.
Bounds or matching can finish an operation earlier. Completion still fires once.

Three unit regressions cover every catalogue entry's policy, the original four bounds and the
default inherited by an existing-style TreeType implementation. Six native regressions cover
exact stopping boundaries for 30/32/180/200/350 waves, native axis and leaf distance/persistence
states, unrelated species, completion, and actual registered ChromatiCraft glow wood. The glow
test also exercises diagonal leaf admission, all-axis box offsets and rejection beyond its box.

The four optional external mods are not impersonated or installed by these tests. Depth fixtures
use registered vanilla logs/leaves and delegate to the real catalogue depth policy, with a test
box large enough to isolate the wave limit. Actual catalogue bounds are tested independently;
the supported glow-wood test uses the real catalogue descriptor and registry blocks. Optional
mod registry/leaf association modernization remains the separate D36 task.

Validation passes in `build/tree-depth-validation.log` (1m 38s): 88 family unit tests
(44 DragonAPI, 33 RotaryCraft, 8 ReactorCraft, 3 ChromatiCraft) and all 33 native DragonAPI tests,
including the six new tree regressions and the existing block-array/sound/API checks. All active
family consumers compile. Normal Play argument files match the previous checkpoint and automated
checks remain disabled there. This batch does not change render or client-audit behavior; actual
remote-client and shader/LOD acceptance remain separate open tasks.
