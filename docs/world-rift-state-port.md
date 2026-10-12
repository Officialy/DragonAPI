# World Rift state contract — D31

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta. Original reference:
ChromatiCraft upstream/master 1a5e514d61630995b6c0477368adfd7b0f91887c,
TileEntity/Transport/TileEntityRift.java and API/Interfaces/WorldRift.java.
Native reads/writes follow the Gradle-generated minecraft-patched-26.3.0.51-beta-sources.jar.

## Complete states and original direction rules

The exported DragonAPI WorldRift contract now requires getBlockStateFrom(Direction).
ChromatiCraft's public interface inherits it, so a caller using either API receives
all actual properties, including facing, waterlogging and shape. getBlockIDFrom stays
available as a default method derived from that state, with the same null sentinel.
The fake-zero getBlockMetadataFrom method is removed. No current project callers or
other implementations use it; there is no general conversion of native states into
legacy four-bit metadata. External implementers must provide the typed state method,
and old metadata consumers must choose the actual properties they need.

The original API prose says every directional read uses the requested face, but the
V33a implementation disagrees. getBlockFrom(dir), getBlockIDFrom(dir) and metadata
reads use target.move(dir.getOpposite(), 1). getTileEntityFrom(dir) uses the far rift's
neighbor on dir itself. Both existing conventions are retained and documented. An
EAST query reads a state west of the far rift and a tile entity east of it. The
current RotaryCraft/ElectriCraft consumers use tile entities and link locations;
their direction/power/network paths are unchanged.

State reads resolve the far dimension from the owning level's logical-side context
on every call, including after block changes, relinking and reset. An unlinked rift
or missing dimension returns null; resolved air returns the actual air state/block.
Reads use the native Level.getBlockState path, retaining on-demand server chunk reads
from V33a. No state cache, global integer state ID or client chunk generation is added.

## Coordinate helper migration

ReikaWorldHelper's unused zero-returning getBlockMetadata and empty
setBlockMetadataWithNotify are replaced by getBlockState and setBlockStateWithNotify.
The coordinate bridge accepts/returns complete states. Writes forward the caller's
native update flags and return Level.setBlock's boolean result, including bounds
rejection. No active or excluded project source calls the removed helper methods.
Other legacy metadata APIs in excluded source remain separate porting work.

## Validation

The native world_rift_states fixture creates actual registered rifts and chest
entities. It verifies all six mirrored state faces and requested tile faces through
DragonAPI's shared interface; multi-property chest/stair reads; changes, resolved
air, relinking and reset; coordinate-helper reads/writes; native bounds rejection;
and actual redstone-lamp notification through the helper's update flags.

world_rift_state_dimensions uses ChromatiCraft's public interface to read a real
Nether target with a different block at matching Overworld coordinates, then verifies
all six missing-dimension state/block/tile sentinels. Existing world_rift_link,
world_rift_far_link and world_rift_shaft_power exercise link placement, transfers,
redstone, recursion limits, distant targets and the real RotaryCraft power consumers.

These checks use the separate GameTest server directory. They do not certify an
actual remote client, external API consumers or optional foreign mod integrations.

## Results and fixture isolation

The initial world_rift_* batch passed both new fixtures, world_rift_link and
world_rift_shaft_power. world_rift_far_link failed its prerequisite that the far
chunk unload before the second placement. Its target is 208 blocks east of its
arena; the next arena begins 202 blocks east, so that target falls inside the
neighbor's held area. No production behavior or unload assertion was changed.
Running world_rift_far_link alone passes its actual unload, link and fluid checks.

Evidence: build/rift-state-native-validation.log (four passes and the overlap
failure), build/rift-state-final-validation.log (isolated distant-link pass and
successful affected unit suites, 1m 14s). All 132 units pass: DragonAPI 87,
RotaryCraft 34, ReactorCraft 8 and ChromatiCraft 3. Accepted primary outputs verify
across eight modules (3,259 classes). Normal Play VM/program argument hashes are
unchanged; no normal client or play save was used.

Reproduce the main batch with:

    gradlew :TestInstance:runGameTest -PexcludeExternalMods -PgameTestSelector=chromaticraft:world_rift_*

The unload fixture requires its own run while its target overlaps neighboring arenas:

    gradlew :DragonAPI:test :RotaryCraft:test :ReactorCraft:test :ChromatiCraft:test :TestInstance:runGameTest -PexcludeExternalMods -PgameTestSelector=chromaticraft:world_rift_far_link --offline --console=plain
