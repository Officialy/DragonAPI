# Explicit vertex emission on Minecraft 26.3

`TessellatorVertexList` previously traversed its vertices into an empty
`addToTessellator()` body. Its 1.7.10 implementation (DragonAPI `de1bcf1f`,
`Instantiable/Rendering/TessellatorVertexList.java`) emitted vertices into the
global tessellator. Explicitly colored vertices called `setColorOpaque_I`;
uncolored vertices inherited the preceding color, and vertices without UVs
inherited the current texture coordinates. The packed color's alpha was ignored.

The common geometry class now emits to `render(VertexSink)` or
`TessellatorVertex.addToTessellator(VertexSink)`. An immutable `VertexData` record
retains coordinates, UVs, color and presence flags. `snapshot()` copies current
geometry in order and returns an unmodifiable list. It survives later transforms,
clear, push/pop, and edits made by a sink during emission. The mutable source list
still belongs to its constructing thread; snapshots are the publication boundary.
Original transforms, winding rules, stack operations and `addAll` semantics are
preserved. The original `addVertex` name is restored alongside `vertex`, and UVs
retain original double precision until native emission converts them to floats.

`reika.dragonapi.client.TessellatorVertexRenderer` owns the physical-client API:

```java
var attributes = new TessellatorVertexRenderer.Attributes(
        0xFFFFFFFF, 0, 0, light, overlay, 0, 1, 0);
var after = TessellatorVertexRenderer.render(vertices, pose.last(), buffer, attributes);
// Pass after to a subsequent list when the same draw sequence should inherit state.
TessellatorVertexRenderer.submit(vertices, pose, collector, renderType, attributes);
```

The caller owns the native `VertexConsumer`, topology, texture/render type and
render pass. The helper never begins, builds, uploads, closes or globally installs
that buffer. Every vertex receives complete position/color/UV/light/overlay/normal
data. Initial color uses native ARGB; a legacy per-vertex color forces opaque RGB,
which then persists for following uncolored vertices. UVs also persist until
explicitly replaced. Normals use the supplied pose's native normal transform.
`render` returns the final attributes instead of leaking global state.

`submit` captures the list and a copied pose before handing a callback to the native
`SubmitNodeCollector`. Empty lists add no callback. A callback captures no mutable
source geometry, live world or shared attribute state, so subsequent source edits
and replay cannot alter it. The collector supplies the pose used at execution.

The removed zero-argument rendering methods require callers to supply an explicit
destination. Remaining original calls are in excluded, pristine ChromatiCraft
renderers/WorldBoxHelper or commented RotaryCraft code; they must migrate to this
contract when those clusters are ported. No active consumer needs global rendering
state. These files are not modified or excluded further to conceal incompatibility.

Four common unit cases verify exact source attributes/precision, explicit emission,
immutable/reentrant snapshots, transforms/winding, stack restoration and independent
vertex copies. The dedicated server's common-side audit additionally executes the
snapshot/sink API and reflects both geometry classes without client linkage.
The isolated client fixture constructs real `BufferBuilder`/`MeshData` objects in
the inspected 26.3 `ENTITY` format and checks their bytes: position, inherited UV
and color, forced alpha, light, overlay and normals. It also captures the native
collector callback, mutates the source list/pose, executes it twice, and verifies
identical meshes and owned allocation cleanup. This is native mesh/collector
contract validation; it does not claim a screenshot or GPU draw of a migrated
ChromatiCraft consumer. Normal Play never runs the fixture.

ReikaRenderDispatcher remains open (D01). Its last working implementation
(`762c437d`) replaced `Minecraft.blockRenderer` and appended custom tessellation
before delegating to vanilla. 26.3 removed that dispatcher. NeoForge's actual
`AddSectionGeometryEvent` provides section callbacks on the meshing worker, requiring
safe captured inputs and the native chunk layer/vertex contract. GeoStrata ocean
spikes already have a separately registered BER; reviving their old renderer
alongside it would duplicate geometry. This D04 change does not silently activate
that dead path or claim D01 acceptance.
