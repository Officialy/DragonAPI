# D56 — runtime mixin and distribution matrix, 2026-10-14

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta / Java 25. Runtime Mixin is
0.8.7 (Fabric sponge-mixin 0.17.4). Native signatures are verified by actual target
transformation against the cached 26.3 artifacts; the older Sources/ tree is not
used as the current API reference.

## Concrete repairs

DragonAPI's config now declares JAVA_25, matching its compiled Java 25 mixins.
MixinPopulationServer was already registered; an audit-development duplicate was
caught and removed. This batch does not claim a newly registered population hook.

The first real packaged dedicated-server boot failed in ChromatiCraft construction
because common registration referenced ColoredAltarEffects, which resolves client
Particle classes. Common payload/codec registration and server sending now belong
to ColoredAltarNetwork. The existing client setup registers rendering-side handling
through native RegisterClientPayloadHandlersEvent on its main-thread default.
The chromaticraft:altar_visual identifier, registrar version 1, CompoundTag body,
dimension/position/target/color/action fields, 64-block recipient radius and all
shock/bolt/break/fragment behavior are preserved. No Java or authored data is
excluded to hide the failure. Real dedicated-server startup now passes.

## Audit guarantees

The opt-in TestInstance audit reads the actual packaged configs and compiled
@Mixin annotations, including target class literals and string targets. It forces
definition of every applicable target through the native transforming loader.
Original required-injection checks remain enabled. Exported transformed bytecode
must contain the mixin's MixinMerged method origin or its generated accessor
interface; merely compiling or finding the target name is insufficient.

Compiled mixin inventory must match config registration: missing classes,
compiled-but-unregistered mixins and duplicate entries fail. Client bindings are
explicitly OTHER_SIDE on dedicated servers. The two @Pseudo targets may be
OPTIONAL_ABSENT only while their actual owning mod is absent; a missing target
from an installed owner fails instead of silently passing API drift.

Four configs register 45 mixin classes and 46 target bindings (VoidRitualTerrainMixin
has two targets). JSON reports contain one row per binding, physical distribution,
scope, optional status and transformation result. Exports remain in each isolated
runtime's .mixin.out/class directory. Both optional targets are validated using the
unchanged installed Chunky 1.5.4 and DH 3.3.4 jars.

The real dedicated-server pair listens only on an ephemeral IPv4 loopback port,
uses explicit offline/whitelist settings and keeps ticking until the connected
packaged client writes strict acceptance. Test identities/directories are separate
from Play. Family archives are immutable private development copies; no release,
feed or artifact is published. SHA-256 provenance for eight family and four
installed optional jars is recorded in build/d56-artifact-proof.json.

Only these named audit runs enable transformation exports. Normal Play remains
OfficialyAwsome / bb5029b7-9381-4d99-aaa9-106da41aa659 / auditClient=false.

## Packaged matrix

| Installed optional mods | Dedicated common bindings | Client bindings | Result |
| --- | ---: | ---: | --- |
| Neither | 30 transformed / 2 absent | 44 transformed / 2 absent | Passed, both clean exits |
| Chunky only | 31 transformed / 1 absent | 45 transformed / 1 absent | Passed, both clean exits |
| DH only | 31 transformed / 1 absent | 45 transformed / 1 absent | Passed, both clean exits |
| Chunky + DH | 32 transformed | 46 transformed | Passed, both clean exits |
| Chunky + DH server; Iris + Sodium + DH + Chunky client | 32 transformed | 46 transformed | Passed, both clean exits |

Final reports: build/mixin-audit/20261014-d56-final-{none,chunky,dh,both,render}/.
Each successful pair requires MIXIN_AUDIT_PASS on both peers,
MIXIN_AUDIT_PAIR_PASS on the server, JSON acceptance and clean exits. Logs are
build/d56-final-<case>-server.log and build/d56-final-<case>-client.log.

## Registered bindings

