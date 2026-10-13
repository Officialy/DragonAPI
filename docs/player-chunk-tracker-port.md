# PlayerChunkTracker ownership and native loading — D49, 2026-10-13

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta.

Original contract: DragonAPI c29c4fcc2d3a65566783a4a163d8e682116e868c,
Auxiliary/Trackers/PlayerChunkTracker.java. Tracking suppresses loading around the
selected player, retains a 40-tick grace period while the condition is false or the
player is dead, resets that grace when the condition becomes true, and calls
onUntrack once when an accepted registration is retired. Renewing the same owner
replaces the condition and resets its timer, as in the original. The original
PlayerManager bridge is preserved as a native ChunkMap bridge.

The previous port retained two Player-keyed HashMaps with different locks and an
unsynchronized isInTick flag. Queries read the maps without locking; registration
could race tick iteration and queue merging. No lifecycle handler retired player
entries. Expiry wrote Double.MAX_VALUE to xo/zo, which are interpolation fields,
not the old managedPosX/managedPosZ chunk-manager markers.

The existing MixinChunkMap cancelled updateChunkTracking only. In 26.3 this method
updates the client-facing ChunkTrackingView. Loading participation is separate:
ChunkMap.move evaluates skipPlayer and updates DistanceManager player membership,
loading tickets, simulation tickets and ignored-player status. Cancelling view
updates alone did not suppress that loading work.

Actual API references are the Gradle-generated Minecraft 26.3 merged source artifact
mergeWithSources_012da5434d06693213a7a9a290d1150db864f83e_output.jar (ChunkMap,
DistanceManager, ServerChunkCache, ServerPlayer and PlayerList) and NeoForge's
26.3.0.51-beta sources (PlayerEvent and server/level lifecycle events). Sources/
Minecraft's older 26.2 signatures were not used.

Ownership and execution:

- Entries are keyed by the owning MinecraftServer and player UUID, and retain the
  exact ServerPlayer and ServerLevel binding. A replacement entity or dimension
  does not inherit suspension by sharing a UUID. Clients are never suspended.
- Registration, conditions, timeout changes, removal callbacks and native chunk-map
  mutations execute on the owning server thread. Off-thread requests are queued on
  that server. A queued registration captures its source level and is rejected if
  its player has moved, disconnected or been replaced before application.
- Queries use a concurrent map; tick iteration uses an entry snapshot. Callbacks
  run outside map locks. Identity checks discard stale iteration results after a
  callback renews a registration. New entries are evaluated on subsequent ticks.
- Lifecycle cleanup retires only matching owners. Logout matches the exact player;
  respawn/dimension cleanup retires the old binding while preserving any fresh
  registration on the current binding. Level unload and server stop are scoped to
  their level/server. Cleanup callbacks cannot resurrect invalid sessions.
- Terminal logout/unload/stopped identities are held weakly, preventing late
  registrations while avoiding strong ownership of a previous world/server.

Native behavior:

MixinChunkMap now also participates in skipPlayer. Starting suspension runs native
ChunkMap.move, removing that player's DistanceManager loading participation without
removing other players or unrelated forced tickets. The existing view-update gate
retains the suspended player's current view. Position bookkeeping and native entity
tracking continue normally.

Expiry or explicit stop removes the registration before invoking onUntrack, then
uses native move and updateChunkTracking to restore tickets and the view at the
current position. The latter has an invoker on the existing ChunkMapAccessor so
an unmoved player does not need a fabricated movement marker. A callback that
registers a fresh suspension keeps the native gates active. Interpolation fields
are never rewritten.

Logout/unload/stop cleanup does not restore tickets into a departing world. Runtime
exceptions from a condition are logged and release that registration; exceptions
from onUntrack are logged and do not prevent native restoration or other players'
evaluation. Removal happens before callback invocation, so notifications cannot
repeat for the retired entry. Fatal JVM errors are not swallowed.

The tracker still runs at server-tick END. Forty false/dead evaluations are forty
server ticks, normally two seconds at 20 TPS. Conditions do not run for dead players.
The public start/query/condition APIs remain; stopTrackingPlayer explicitly releases
a current registration, and getTrackedPlayerCount provides scoped diagnostics.
The current accepted gameplay caller remains MixinChunkMap; this port does not
invent a machine or ability that registers a condition.

Native regressions: RotaryCraft's DragonAPIChunkTrackerTests inspects actual
DistanceManager player membership and ChunkTrackingView, exercises the exact grace
boundary/reset/death behavior, replacement from condition/removal callbacks,
runtime failure recovery, real teleport/respawn/logout, stale UUID ownership and
queued worker starts/stops. TestInstance's disposable audit verifies integrated
movement/view restoration and real logout/unload/server-stop cleanup. Normal Play
does not run these fixtures.
