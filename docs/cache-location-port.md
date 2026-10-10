# Typed tile caches and unresolved locations — D26/D27

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta. Original reference: DragonAPI de1bcf1f,
Instantiable/Data/Maps/TileEntityCache.java and Instantiable/Data/Immutable/WorldLocation.java.
Current native source contracts are read from the Gradle-generated 26.3 Minecraft source jar.

## Runtime cache contract

Erasure makes a cast to V incapable of checking a requested subclass. TileEntityCache now
accepts Class<V>, including interfaces implemented by block entities. Typed puts and putAll
validate before mutation; putAll validates every value first. NBT loading requires this token,
uses isInstance/cast, skips missing or incompatible block entities and reports incompatible
entries at the point of loading. Invalid dimension strings are reported and skipped without
losing valid entries later in the list. NBT remains the original location-only locs list.

The no-argument constructor still supports arbitrary location/value maps and LocationEntry
objects. It rejects block-entity NBT resolution before clearing its existing contents, rather
than pretending to know an erased subtype. It does not invent a serializer for arbitrary map
values. Accepted ReactorControlLayout and PebbleBedArrangement use their actual BE classes;
CrystalNetworker uses its CrystalNetworkTile interface and actual pylon class. The excluded
DungeonGenerator metadata map retains its ordinary map behavior and is not newly admitted.

## Locations without live worlds

Chunk identity, move/copy/to2D and decimal offsets are values of dimension key and coordinates;
they do not resolve a level. Client-side identity is retained across moves without holding any
client Level reference. Existing explicit logical-side resolution remains: a server context may
resolve the named dimension, while a client context must itself match that dimension. Missing
dimensions never fall back to the caller's world.

Block/state/key, block-entity, sky and loaded-chunk read helpers do not force chunk generation.
Unresolved state/key/BE queries return null; sky/loaded/air queries return false. BlockGetter
views are still supported, with actual Level instances routed through dimension-aware lookup.
BlockKey captures the actual state, including facing/properties, instead of a dry default.
Original numeric NBT dimensions (-1/0/1 and stable legacy custom keys) and current identifier
NBT are supported. Malformed identifiers fail explicitly. Ordering uses dimension then x/y/z,
so unequal locations cannot disappear on hash collisions.

The original hit constructor uses the block actually hit, including exact face-boundary hits.
The original item-drop helper is restored with random offsets inside the block, a copied stack,
pickup delay and the supplied velocity scale. It runs only on the server. The coordinate-only
isWithinDistOnAllCoords contract remains unchanged; isWithinSquare again includes dimension.

## Validation boundary

Nine unit regressions cover type-token/map/interface contracts, atomic rejection, unresolved
chunk/near queries, no retained Level field, numeric/current persistence, ordering and safe
sentinels. Four native ReactorCraft fixtures exercise class/interface filtering, malformed NBT,
actual CPU control-rod reload with a chest in its saved locations, actual state properties,
missing dimensions/unloaded chunks, face-boundary hit identity and zero-velocity item drops.
The crystal-network consumer compiles against its new tokens. This does not claim a remote
client session, arbitrary LocationEntry serialization, or full reactor-network acceptance.
Normal Play and its save directory are not used by these checks.
