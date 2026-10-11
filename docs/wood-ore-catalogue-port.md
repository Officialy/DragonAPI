# Wood and ore state/item associations — D36, Minecraft 26.3

Source reference: DragonAPI upstream/master at
`c29c4fcc2d3a65566783a4a163d8e682116e868c`, `ModRegistry/ModWoodList.java` and
`ModRegistry/ModOreList.java`. Native reference: the Gradle-generated Minecraft
26.3.0.51-beta source jar, particularly BlockItemStateProperties and BlockItem.

## Wood association contract

The original getLogItemWithOffset argument indexes the declared metadata array; it
is not a numeric increment. The port had dropped the array and ignored the index.
It now indexes an explicit, ordered list of complete native BlockStates. The preferred
state comes first and invalid indexes fail rather than aliasing the preferred log.

WoodSpecies holds immutable log, leaf and optional sapling state sets. ofBlocks
expands the actual state definitions of distinct registered blocks, with their
default states first. The public constructor accepts exact lists for shared-block
species or multiple block identities. ofItems preserves an integration's supplied
item components and uses its native BLOCK_STATE properties to select species,
while expanding axis, leaf distance/persistence/waterlogging and sapling growth
states. Invalid provided property names/values fail explicitly.

Inventory enumeration retains block/species properties in BLOCK_STATE and deduplicates
world conditions: axis, distance, persistence, waterlogging and growth stage do not
create additional species items. Returned stacks are independent, including supplied
integration templates. States can be associated before item prototypes are bound;
state-derived item stacks are constructed only when an item query is made after
server data binding. A block-only variant has no obtainable inventory item.

ModWoodList resolves the accepted glow tree by its existing registered identities,
without reflective initialization in enum constructors. Modern external integrations
can call registerSpecies after registration. Existing distinct-block field and item
adapters remain; component-bearing item adapters retain their supplied components.
Shared legacy field names require the corresponding port's explicit state association.
Assigning every state of the same block to multiple old metadata species would lose
identity. These optional mods are not installed or impersonated by this batch, and
no unverified modern registry identifiers are invented for them.

Registration validates all reverse maps before publishing an immutable replacement
index. Conflicting species assignments fail without partially changing the existing
association. State and native component-bearing item lookups are precise. Block-only
lookups return an entry only when the block has one unambiguous species. Null, empty
and ordinary non-block items have no association. Missing mods cannot be registered
as present species. Loaded-mod queries return independent collection snapshots.

Natural-leaf checks first require membership in the declared leaf set. Ordinary
native leaves then respect PERSISTENT. The accepted glow tree explicitly retains
its authored never-decaying behavior, so its persistent worldgen leaves remain natural.
The old unconditional result for four mod families no longer misclassifies unrelated
blocks or placed leaves. No map-colour heuristic determines leaf membership.

## Active consumers and source parity

TreeType supplies compatible default log/leaf state lists for existing providers.
ModWoodList supplies its exact sets, and ProgressiveRecursiveBreaker consumes those
sets instead of every possible state of the primary block. Undeclared variants of
that same block therefore survive. Original maximum spatial boxes, diagonal spread,
30-wave default and 350/200/32/180 special limits remain unchanged.

ReactorCraft radiation now supplies the actual leaf BlockState to the catalogue,
while retaining its existing broad native LEAVES-tag behavior. Excluded legacy
ChromatiCraft files remain untouched; their metadata calls migrate with their full
subsystems rather than through numeric metadata overloads.

The glow tree has three log axes, 28 native leaf states and one registered sapling.
Those leaf world conditions produce one obtainable leaf item. The existing generated
leaf loot has the genuine glow sapling association: all 28 states return exactly
that item at the original guaranteed Fortune-X threshold, and the dropped item
resolves to the same species. The original five legacy leaf metadata entries are not five registered items in
the current accepted port. This batch catalogues actual registered states and does
not add invented items or textures or claim a new audit of their visual parity.

## Ore contract and compatibility

ModOreList now exposes BlockState classification and complete state enumeration for
its actual common-tag item members. Both stone and deepslate copper identities remain
available. Generic display/components do not become an ore damage discriminator.
First/random/all-item queries return detached stacks so callers cannot destroy the
catalogue by shrinking a returned item. Native RandomSource can be supplied for
repeatable/world-owned selection; the compatibility overload avoids allocating a
new Random on every call.

The original getEntryFromDamage implementation simply decodes an enum ordinal.
Deleting it would lose old saved-data compatibility; converting its integer to a
block state would change its meaning. getByLegacyOrdinal names that operation,
retains exact historical ordering/bounds, and the deprecated old spelling delegates.
New live code uses block state/item lookups; no active production caller relies on
ore item damage.

D37 remains separate: plank lookup still requires a real loaded crafting recipe
and a PLANKS-tagged block result. The selected input now preserves the species item
components, and block-only logs safely have no recipe input. No glow plank recipe
or association is guessed. D38 was completed in the subsequent ore/product catalogue batch;
see [the D38 contract](ore-product-catalogue-port.md) for complete product lookup,
multiple classifications, native tag lifecycle and reload replacement.

## Evidence

Five new state/legacy-ordinal unit regressions pass. Four new native catalogue tests
exercise real glow blocks, every leaf state's actual loaded loot, shared-block item
components, supplied integration components, multiple leaf/sapling blocks, detached
stacks, native RNG, stone/deepslate ore states and the actual recursive breaker.
The final full DragonAPI native suite passes all 69 tests, including the original
six depth/bounds fixtures. Family units pass 146 tests (98 DA + 34 RC + 8 ReC + 6
Chroma). Eight accepted-class guards verify 3,268 primary outputs. Native item/loot
checks use bound server data rather than rebinding global registries in unit tests.
Details and logs: TestInstance/docs/wood-ore-catalogue-2026-10-11.md.
