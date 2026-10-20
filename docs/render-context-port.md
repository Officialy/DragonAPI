# D06: explicit rendering context and lighting review

Status: in progress. Matrix getters have been replaced with explicit owned
snapshots. Active lighting callers now use explicit draw state, and the obsolete no-op
helpers have been removed. Excluded legacy callers still require migration. Backlog remains 57 complete / 107 open /
164 until the complete D06 contract is implemented and verified.

## Matrix contract

The previous getModelviewMatrix/getProjectionMatrix/getTextureMatrix called a
private getMatrix(int) that returned identity for every request. There are no
callers of the old parameterless signatures anywhere in the family source outside
their own implementation (full source search, including excluded and legacy-root files).
The parameterless getters have therefore been replaced by context-taking getters;
there is no silent identity fallback or mutable global current-context variable.

RenderMatrixContext is a common, JOML-only owned snapshot. capture receives an
already-composed model-view, projection and texture matrix. compose receives view
and model separately and multiplies view * model in that order. All inputs and
returned values are copied, so stack reuse, texture-supplier reuse, separate passes
and deferred callbacks cannot overwrite captured matrices. Identity must be
provided explicitly by a caller whose pass actually uses it.

ReikaRenderHelper.captureMatrices accepts the current native CameraRenderState,
a camera-relative model PoseStack and the draw's TextureTransform. It captures
camera.viewRotationMatrix * modelPose, camera.projectionMatrix and the result of
TextureTransform.createMatrix(). Camera-origin translation belongs in the model
pose exactly once. GUI, screen-space or other custom passes should supply their
own complete matrices through RenderMatrixContext.capture/compose instead of
borrowing a world camera. No OpenGL matrix enumeration, GPU readback or global
matrix-stack mutation is used by the production capture API.

26.3 references: generated minecraft-patched-26.3.0.76-sources.jar,
LevelRenderer render feature preparation (push model-view, multiply native camera
viewRotationMatrix), CameraRenderState and TextureTransform.createMatrix().
The original helper at DragonAPI de1bcf1f queried GL_MODELVIEW_MATRIX,
GL_PROJECTION_MATRIX and GL_TEXTURE_MATRIX. Those implicit global APIs have no
backend-independent modern equivalent; explicit pass inputs preserve the useful
transforms rather than returning invented identity values.

## Lighting source contracts and migration boundary

Original reference: DragonAPI de1bcf1f, Libraries/Rendering/ReikaRenderHelper.java.
Do not infer a shader profile from a helper's name alone:

| Legacy helper | Original operation | Required modern decision |
| --- | --- | --- |
| disableLighting | Disable lightmap, standard item lights and GL_LIGHTING | Explicit unlit shader and lightmap bypass/full-bright input for that draw |
| enableLighting | Enable lightmap, standard item lights and GL_LIGHTING | Choose the lit shader and actual packed light for the following draw |
| disableEntityLighting | Disable the lightmap texture unit only | Preserve surrounding directional-light intent; choose lightmap bypass independently |
| enableEntityLighting | Enable the lightmap texture unit only | Supply packed light without assuming a directional-light transition |
| prepareGeoDraw(alpha) | Disable lighting and texturing; enable blending when alpha is true | Choose untextured vertex layout, shader and per-draw blend state explicitly; alpha=false did not reset existing blend state |
| exitGeoDraw | Enable lighting/texturing and disable blending | Following submissions own their own pipeline; no global reset shim |

The uncommented full-source inventory has 95 qualified helper calls across 62
files after the reservoir migration: disableLighting 16, enableLighting 3, disableEntityLighting 55,
enableEntityLighting 9, prepareGeoDraw 7, exitGeoDraw 5. Comments and string tokens
were distinguished from executable source. Excluded legacy call sites remain
part of migration planning; they were not deleted, commented out or added to the
build without their dependencies. The current inventory is build/d06-reservoir-full-source-callers.json; the earlier
97-call/63-file inventory remains build/d06-full-source-callers.json.

