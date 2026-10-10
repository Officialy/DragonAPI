# Inventory and resource empty contracts — D30

Reviewed against DragonAPI `de1bcf1f` and RotaryCraft
`b2288638f7f4703bb4555f72a2b86f72407cc2ae`, the accepted 26.3 callers and the generated
Minecraft / NeoForge 26.3.0.51-beta sources. Native registries are required to test
component-bearing resource handlers; these checks run as GameTests rather than synthetic
unbound item prototypes.

## Inventory contracts and fixes

Legacy ItemStack arrays still accept null entries. Queries treat null, native EMPTY and
zero-count stacks as empty, without rewriting the queried slots. Native inventory writes
always use EMPTY. Empty slots no longer match AIR or an empty requested item, crafting
lists omit empty ingredients, and exact empty-slot counts use values rather than EMPTY
object identity. Array range checks retain their original exclusive upper boundary;
`isEmptyFrom` and transfer-count ranges retain their inclusive boundary.

Full-fit insertion is atomic for ManagedItemHandler: one native transaction commits only
if the whole offered stack fits. Capacity checks simulate the same transaction and abort.
Administrative `overrideValid` writes remain supported, but retain actual item-dependent
slot capacities through the handler's new resource-aware slot-limit overload. Merges
write back to storage, including Container adapters whose reads return detached snapshots.

Partial insertion preserves all components, splits oversized inputs across bounded slots,
and subtracts the remaining count after each insertion. Player inventory simulation respects
slot filters and leaves storage unchanged. These helpers retain their copy-input contract;
`addStackAndReturnCount` intentionally retains its separate consuming-input contract.
Single-slot output insertion rejects a stack that does not fit, instead of overwriting
an empty slot with an oversized stack. Closing/draining use EMPTY and mark native inventory
changes. Nonpositive decrements cannot increase a stored count.

Spilling runs on the server, clears each slot before spawning its actual stack, skips empty
entities, and retains components and pickup delays. Repeating a spill cannot duplicate
items. Transferable queries simulate extraction rather than treating insertion filters as
extraction policy. The smallest-stack selector now selects the smallest nonempty entry.
The previously empty unique-stack helper counts distinct item/component types, ignoring
counts and empty entries without mutating its argument; the separate player-to-chest
transfer remains D17, not completed by this helper.

The Line Builder's block selector returns the selected block before decrementing storage.
Consuming the final block now retains its identity and components after the slot empties.

## Accepted Item Cannon consumer

The cannon again consumes exactly the quantity delivered to the destination. Transfers
retain components, target locations survive detached native NBT loading, and null/self
targets cannot transfer or produce nonfinite motion. The original full-first-stack capacity
gate is retained in both stack and single-item modes, as are the short-lived visual item,
speed, pickup delay and vanilla explosion sound. Native WorldlyContainer / item capabilities
restore insertion from every side and extraction from the bottom only.

The native fixture directly exercises the private firing operation to isolate count/component
conservation from power acquisition. It tests single/stack mode, full destinations, target
persistence and native sided capability transactions. It does not claim complete powered
cannon-chain, GUI or audible client acceptance.

## Resource review

DirectResourceManager already returns Optional.empty and an empty resource stack for an
unregistered identifier. Its PackResources IoSupplier is nullable for missing pack entries,
as required by the actual native interface. DirectResource.open reports an IOException for
a registered source that cannot be opened. DynamicDirectResource converts its legacy
RemoteSourcedAsset null sentinel into FileNotFoundException. These are different contracts;
blindly replacing every null would break valid native behavior.

Existing native resource tests cover namespaces, source-pack metadata, listings, missing
identifiers and cache invalidation. Remote asset download reliability and excluded legacy
asset repositories are separate work; no network download behavior is claimed here.

## Validation

Six new native fixtures cover legacy arrays, atomic handler transactions, real player
inventories, actual dropped entities, last-block selection and actual registered cannons.
Affected-module unit suites and the family-loaded DragonAPI native suite are recorded in
the checkpoint ledger. The first RotaryCraft-only broad run passed 43/44 tests; its glow-tree
case requires ChromatiCraft to be loaded. The family launcher passes all 44 cases after
a complete compile restored missing accepted ChromatiCraft class outputs. All 127 units
pass (85 DragonAPI, 34 RotaryCraft, 8 ReactorCraft); see
build/inventory-family-full-compile.log (1m 14s).
