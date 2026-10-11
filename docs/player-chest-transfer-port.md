# Player-to-chest transfers — D17

The original `de1bcf1f:Libraries/ReikaPlayerAPI.java` public array helper only counted
unique stacks in `mainInventory` and returned early; it never implemented a transfer.
The accepted port retained an empty body. There are no current project callers of
this API. This batch implements the explicitly requested intended behavior, rather
than attributing a nonexistent transfer algorithm to the original source.

The source remains the original main storage/hotbar contract: all 36 current
`Inventory.getNonEquipmentItems()` slots, in slot order. Armor, offhand, body armor
and saddle equipment remain equipped. Native `getContainerSize()` includes these
additional slots and would broaden the original mainInventory scope.

For each nonempty source stack, component-equal destination slots accept only their
remaining capacity; empty slots accept up to the actual item/container limit.
Oversized inputs split across slots and nonstackable items respect their one-item
limit. Distinct components never merge. Exactly the accepted amount leaves the
source; its remainder stays in the same slot, and a fully moved slot becomes EMPTY.
Input stack objects are not modified, destination writes retain complete components,
and the player's native inventory is marked changed when anything moves.

The original `ItemStack[]` entry point remains available, including null legacy
destination slots. The array has no owning container reference, so its owner must
mark destination storage dirty and expose the updated slots as appropriate.
The added native `Container` overload uses resource-dependent capacity and placement
filters, persists merges even when reads return copies, and marks native destination
storage changed. Transferring into the same player inventory changes nothing.

This is a partial transfer: full and zero-capacity destinations preserve all
leftovers, and repeated blocked calls cannot duplicate or discard items. It does
not require an artificial minimum number of free slots or reject an entire stack
when a partial amount fits. Both entry points require the owning server thread
before either inventory is read or written; client/off-thread misuse fails explicitly.
Normal server menu synchronization observes the resulting native inventory changes;
the helper does not manufacture client inventory packets or spawn dropped leftovers.

The shared partial-insertion helper now accepts native Container while retaining its
existing Inventory overload. Simulated insertion is read-only and applies the same
filters and resource-dependent limits as a real insertion.

Two family-server regressions cover exact array transfers, partial/full/zero capacity,
component separation, oversized nonstackables, the farthest main slot, equipment
preservation and repeated calls. The native-container fixture uses an actual chest,
then a snapshot-reading filtered container with component-dependent limits, observes
dirty notifications, checks simulation, rejects off-thread mutation and verifies
self-target behavior. These checks require real registries rather than unbound
synthetic item prototypes. Evidence: `build/player-chest-transfer-validation.log`.

This does not complete the separate Item Cannon powered-chain acceptance, arbitrary
foreign container adapters, menu interaction or remote-client acceptance tasks.