Compiled bytecode at the matrix checkpoint identified RenderBevel and
RenderReservoir as external users. After the reservoir port only RenderBevel
remains, in its dormant face-number method; current evidence is
build/d06-reservoir-remaining-lighting-callers.json. This is narrower than file/class-name
matching: the preserved VoidMonster/Render/VoidGrowthRenderer.java has the same
class identity as a modern replacement and must not be mistaken for its compiled
implementation. Bytecode inventory: build/d06-active-render-helper-callers.json.

RenderBevel's calls surround a private renderFaceNumbers method which submit does
not call. Its native text submission already supplies full-bright light. The
native submit still has outstanding face-number/compass behavior; removing two
no-op calls alone would not restore those features.

RenderReservoir's helper calls are in renderLiquid, reached through an obsolete
public render signature, not native submit. At the matrix checkpoint native submit owned a separate fluid surface with
incorrect lighting and tint, while cover and legacy-fluid behavior were missing.
Those paths are now fully replaced by extracted native rendering state and
explicit lighting profiles, as detailed in the reservoir milestone below.

ChromatiCraft has accepted explicit pipelines and numerous excluded legacy
renderers. Their directional-light versus lightmap choices require checking each
actual original draw and its surrounding state. The UNLIT_ENTITY_CUTOUT comment
mentions disableEntityLighting while its NO_CARDINAL_LIGHTING shader choice acts
on directional lighting. That comment is not sufficient evidence to change the
pipeline: original surrounding GL_LIGHTING state must be traced first.

## Verification

DragonAPI units: 260 cases, zero failures/errors/skips, including four new matrix
regressions for non-commuting view/model order, defensive ownership, pass separation
and missing-input rejection. Family/TestInstance compilation passed.
Evidence: build/d06-matrix-unit-compile.log.

MatrixContextClientChecks compares the camera-based bridge with
RenderSystem.getModelViewMatrixCopy during a real AfterOpaqueBlocks event. It also
checks the native projection and a non-identity texture transform, then reuses
local input/output matrices to verify retained context ownership. The comparison
reads Minecraft's CPU-side native stack for validation; production capture uses
only supplied pass data. First compile corrected the test accessor name to the
actual 26.3 getModelViewMatrixCopy. Final native acceptance passed in
build/d06-matrix-client2.log (3m54s), with MATRIX_CONTEXT_CLIENT_AUDIT_PASS,
water-composition and full DRAGONAPI_AUDIT_CLIENT_PASS, followed by clean process
exit. This proves the matrix bridge and full client regression; it does not make
the still-unported lighting helpers complete. Evidence: build/d06-matrix-proof.json.


## Reservoir and independent lighting profile milestone (2026-10-19)

Added EntityLighting profiles separating directional lighting from lightmap use.
The fully rewritten reservoir renderer now captures native wall/cover/fluid state,
uses the registered fluid model/tint and selects FULL_BRIGHT for luminous fluids
instead of relying on the no-op disableLighting. Ordinary fluid uses LIT. Forward
and all three OIT stages exist for each profile and culling choice. The fluid's
component renderColor and original world-tint precedence are also restored.

Unit/native mesh/GPU and dedicated results are recorded in
RotaryCraft/docs/reservoir-renderer-port.md. The no-op call sites in RenderReservoir
have disappeared because their real behavior is now implemented. RenderBevel and
excluded legacy callers still require migration; the generic no-op lighting
helpers are not yet retired. D06 therefore remains open, with no checklist count
change. The earlier active-consumer inventory is a historical checkpoint.


Final reservoir orientation/scene milestone: combined units and native client
passed in build/d06-reservoir-orientation-client.log (10m38s). World/inventory
transforms, authored mirroring, all 120 inventory vertices, 32 GPU pipelines and
both actual transparency modes passed. The configured external-mod dedicated
profile passed all 1,004 required tests. Full pinned evidence and unchanged Play
argument hashes are in build/d06-reservoir-proof.json; the per-file contract is
RotaryCraft/docs/reservoir-renderer-port.md. D06 remains open for no-op API
retirement and remaining caller migration, with no checklist count change.


## Bevel migration and global-helper retirement

