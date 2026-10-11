# Ore and declared-product catalogue — D38, Minecraft 26.3

Source reference: DragonAPI `upstream/master` at
`c29c4fcc2d3a65566783a4a163d8e682116e868c`, `ModRegistry/ModOreList.java`
and `DragonOptions.java`. Native references: the generated Minecraft
26.3.0.51-beta sources (`ReloadableServerResources`, `ClientPacketListener`,
`TagLoader`) and cached NeoForge 26.3.0.51-beta `TagsUpdatedEvent` sources.
The accepted RotaryCraft common-tag providers establish compound material paths,
Certus crystal/gem aliases, coal products and the historical lapis association.

## Associations and compatibility

The old reverse cache stored one entry per stack/name, so the last classification
overwrote earlier ones. Individual initialization also left reverse mappings from
the preceding generation behind. The commented original `getByDrop` searched
ore-block names, although the enum separately declares product names.

The replacement `OreCatalogue` retains every ore-block, declared-product and ore-name
association in one immutable generation. `getByDrop` now recognizes declared product
tags and preserves the original ore-block fallback. `getAllByDrop`,
`getAllModOresFromOre` (stack/block/state) and `getAllByOreName` expose all matches.
Singular compatibility APIs choose the first original enum entry consistently;
callers that need all classifications must use the plural methods. One entry cannot
occur twice in a reverse result, even when multiple aliases contain the same item.

Ore members must be block items. Product members may be ordinary items or block items.
Native item tag membership depends on item identity, regardless of stack count,
custom display components or placement properties. The catalogue retains original
enum ordering, ore aliases, product strings, colours, rarities and declared counts.
`getAllRegisteredOreBlocks` still enumerates each ore entry's blocks, including
shared associations. Returned product and ore stacks are independent one-item stacks;
each query uses the item's currently bound default components. Missing associations
retain the existing empty-list/null conventions. Legacy ordinal decoding remains.

`getProductItems` enumerates declared product-tag members. This API does not infer
recipes, scan loot tables, or claim every possible raw-material/fortune/silk-touch
drop is a declared product. For example, `ingotCopper` establishes copper ingots,
not an invented association to every copper-derived item. The historical `dyeBlue`
product recognizes both `c:dyes/blue` and `c:gems/lapis`, retaining the source's lapis
association and all current members of its declared dye tag.

## Native tags and configuration

Legacy category/material names translate to snake-case common tags, including
`oreNetherCopper` → `c:ores/nether_copper` and `ingotFzDarkIron` →
`c:ingots/fz_dark_iron`. The previous port's flat translations remain readable
aliases. The unusual source names `MonazitOre` and `ForciciumItem` have explicit
common-tag translations; Certus crystals also recognize `c:gems/certus_quartz`.
There is no global change to the generic legacy tag helper or recipe providers.

`getAllByOreName` and its singular adapter accept retained source names and resolved
native tag identifiers. Modern integrations can use `addOreTag(TagKey<Item>)`;
`addOreDictName` accepts native identifiers or valid retained legacy names. Invalid
names fail before changing a definition or publishing a generation. Alias mutations
and complete rebuilds are synchronized; a failed rebuild rolls the alias back and
keeps the preceding generation. Duplicate registration remains idempotent.

The original `CHARGEDCERTUS` option is restored with its original label and false
default. It is appended to the accepted option enum to avoid shifting existing port
ordinals, and uses the existing ControlledConfig legacy/TOML migration machinery.
On each rebuild, an enabled option includes the original `oreChargedCertusQuartz`
association and the verified charged-Certus native spellings. Disabled by default,
ordinary Certus does not acquire that association automatically. The option is
evaluated when rebuilding; a changed setting takes effect on the next tag update
or explicit initialization. Explicitly added integration aliases remain registered.

## Reload lifecycle

`initializeAll`, individual `initialize`, and alias registration build the complete
next generation before replacing the volatile snapshot. Removed/reassigned ore and
product tag members disappear from all reverse maps. Lists retained by callers
continue to describe their original immutable generation.

The DragonAPI listener now uses the root `TagsUpdatedEvent`, including server data
loads and remote-client tag packets. It uses that event's authoritative item registry
lookup. The integrated-client notification is skipped via `shouldUpdateStaticData`,
preserving the shared server-owned static catalogue. Manual initialization remains
supported against the built-in item registry.

In 26.3, the server applies tags and posts `ServerDataLoad` before applying new item
default components. Therefore this cache stores item identities and creates no stacks
while tags bind. Item enumeration constructs fresh stacks when queried after binding.
This removes the former dependency on `DefaultDataComponentsBoundEvent` without
using incomplete item prototypes during the earlier tag event.

## Validation and limits

Five new unit regressions cover overlapping ore/product/name associations, order and
deduplication, immutable generations, removal/reassignment, non-block filtering,
all original names' translation, special aliases and both charged-Certus policies.
Two native fixtures cover real loaded copper/coal/lapis product tags, detached stacks,
native tag rebinding, individual initialization, the server listener, remote/integrated
events, component-bearing queries, invalid registration and duplicate registration.
Bindings are restored synchronously in `finally`; unrelated server reload listeners
are not replayed without preparing their single-use reload data.

The completed family run passes 151 units (103 DA, 34 RC, 8 ReC, 6 Chroma), all 71
required native DragonAPI tests and eight accepted-class guards covering 3,271 primary
outputs. DragonAPI and RotaryCraft production jars build. Normal Play argument hashes
remain unchanged. See `TestInstance/docs/ore-product-catalogue-2026-10-11.md` and
`build/ore-catalogue-family-validation.log` for evidence and reproduction.

The native fixtures exercise event handling in a headless server; they do not establish
interactive visual acceptance or a live remote-client connection. Optional mods are
not installed or impersonated. D37 plank recipe provenance and D39 KeyedItemStack
matching semantics remain separate work. Pending worldgen, rift, startup compatibility,
IDE and launcher changes are outside this checkpoint.