| Mod | Mixin | Native target | Scope |
| --- | --- | --- | --- |
| chromaticraft | `BiomeWeatherMixin` | `net.minecraft.world.level.biome.Biome` | mixins |
| chromaticraft | `EnderForestThunderMixin` | `net.minecraft.server.level.ServerLevel` | mixins |
| chromaticraft | `ItemStackNameMixin` | `net.minecraft.world.item.ItemStack` | client |
| chromaticraft | `LargeBiomeInitializationMixin` | `terrablender.util.LevelUtils` | mixins |
| chromaticraft | `LargeBiomeSourceMixin` | `net.minecraft.world.level.biome.MultiNoiseBiomeSource` | mixins |
| chromaticraft | `LuminousCliffsTerrainMixin` | `net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator` | mixins |
| chromaticraft | `SoundEngineChromaMusicMixin` | `net.minecraft.client.sounds.SoundEngine` | client |
| chromaticraft | `SoundOptionsScreenMixin` | `net.minecraft.client.gui.screens.options.SoundOptionsScreen` | client |
| chromaticraft | `SpawnerActivationMixin` | `net.minecraft.world.level.BaseSpawner` | mixins |
| chromaticraft | `VoidRitualTerrainMixin` | `net.minecraft.client.renderer.chunk.ChunkSectionsToRender$DrawIndirect` | client |
| chromaticraft | `VoidRitualTerrainMixin` | `net.minecraft.client.renderer.chunk.ChunkSectionsToRender$DrawSeparate` | client |
| dragonapi | `CameraAccessor` | `net.minecraft.client.Camera` | client |
| dragonapi | `ChunkMapAccessor` | `net.minecraft.server.level.ChunkMap` | mixins |
| dragonapi | `CreeperAccessor` | `net.minecraft.world.entity.monster.Creeper` | mixins |
| dragonapi | `MixinBiome` | `net.minecraft.world.level.biome.Biome` | mixins |
| dragonapi | `MixinBiomeColors` | `net.minecraft.client.renderer.BiomeColors` | client |
| dragonapi | `MixinBlockStateBase` | `net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase` | mixins |
| dragonapi | `MixinBlockTransformer` | `net.minecraft.core.component.BlockTransformer` | mixins |
| dragonapi | `MixinChunkMap` | `net.minecraft.server.level.ChunkMap` | mixins |
| dragonapi | `MixinChunkyGenerationTask` | `org.popcraft.chunky.GenerationTask` | mixins |
| dragonapi | `MixinClientLevel` | `net.minecraft.client.multiplayer.ClientLevel` | client |
| dragonapi | `MixinCustomFeatureRenderer` | `net.minecraft.client.renderer.feature.CustomFeatureRenderer` | client |
| dragonapi | `MixinDistantHorizonsChunkyAccessor` | `com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor` | mixins |
| dragonapi | `MixinEnderMan` | `net.minecraft.world.entity.monster.Enderman` | mixins |
| dragonapi | `MixinEnvironmentAttributeSystem` | `net.minecraft.world.attribute.EnvironmentAttributeSystem` | mixins |
| dragonapi | `MixinFireBlock` | `net.minecraft.world.level.block.FireBlock` | mixins |
| dragonapi | `MixinFluidRenderer` | `net.minecraft.client.renderer.block.FluidRenderer` | client |
| dragonapi | `MixinGameTestRunner` | `net.minecraft.gametest.framework.GameTestRunner` | mixins |
| dragonapi | `MixinGameTestServer` | `net.minecraft.gametest.framework.GameTestServer` | mixins |
| dragonapi | `MixinGenerationTask` | `net.neoforged.neoforge.server.command.generation.GenerationTask` | mixins |
| dragonapi | `MixinGrassBlock` | `net.minecraft.world.level.block.GrassBlock` | mixins |
| dragonapi | `MixinHurtByTargetGoal` | `net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal` | mixins |
| dragonapi | `MixinItemInHandRenderer` | `net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer` | client |
| dragonapi | `MixinLavaFluid` | `net.minecraft.world.level.material.LavaFluid` | mixins |
| dragonapi | `MixinLevelChunk` | `net.minecraft.world.level.chunk.LevelChunk` | mixins |
| dragonapi | `MixinLivingEntityAggro` | `net.minecraft.world.entity.LivingEntity` | mixins |
| dragonapi | `MixinNeoForgeEventBus` | `net.neoforged.neoforge.common.NeoForge` | mixins |
| dragonapi | `MixinPlayerList` | `net.minecraft.server.players.PlayerList` | mixins |
| dragonapi | `MixinPopulationServer` | `net.minecraft.server.MinecraftServer` | mixins |
| dragonapi | `MixinZombifiedPiglin` | `net.minecraft.world.entity.monster.zombie.ZombifiedPiglin` | mixins |
| dragonapi | `MultipleTestTrackerAccessor` | `net.minecraft.gametest.framework.MultipleTestTracker` | mixins |
| dragonapi | `PictureInPictureRendererAccessor` | `net.minecraft.client.gui.render.pip.PictureInPictureRenderer` | client |
| rotarycraft | `BulkCraftingMenuMixin` | `net.minecraft.world.inventory.CraftingMenu` | mixins |
| voidmonster | `GhostHealingPotionMixin` | `net.minecraft.world.effect.HealOrHarmMobEffect` | mixins |
| voidmonster | `VoidCameraMixin` | `net.minecraft.client.Camera` | client |
| voidmonster | `VoidGuiDistortionMixin` | `net.minecraft.client.renderer.GameRenderer` | client |

