# Tank sender/recipient migration — D45, 2026-10-12

Minecraft 26.3, NeoForge 26.3.0.51-beta. Original DragonAPI source:
`c29c4fcc2d3a65566783a4a163d8e682116e868c`, `Base/TileEntityBase.java` and
`Libraries/IO/ReikaPacketHelper.java`.

## Source contract and activation

The original private syncTankData collected HybridTank fields and discarded them;
its only proposed caller in syncAllData was commented out. Consequently this was
not a functioning automatic per-tile tank transport to revive on every update.
The original working named-field helper sent tank levels to players within a
24-block radius in the tile's dimension. The current port already extended its
body with a fluid registry identifier, allowing empty client tanks to be filled.

BlockEntityBase.syncTankData is now a complete public explicit refresh API. It
discovers inherited instance HybridTank fields and sends them on the existing
DragonAPI channel using the named-field helper. Automatic initial/full/periodic
block-entity synchronization remains owned by the existing vanilla/BE_NBT_SYNC
paths. No extra per-tick reflective broadcast is added to every machine.

The three-argument sendTankSyncPacket retains the original 24-block radius and
same-dimension recipient intent. An explicit PacketTarget overload provides
clientbound recipient control through the existing PacketPipeline. Sending checks
the logical ServerLevel, owning server thread, loaded chunk and live tile identity
before reading tank state. It rejects unbound channels and malformed field targets
instead of dispatching partially encoded bytes. Native negotiated-channel checks
remain in PacketPipeline; no second transport or payload registration is added.

## Active menus

CoreContainer supplies a protected syncTank helper for its actual inventory owner,
using a PlayerTarget on the same logical server level. Thirteen accepted RotaryCraft
tank menus now use it. Each viewer's update goes to that viewer rather than being
rebroadcast to all nearby players for each open menu. Nearby rendering state still
uses the established block-entity synchronization and explicit nearby/tool sends.

ReservoirContainer no longer checks FMLEnvironment's physical dedicated-server
distribution. Its sender now also works on an integrated server. SteamContainer's
initial sendAllDataToRemote path is migrated along with the broadcastChanges paths.
Commented/pristine integrations remain preserved. Excluded Chroma containers and
ChromatiPackets are still source references until those subsystems are fully ported.

## Frame and receive behavior

TankSyncData provides a detached snapshot and one fully checked codec for the
current body: UTF-16 field name, three signed coordinate ints, amount int, UTF-16
fluid identifier. Existing TANK ordinals, frame bytes, payload/channel identities
and negotiated protocol 2.1.0 are unchanged. Names are bounded to the existing
short-length format; invalid levels, truncation and trailing bytes are rejected.
All four active DragonAPI/RotaryCraft/ReactorCraft/ElectriCraft TANK handlers use
the shared complete-body decoder before applying state. Serverbound TANK remains
forbidden. Receivers resolve only loaded targets and never create/load a tile.

Field discovery is cached per class with ClassValue. Private inherited fields and
HybridTank subclasses work; static fields, null values and shadowed ancestor names
cannot become instance targets. Ordinary unknown field targets are contained at
the receive boundary. Named unknown/malformed/empty fluid IDs do not fall back to
the previous fluid. Positive amounts clamp to capacity; zero empties the tank.
Same-fluid level updates preserve components already supplied by full BE sync.
The deprecated direct level-only API still resizes an existing fluid, but cannot
invent a fluid for an empty tank.

The retained legacy TANK body carries amount and fluid identity, not complete fluid
components or a block-entity type identifier. Component changes continue through
full/periodic block-entity tags. Extending this wire representation needs an
intentional negotiated protocol migration; this batch does not silently append data.

## Validation

Three new units cover golden frame parity (Unicode and full signed Y), malformed
frames, and private/inherited/subtype/static/shadowed field lookup. Fluid construction
requires the 26.3 registry component lifecycle, so fluid application is tested in
the real in-world environment rather than artificially binding registries in units.

Five native fixtures cover real registered reservoirs, levels, players, native
bridge encoding/decoding, 24-block and explicit recipients, detached snapshots,
actual reservoir-menu routing, actual base refresh, fluid fill/switch/clamp/empty,
unknown-ID rejection, component retention, thread/direction guards and removed
tiles. Controlled transport calls are captured; this is not a remote socket/client
render acceptance test. Temporary pipeline captures restore the production map in
finally and do not register a competing transport.

All 89 required DragonAPI native regressions and 178 family units pass. Final
receiver changes receive a focused five-fixture rerun; eight crafter regressions
check the shared container path. Eight guards cover 3,285 primary class outputs
(DA 670 / RC 1,023); DA/RC development jars build. Evidence is in
`TestInstance/docs/tank-sync-2026-10-12.md` and root `build/tank-sync-*.log`.
D45 is complete. D46 explicit packet sub-IDs and negotiated schema work remain open.
