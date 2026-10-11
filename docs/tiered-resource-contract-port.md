# Tiered-resource harvesting contract — D34, 2026-10-11

Source references: DragonAPI `c29c4fcc2d3a65566783a4a163d8e682116e868c`,
`Base/BlockTieredResource.java`; ChromatiCraft
`1a5e514d61630995b6c0477368adfd7b0f91887c`, `Base/BlockChromaTiered.java`
and `Block/Worldgen/BlockTieredOre.java` / `BlockTieredPlant.java`.
API references are the generated Minecraft 26.3 / NeoForge 26.3.0.51-beta
source jar and cached NeoForge 0.51 source jar, including native removal,
loot, dynamic entries, block-drop events and mock-player APIs.

## Shared player-removal contract

The original shared class suppresses vanilla harvesting and implements resource
selection in its player-removal hook. Returning false from `canHarvestBlock` is
intentional: it prevents the server game mode from running `playerDestroy` a
second time. It does not mean the player receives nothing. The missing removal
hook in the accepted DragonAPI class made this an incomplete port.

`onDestroyedByPlayer` now chooses loot while the original block and block entity
are still present, using the exact pre-damage tool snapshot supplied by NeoForge.
Removal delegates to the native fluid-aware client/server implementation. Only
successful server survival removal dispatches drops; creative removal produces
neither drops nor XP. Insufficient-tier plants yield nothing. Sufficient-tier
harvests retain the original 2–6 XP splash, including plants whose standalone
modern implementation had lost it. XP is processed by native tool enchantments.

Item entities retain native randomized offsets, pickup delay and block-drop /
snapshot-restoration checks. One `BlockDropsEvent` receives the original block
state, block entity, player, tool, mutable items and XP. Cancellation suppresses
both items and XP, and modifications are the values actually emitted. The
obsolete `HarvestDropsEvent` is not recreated.

## Accepted ChromatiCraft consumers and loot data

All 15 modern tiered ores and seven modern plants extend the shared class. Their
source-authored count formulas, stages, disguises, plant shapes/support rules,
Proxima tuning and per-player Elemental Stone roller remain authoritative.
Plant harvesting takes max(Fortune, Looting), including the native tool snapshot
when the tool breaks. Public integer-Fortune helper APIs remain available; host
loot uses a copied tool with that explicit Fortune without modifying inventory.

The current port's deliberate improvements remain: creative players may inspect,
target and place resources, and an insufficient ore miner receives ordinary host
loot (or the host itself with Silk Touch). Upstream's base computed the insufficient
drop collection but suppressed its spawning with a final `tier` check. This batch
preserves the accepted port's working disguise behavior rather than reproducing
that historical suppression. Sufficient Silk Touch yields resources, never the ore
or plant block itself.

The datagen provider writes 22 block loot tables with a native `minecraft:dynamic`
entry named `dragonapi:tiered_resource`. The shared class supplies its contextual
resources to that entry, then evaluates the reloadable native table and loot
modifiers. This replaces empty tables plus unconditional manual drops; table
conditions/functions or replacement tables control emission. Formula code remains
the contextual implementation, including progression, tuning and persistent roller
state; there is no runtime map replacing the reloadable loot registry.

Unowned loot contexts remain empty, preserving no generic explosion/support-loss
resource drops. Player-owned DragonAPI helper and recursive-miner APIs recognize
the actual registered ores/plants again through the shared type. Their explicit
resource queries retain their established direct collection contract. The shared
Silk Touch helper now safely handles an empty sufficient-tier collection.

## Placement and compatibility

Native `setPlacedBy` now rejects insufficient-tier player placement on the server,
broadcasting a break effect and removing the placed resource with neighbor updates.
This applies to plants as well as ores. Creative permission comes from the concrete
consumer's tier predicate. Non-player placement and world generation are retained.
The earlier `onBlockPlacedBy` spelling remains a delegating compatibility entry
point, while actual BlockItem placement calls the native override.

Client-only imports and the redundant `onPlace` passthrough are removed from the
shared class. Accepted randomness uses the owning level's RandomSource; the old
protected Random field remains only for compatibility with future source consumers.
Excluded legacy Chroma blocks/WAILA classes remain pristine and are not admitted
to the build by this batch.

Validation evidence and scope are recorded in
`TestInstance/docs/tiered-resource-contract-2026-10-11.md`.
