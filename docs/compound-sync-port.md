# Compound sync and dormant payload retirement — D44, Minecraft 26.3

Original source: DragonAPI `c29c4fcc2d3a65566783a4a163d8e682116e868c`,
`IO/CompoundSyncPacket.java`. Native references are the generated Minecraft 26.3
sources and NeoForge 26.3.0.51-beta PacketDistributor, level tick/lifecycle events
and native packet bundling. This batch uses the working D43 channel bridge.

## One transport owner

DragonPayloads was an unregistered early design with no repository callers.
Player/config/block-entity/popup handlers only logged; particle handling cast a
particle type to options and ignored count, and sound ignored its relative flag.
Activating those records would create a parallel transport with incomplete semantics.
Its exact prior committed source is retained in
`docs/reference/DragonPayloads-early-port.java.txt`; the former compound code is
retained in `docs/reference/CompoundSyncPacket-before-native-batching.java.txt`.
Reference attributes disable newline conversion so archived bytes remain exact.
These archives are outside Java source/resource roots. No registered content was
excluded to obtain a green build. The typed design compiled before its deliberate
retirement; no current sender, receiver or behavior depends on its classes.
DragonNetwork's useful namespaced identifier helper remains.

CompoundSyncPacket no longer claims a fabricated vanilla PacketType. It is a
server batching queue and a detached snapshot/merge API. Its outgoing deltas use
SyncPacket.fromChanges, the existing BE_NBT_SYNC category, original channel ID,
protocol 2.1.0 and existing byte framing. PacketDistributor's native bundle wraps
multiple existing clientbound payloads, capped to 256 per bundle. There is no
new custom payload registration, discriminator or alternate transport.

## Queuing and consumption

Queued full tags are copied. Delta comparisons use the last consumed baseline,
so multiple updates before a flush retain every outstanding changed field, keys
which disappear, and final-value reversions. Removed keys use the existing
SyncPacket removal marker. Force requests retain full-state policy. Flush captures
payloads, baselines, ranges and revisions before sending; a failed send leaves
pending work available for idempotent retry. Reentrant newer writes survive the
flush, including forced writes with identical values. Snapshot/merge accessors
return detached tags.

Queues accept writes and flushes only on the owning server thread. Cache entries
use weak tile references and stable registry type identifiers. Removal, replacement,
unloaded chunks and stale identities discard old work. Native world-end ticks use
separate per-dimension clocks, four ticks normally or twenty with SLOWSYNC; duplicate
callbacks in the same game tick do not advance the clock. Level unload/server stop
clear baselines and clocks. Recoverable automatic send failures are logged with
pending work retained; repeated errors are limited to one log per ten seconds.

Direct legacy setData callers retain dimension-wide recipient intent. The explicit
radius overload filters recipients per tile; BlockEntityBase uses its existing
normal/forced radius. Actual sends retain native negotiated-channel checks. No
recipient forces a chunk load. A new watcher receives the existing vanilla full
block-entity update; the batching queue does not replace initial chunk sync.

## Optional activation and mode transitions

COMPOUNDSYNC is a real configuration option, appended to the enum to preserve
other ordinals and defaulting to false. Default Play keeps the working per-tile
path. The option restores the original intended aggregate branch, using native
bundles. A transient per-tile mode flag forces authoritative state when toggled.
Enabling batching primes the previous per-tile full state into the queue, so keys
removed before the first batch still reach the client. The per-tile SyncPacket
continues tracking state/history in either mode. Disabling batching cancels old
queued updates and forces per-tile values/removals, preventing a later stale batch
from overwriting them. No config file is saved by the test's temporary value change.

The client receiver keeps the same type guard and only resolves loaded targets.
BlockEntityBase uses its established merge/removal application; implementations
of the original CompoundSyncPacketHandler interface outside that base receive a
detached adapter with readForSync. The casting table retains explicit EMPTY output
encoding, with its obsolete compound-deletion comment corrected.

## Standalone snapshot API

write/readPacketData serialize a versioned local snapshot, not a new network packet.
Snapshots contain dimension keys, full signed int coordinates, registry type names
and copied patches. Counts, duplicate locations, total size and trailing data are
checked. Failed reads preserve the previously decoded snapshot; failed writes
restore the caller's writer index. This dormant, previously unregistered packet
format has no published wire compatibility to preserve; its exact prior source
is archived. Application rejects wrong dimensions/types and unloaded client tiles.

## Validation and limits

Six new units cover difference/removal/force semantics, nested tag ownership,
unsent-field preservation and replacement order. Six native fixtures cover real
registered tiles/players, two-item batches, repeated writes, range exclusion,
failed-send retries, reentrant writes, entity replacement, dimension cleanup,
snapshot round trips/negative Y/atomic errors, actual config-driven base routing
and mode transitions, and off-thread rejection. Controlled send hooks capture
payload batches; this does not claim actual remote socket/client-render acceptance.

All 84 required DragonAPI native regressions and 170 family units pass, along with
eight crafter regressions. The focused final codec rerun covers oversize writer
rollback. Eight class guards verify 3,280 primary outputs (DA 669 / RC 1,022).
The development DA/RC jars build; the abandoned payload class and reference files
are absent from runtime jars. See `TestInstance/docs/compound-sync-2026-10-12.md`
in the workspace root for the evidence and normal Play argument hashes.
D45 tank sender semantics, D46 legacy wire sub-IDs and remote/production acceptance
remain separate tasks. D44 is complete for the selected native bridge migration.
