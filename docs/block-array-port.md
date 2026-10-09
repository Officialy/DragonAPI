# Block-array API restoration — D20, 26.3

Source reference: original DragonAPI commit `de1bcf1f`, `Instantiable/Data/BlockStruct/`
and `Instantiable/Data/Immutable/Coordinate.java`. Current API references are the project's
generated Minecraft 26.3 source jar and cached NeoForge 26.3.0.51-beta sources.

## Coordinate transforms and declared data

`BlockArray`, `StructuredBlockArray` and `FilledBlockArray` support left/right 90-degree
and 180-degree rotations about an explicit X/Z pivot, plus X/Z mirrors. Exact integer
arithmetic replaces the original floating-point rotation path. For a left turn, relative
`(dx, dz)` becomes `(dz, -dx)`; Y is retained. Transforms return a new array of the same
kind, retain insertion order, recalculate bounds and preserve world/search settings.

The original methods transformed coordinates while retaining block metadata. Their modern
equivalent retains exact BlockStates and checks: it does not rotate facing/axis properties or
reinterpret NBT coordinates. All coordinate-indexed layers move together, including the
structured representative state, filled checks and placement overrides. Destination cells
are never sampled from a world, so declared layouts also transform during datagen or with a
null world. MultiKey containers are copied independently; their individual immutable or
caller-defined checks retain their original identity.

Replacements update the parent representative state as well as the filled check. Copying into
an existing array removes stale state/check/override entries; copying to itself retains data.
Merges import declared states without needing a world, preserve filled checks and placement
overrides, and keep membership consistent when configured bounds refuse a new coordinate.
Clearing an array resets its bounds as well as membership/data.

## Material requirements

`tally()` and the original coordinate-validity callback overload count one representative
item per admitted coordinate. Alternatives use their first representative, matching the
source contract. Empty/air/unitemized cells are omitted. A standalone flowing fluid block
item is excluded; native fluid constraints supply their container item instead. Stair
orientation does not invent separate material variants because the modern stair block states
share the same item.

The result uses the component-aware ItemHashMap mode so actual item component variants remain
distinct. Display stacks are copied and normalized to count one; their original counts and
components are never changed. Material tallying does not substitute world loot drops for the
structure's authored representative item.

## Sliced blueprints

`fillFrom()` delegates to the restored `putInto()` API. Mapped states, explicit air, ignored
cells and anti-mappings are retained. Import preserves the original row-left/column-up
orientation, adding the previously missing X/Z direction components so all four horizontal
facings work. The original checker uses columns across its plane and rows downward; that
separate historical reference convention remains intact. Its indexing now supports rectangular
slices instead of only working accidentally for square slices. No blanket transpose of existing
turbine definitions is introduced.

Anti-mappings now survive import as forbidden-block constraints. Placement leaves a permitted
existing block alone and clears a forbidden one to air. Matching and failure callbacks still
describe the actual forbidden cell. Empty rows and vertical/absent directions are rejected
before import mutation, since this API describes horizontal sliced structures.

## Block entities, fluids and empty-space checks

`setTile()` accepts modern BlockStates and entity prototypes, with an explicit registry-provider
overload for detached prototypes. TileEnum/class-only forms require the exact entity class without
inventing selected NBT values. BasicBlockEntityCheck snapshots the requested top-level NBT fields
using the current save API. Matching requires the exact state, exact class and equality of those
selected fields, as in the original tagContains contract. Changes to the prototype after capture
cannot change the snapshot. A weak prototype reference is available; class-only checks return null
safely when no prototype exists.

Placement overlays copied selected fields onto the destination's existing NBT, then loads through
26.3 TagValueInput/loadWithComponents and marks the entity changed. Unselected inventory/components
remain intact, and metadata does not relocate the destination entity. No ambient integrated-server
registry lookup is required.

Fluid checks normalize source/flowing identities to the native fluid family and preserve the
source-required, any-fluid and flowing-only policies. Placement uses actual source/flowing states,
not guessed metadata or finite-fluid quanta. A waterlogged solid cannot replace a standalone fluid
cell. Fluids without a representable required placement state fail explicitly. Empty checks and
isSpaceEmpty now honor their original soft-block allowance; explicit exclusions take precedence.

## Validation

Four focused unit regressions cover integer pivots/order/bounds/settings, inverse transforms,
world-free declared states, alternative-container independence, replacements/copies/merges and
rectangular imports in all four directions. Five native GameTests cover placement overrides,
component-aware material counts, rectangular checker/failure/anti-placement behavior, selected-NBT
matching/placement with unrelated inventory, entity classes/prototypes, fluid policies and soft
allowances.

`build/block-array-validation.log` passes all 85 family unit tests (DragonAPI 41, RotaryCraft 33,
ReactorCraft 8, ChromatiCraft 3), the changed library jar and all 18 native DragonAPI tests, including
the five new integration tests (1m 50s). The initially empty transformed bounds were repaired after
the focused test caught them; the complete final suite includes that regression. Client checkpoint
validation and packaging results are recorded in the current project backlog and ReactorCraft ledger.