The bevel's native path now extracts all mutable state and restores source face
colors and centered colorblind labels. The intentionally disabled compass remains
disabled, with its complete geometry preserved. IO fill/line types have native
OIT pipeline sets. Details: RotaryCraft/docs/bevel-renderer-port.md.

The six no-op lighting/geometry helpers and two unused pseudo-state booleans have
been removed. The final bytecode inventory has zero references across all eight
modules (build/d06-bevel-remaining-lighting-callers.json). A preserved source
inventory still records 93 qualified calls across 61 excluded legacy files:
build/d06-bevel-full-source-callers.json. These are forward migration work, not
working fixed-function APIs. Apply the source-contract mapping above when each
subsystem lands; do not resurrect silent no-ops. D06 remains open while those
caller contracts are unresolved. No checklist count changes in this milestone.


## Focus crystal: source context resolves the misleading unlit profile

Original ChromatiCraft 5cde0068c^ proves that focus shards keep GL_LIGHTING enabled
while disableEntityLighting disables only their lightmap. The base precedes that
call and retains both systems. EntityLighting.cutout now owns all four lighting
choices with opaque alpha clipping/depth writes, explicit culling and correct
lightmap binding. Focus uses LIT for the base and DIRECTIONAL_ONLY for shards.
Rift black boxes and Crystal Glow inventory cards preserve LIGHTMAP_ONLY; their
source explicitly disables GL_LIGHTING. The misleading Chroma unlit factory is
removed after migrating all users. This supersedes the earlier unresolved comment
review in this document; it does not resolve unrelated excluded source contexts.

ScreenParticleRendering.submitGeometry exposes the existing native forward
screen-color phase to CustomFeatureRenderer submits. Focus world glows require
that RGB screen equation on composed scene color; they retain opaque-depth
testing and no depth writes. Solid item/model draws stay in their native pass.
All eight cutout profiles, five focus tiers, base/shard models, source normals
and complete native focus world draws in both transparency modes passed in
build/d06-focus-final-client.log (5m), along with existing matrix/water/bevel/
reservoir/full-client acceptance. Native mesh tests caught and now cover missing
overlay attributes. Full contract: ChromatiCraft/docs/focus-crystal-lighting.md;
pinned evidence: build/d06-focus-proof.json. D06 and the backlog counts stay open.


## Rift/Aurora: lightmap-aware RGB screen profiles

Original Rift source disables GL_LIGHTING but retains the lightmap around its
rune and halo. EntityLighting.screen now expresses the RGB screen equation with
all four independent lighting profiles and both culling choices. Rift chooses
LIGHTMAP_ONLY and supplies complete ENTITY attributes. World glows use the owned
native forward phase; item/detached draws keep native item collector ownership.
Aurora's original draw disables both lighting systems, so its unlit sprite shader
remains appropriate; its native world submission now uses forward screen blending
and captures its UV animation frame before deferred replay.

Eight screen profile pipelines, source Rift mesh/branch cases, Aurora clock/state
isolation and both actual world renderers passed in classic and improved
transparency, together with the full client suite (5m01s). Details:
ChromatiCraft/docs/rift-aurora-rendering.md; pinned proof:
build/d06-rift-aurora-proof.json. Existing focus/bevel/reservoir and all twelve
water screenshot checks passed again. Compiled global-lighting helper inventory
remains empty. D06 remains open; no checklist count change.


### 2026-10-20: Personal Charger screen depth and preview context

EntityLighting.screen(texture,cull,depthWrite) adds explicit source depth intent;
the existing overload remains no-write. All 16 screen lighting/cull/depth profiles
register and GPU-compile. Personal Charger retains lightmap/depth writes in item
draws and bypasses both lighting systems/no writes in world draws. StructureRenderer
now distinguishes synthetic/live-data preview context and restores prior flags
and rotations across nested stand-in extraction, including a throwing hook.
Native world/mesh/item/preview checks passed in 4m02s; DragonAPI 260 unit cases
reran clean. Contract: ChromatiCraft/docs/personal-charger-rendering.md; proof:
build/d06-personal-proof.json. D06 stays open.
