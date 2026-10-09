# Dye identity and cache lifecycle — D24, 2026-10-09

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta / Java 25.

Finding 1.43 was an empty `ReikaDyeHelper.buildItemCache` and absent item-colour lookup APIs.
ElectriCraft's accepted machine block also retained a disabled resistor-dye branch inside an
obsolete activation method. That branch could never run through the native interaction API.

The reference contracts are DragonAPI commit `de1bcf1f`,
`Libraries/Registry/ReikaDyeHelper.java`, and ElectriCraft V31a commit `950badc`,
`Blocks/BlockElectricMachine.java`. Current APIs were read from the generated patched 26.3
Minecraft source jar and NeoForge 26.3.0.51-beta source jar, including `DyeColor`,
`DataComponents.DYE`, `TagsUpdatedEvent`, registry tag reloads and native block interactions.

## Identity and lifecycle

The original sixteen constants, RGB palette and black-first damage order are unchanged.
Legacy damage/wool-index getters, colour names and stack factories now map to native
`Items.DYE` and `Blocks.WOOL` identities. Old ore-dictionary names remain readable compatibility
names; no runtime ore dictionary or hand-written recipe registration is introduced.

The cache maps item identities to every matching colour-specific `c:dyes/<colour>` tag.
All sixteen tags, including black, participate. A generic dye tag without a colour does not
invent one. An item's unrelated components or stack count do not affect its tag membership.

26.3's typed `DYE` component replaces the old dye metadata and is read from the actual stack
on every lookup, including custom pigments and stack-level overrides. It takes priority in
single-colour queries; multi-colour queries preserve all remaining tag memberships. Ambiguous
tag-only items choose the first colour in original enum order, replacing the old hash-set
iteration's arbitrary selection without dropping other colours. Results are immutable, and
null, empty and uncoloured stacks retain the original null/false lookup contract.

The cache is published as a complete immutable map. Manual `buildItemCache()` remains functional,
and first lookup can initialize it. DragonAPI registers the root `TagsUpdatedEvent` listener.
Server datapack loads/reloads and remote client tag packets rebuild from the event's registry
lookup. `shouldUpdateStaticData()` prevents an integrated client's duplicate update from
replacing its server's shared cache. Typed stack components are not cached, so changing one
cannot poison other stacks of the same item.

## Actual resistor consumer

ElectriCraft now uses native `useItemOn`, the supplied held stack/hand and the hit's block-local
coordinates. The original band starts, widths, horizontal-facing reversal and flipped Z-axis
mapping are preserved for both three-band and four-band resistors. Lime remains the original
GREEN resistor digit. Unsupported colours, between-band hits and grey/white multiplier bands
are rejected without consuming an item. Invalid public digit indices reject safely.

The server changes the band, recalculates the decimal current limit, updates an existing network,
consumes exactly one dye in survival, and synchronizes the changed block entity. Creative keeps
its dye. The client previews valid interactions without changing bands or consuming stacks.
Persisted/synchronized data retain their existing band/selected-current fields.

The same obsolete activation method contained real motor upgrade behavior. Its native migration
preserves the authored inductive/tungsten/bedrock limits of 2/4/1000 A, with a side-effect-free
client preview, server commit and duplicate/lower-tier rejection. Screwdriver/upgrade-item
actions retain item handling, and the transformer's existing native menu remains reachable
through vanilla's empty-hand fallback.

## Validation and limits

`build/dye-cache-validation.log` passes in 40s: 51 DragonAPI + 33 RotaryCraft + 8 ReactorCraft
unit tests, and all 19 native ElectriCraft tests. Four new units verify legacy identity/order,
typed overrides, null/empty/non-dye behavior, immutable results and stack preservation.
JUnit does not bind 26.3 item prototypes; those units use native direct holders with explicit
component maps, while loaded-server tests exercise the real registry-backed stack factories.

Six new native tests verify every dye/wool identity, actual tag additions/removals/recolouring,
multiple colour membership, the integrated-client guard and registered remote-client event
listener, tag-only pigments through the real resistor, 56 normal/precise orientation/flip/band
combinations, offhand/creative/survival behavior, original current arithmetic, rejection and
synchronized fields, and actual motor upgrades.

The reload fixture synchronously invokes native `prepareTagReload(...).apply()`, calls the
server dye-update callback with real server resources, and posts client tag events through
NeoForge's event bus. It restores every original binding and rebuilds the cache in `finally`.
It does not repost the server reload event to unrelated single-use reload listeners, and does
not claim a complete resource-pack reload or a real remote connection. Broader network
conservation and real multiplayer acceptance remain E02/N07. Existing native colour tags are
used; no new recipes, tags, textures or other data generation are needed.
