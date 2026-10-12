# Dimension-scoped packets — D47, 2026-10-12

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta. Source reference: original
DragonAPI `c29c4fcc2d3a65566783a4a163d8e682116e868c`, APIPacketHandler,
Libraries/ReikaEntityHelper and Command/BiomeMapCommand. Native palette, registry,
client tint-cache and section-render APIs were checked against the generated 26.3
Minecraft source artifact.

## Deliberate protocol migration

The old port ignored ENTITYVERIFY's dimension, replied with ResourceKey.hashCode(),
ignored that reply's dimension and emitted zero for BIOMEPNGSTART's dimension.
It also called a server-only biome setter from the client BIOMECHANGE receiver.

CustomNetworkBridge now negotiates **2.2.0** through its actual PayloadRegistrar.
The following six controls require the RAW category and a new strict body. Old
2.1 peers are incompatible; update both ends together. All 353 published numeric
IDs and native channel/payload identities remain unchanged. The 2.1 ID manifest
remains a historical fixture, not a schema declaration for these new bodies.

The native DataPacket frame remains category byte + varInt body length + body.
The new body is: control int32, dimension string, name string, fixed int32 fields.
Strings use the existing short character count + UTF-16 character encoding. A
dimension must be an explicit namespaced key of the DIMENSION registry. Numeric
legacy dimension IDs, runtime hashes and implicit current-dimension substitution
are never decoded. The complete body, including EOF, is checked before dispatch.

| Control | ID | Direction | Name | Integer fields |
| --- | ---: | --- | --- | --- |
| BIOMECHANGE | 0 | clientbound | Namespaced biome key | x, z |
| BIOMEPNGSTART | 32 | clientbound | Original world/seed name | session hash, origin x, origin z, range, resolution, grid, full-grid flag |
| BIOMEPNGDAT | 33 | clientbound | Empty | session hash, PACKET_COMPILE triples of x/z/synchronized biome registry ID |
| BIOMEPNGEND | 34 | clientbound | Empty | session hash |
| ENTITYVERIFY | 42 | serverbound | Empty | entity ID, verification class-name hash |
| ENTITYVERIFYFAIL | 43 | clientbound | Empty | entity ID, requested verification class-name hash |

DimensionPacketData is the common encoder/decoder and sends through the existing
DragonAPI PacketPipeline and PacketTarget. It creates no additional transport or
registration. BIOMECHANGE's biome key avoids conflating registry iteration order
with a synchronized numeric ID. PNG samples continue to use the actual synchronized
registry getId/byId pair and preserve the original batching and padding.

## Entity verification

The original verifyClientEntity API is restored. Server handling checks only the
requesting player's current dimension and its entity table. Requests from an old
dimension after teleport are ignored. Existing entities with the expected
class hash need no reply; missing/class-mismatched entities receive a failure in
the requested dimension with that same class hash.

The client requires its current dimension and the requested class hash before
discarding an entity. The common removal API independently rejects server levels.
Wrong-direction packets cannot remove a server entity or perform a client lookup.
Player implementations differ between logical sides, so all player subclasses use
the shared Player class hash. Other entities retain exact Java class-name hashes.
The original class-hash existence contract is otherwise retained; it is not a UUID identity
protocol and cannot distinguish same-class ID reuse within the same dimension.

## Biomes and map sessions

Every start/data/end packet now carries its source dimension captured at generation
start, including END if the original completion callback teleports the player. A different current
dimension rejects it. Each active PNG map also captures its actual Level instance;
samples and legends use that instance's registry, and data/end calls from another
instance discard the session. Client level unload and logout clear active maps.
Valid world/seed names, dimensions, batching, colors, grids, PNG/CSV output and
original callbacks remain. Normal world maps use the namespaced dimension in their
path; forced-seed exports retain their existing Forced/SEED path convention.

Client BIOMECHANGE updates only a loaded, registered target's biome quart column
across every section, preserving neighboring quarts. Native 26.3 stores biomes at
4x4x4 resolution rather than the old byte-per-block x/z column; this is the same
resolution used by native fillbiome. The client clears tint caches and dirties the
surrounding sections through the current renderer. Missing biomes/chunks are
ignored without generating terrain. Server setBiomeForXZ continues using native
FillBiomeCommand and its normal native synchronization; its inclusive getMaxY
bound is corrected for 26.3.

## Verification boundary

Unit fixtures cover independent wire bytes, custom keys, Unicode names, snapshot
ownership and invalid/old/truncated/trailing bodies. Four native fixtures exercise
real server entities and failure replies, Nether PNG start/data/end frames through
the actual bridge encoder/decoder, wrong directions, full-height palette changes
with neighbor preservation, and level-bound map cleanup. The disposable audit
client additionally exercises an actual integrated request/reply: a client-only
ghost is discarded, the real player survives, stale dimension/class replies are
rejected, and the production BIOMECHANGE receiver changes/restores client palettes.

The opt-in client check belongs to Automated Checks and does not activate in Play.
Integrated-client verification does not satisfy actual remote-client acceptance
or the user's Iris/Distant Horizons rendering acceptance. Final counts and logs
are recorded in TestInstance/docs/dimension-packet-2026-10-12.md.
