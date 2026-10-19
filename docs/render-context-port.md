# D06: explicit rendering context and lighting review

Status: in progress. Matrix getters have been replaced with explicit owned
snapshots. Lighting migration is still open; the legacy lighting helpers remain
no-ops and must not be counted as ported. Backlog remains 57 complete / 107 open /
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

The uncommented full-source inventory has 97 qualified helper calls across 63
files: disableLighting 17, enableLighting 4, disableEntityLighting 55,
enableEntityLighting 9, prepareGeoDraw 7, exitGeoDraw 5. Comments and string tokens
were distinguished from executable source. Excluded legacy call sites remain
part of migration planning; they were not deleted, commented out or added to the
build without their dependencies. The inventory is build/d06-full-source-callers.json.

Compiled bytecode method references identify only RenderBevel and RenderReservoir
as external users of the legacy helpers. This is narrower than file/class-name
matching: the preserved VoidMonster/Render/VoidGrowthRenderer.java has the same
class identity as a modern replacement and must not be mistaken for its compiled
implementation. Bytecode inventory: build/d06-active-render-helper-callers.json.

RenderBevel's calls surround a private renderFaceNumbers method which submit does
not call. Its native text submission already supplies full-bright light. The
native submit still has outstanding face-number/compass behavior; removing two
no-op calls alone would not restore those features.

RenderReservoir's helper calls are in renderLiquid, reached through an obsolete
public render signature, not native submit. Native submit already owns a separate
fluid surface, but uses the tile's packed light and entity-translucent pipeline
without the old emissive-fluid lighting decision. That active path needs an
explicit lighting profile and source-parity review. The class also retains cover
and legacy-fluid gaps; these are not made complete by the matrix repair.

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