## Other validation and remaining work

The base source-folder client audit passes all 44 applicable bindings plus the
complete existing gameplay/render checks and actual chunk-tracker shutdown cleanup;
clean exit in 4m 22s. The 100 native DragonAPI GameTests pass, with all 30 applicable
common bindings verified, in 58s. The complete 196-unit family passes after the
altar transport fix. Eight guards verify 3,305 accepted primary outputs.

The installed renderer-stack packaged case passes all 46 client bindings. The
isolated final integrated DH/Chunky run also passes all 46 client and 32 integrated
server bindings, DH_CHUNKY_LIFECYCLE_PASS, DH_CHUNKY_INTEGRATED_CLIENT_PASS and the
complete existing graphical/gameplay audit. It exits cleanly in 3m 47s. Logs:
build/d56-client-optional-final.log; reports:
build/mixin-audit/20261014-d56-integrated-final/. A separate earlier integrated attempt timed out on Chunky's native
warning screen; the audit now uses only the real warning-only Continue Launch
button, and rejects fatal loading screens. A later native window-startup attempt
exited before acceptance and is not counted as a passing run. Old whitelist and
flat-world fixture setup failures are retained as failed evidence. The final fixture
sets connection settings explicitly and uses the normal world preset. Profiling/UI
dev tools are omitted from the dedicated audit to avoid unrelated shutdown races.

This matrix validates transformation, required signatures, distribution loading
and optional dependency combinations. It does not establish every injection's
complete gameplay semantics, public authenticated hosting, or visual correctness
of shader/LOD effects. CV12 and VM01 visual acceptance remain separate. D50's
updater remains inactive per the owner's release-permission instruction.

## Rerun

Prepare a new immutable jar set:

```powershell
.\gradlew.bat :TestInstance:prepareHashAuditJars -PhashAuditRunId=my-new-jars -PexcludeExternalMods --console=plain
```

Use one fresh mixinAuditId for each pair and the same jar ID on both peers. In two
terminals, selecting none, chunky, dh or both as appropriate:

```powershell
.\gradlew.bat :TestInstance:runMixinServerAudit -PhashAuditRunId=my-new-jars -PmixinAuditId=my-new-case -PmixinAuditOptional=both -PmixinAuditRemoteClient -PexcludeExternalMods --console=plain
# Wait for MIXIN_AUDIT_SERVER_READY.
.\gradlew.bat :TestInstance:runMixinClientAudit -PhashAuditRunId=my-new-jars -PmixinAuditId=my-new-case -PmixinAuditOptional=both -PexcludeExternalMods --console=plain
```

For the installed renderer stack, use both on the server and render on the client.
For a standalone dedicated transformation audit, omit mixinAuditRemoteClient.
For the full integrated optional listener lifecycle and graphical audit:

```powershell
.\gradlew.bat :TestInstance:runClientAudit -PmixinAudit -PmixinAuditId=my-new-integrated -PmixinAuditOptional=both -PdhChunkyCompatibilityTest -PexcludeExternalMods --console=plain
```

The paired run tasks reject existing acceptance reports; choose fresh IDs.
D56 is complete and checkpointed locally. No push or release publication occurred.


## D11 inventory extension (2026-10-18)

Boss fog, lightmap and renderer-tick reset add three client mixins: the current
inventory is 48 classes / 49 target bindings. Fresh native D11 audit verifies
30 transformed, 17 other-side and two optional-absent rows. Fresh integrated
client D11 audit verifies 47 transformed and two optional-absent rows, including
all three new hooks. The integrated server verifies the same native counts.
Reports: build/mixin-audit/20261018-d11-native/ and
build/mixin-audit/20261018-d11-client/. The separate boss-color fixture also
verifies native fog dispatch and real GPU lightmap output; see boss-color-port.md.
This extension does not rerun or replace the earlier packaged optional-mod matrix.
