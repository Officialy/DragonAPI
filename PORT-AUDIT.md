# DragonAPI Port Audit — 26.3 quality & standards review

_Complete. Source tree: `DragonAPI/src/main/java/reika/dragonapi` (654 `.java` files, all scanned)._
_Scope: (1) porting errors (stale 1.7.10 / removed 26.2 APIs, stubs), (2) bad modern-modding practice, (3) standards gaps._

**Implementation status (2026-10-08):** This is the historical findings ledger. Confirmed priority
repairs, corrections to disputed findings, regression results and remaining port boundaries are
recorded in [PORT-AUDIT-REVIEW.md](PORT-AUDIT-REVIEW.md), including the extended implementation section.
An original finding below is not evidence that its old implementation remains present.

## Legend
- **CRITICAL** — dead feature / silently wrong behaviour / hard-rule violation (unsanctioned stub)
- **HIGH** — crash-class, behaviour regression, thread-safety, or side-unsafe code
- **MEDIUM** — degraded design, reflection in hot paths, unbounded caches, partial data loss
- **LOW** — hygiene, raw types, stale comments, modernization opportunities

Sanctioned markers (`CHROMA-PORT:`, `DRAGONAPI-PORT:`, `BLOCK-PORT:`) are **intentional** per project rules and are not flagged as stubs. Gutted code **without** such markers violates the hard rules and is flagged CRITICAL.

---

## 0. Build & resource configuration

### CRITICAL — `accesstransformer.cfg` still uses SRG field names
`DragonAPI/src/main/resources/META-INF/accesstransformer.cfg` (whole file):
```cfg
public-f net.minecraft.client.Minecraft f_91052_
public-f net.minecraft.client.model.geom.ModelPart f_104212_ # cubes
```
These are 1.7.10/Forge-era **SRG** names (`f_91052_` = `minecraft`, `f_104212_` = `cubes`). Modern NeoForge (MojMap runtime) resolves AT entries by Mojang names; SRG entries fail to resolve, so `ReikaRenderDispatcher`'s access to `Minecraft.minecraft` and `ModelPart.cubes` will crash with `IllegalAccessException` (or NoSuchField on AT validation) at use time.
**Fix:** `public-f net.minecraft.client.Minecraft minecraft` and `public-f net.minecraft.client.model.geom.ModelPart cubes` (then verify against the 26.3 AT remap log).

### MEDIUM — stale duplicate mixin config `dragonapi.mixin.json`
`DragonAPI/src/main/resources/dragonapi.mixin.json` points at package `reika.dragonapi.mixins` (plural) — that package does not exist (the real one is `reika.dragonapi.mixin`, referenced by `mixins.dragonapi.json`). It is empty, `required: true`, and carries a dangling `refmap` name. Dead/contradictory config; if any loader path ever picks it up it registers nothing while claiming to be required.
**Fix:** delete `dragonapi.mixin.json` after confirming nothing references it (mods toml only wires `accesstransformer.cfg`; check the jar manifest for `MixinConfigs` attributes).

### MEDIUM — mixin target drift risk, all 26 mixin files (`mixin\`)
JSON wiring itself is correct (`mixins.dragonapi.json`, `defaultRequire: 1`, proper common/client split). But with `defaultRequire: 1`, any single renamed 26.3 target **crashes at apply time** instead of silently no-op'ing — one-time verification of each target against the 26.3 source jar is warranted. Highest-risk targets: `MixinGameTestServer` (shadows `testBatches`/`testTracker` + 4 methods), `MixinPlayerList` (remove/save redirect), `MixinChunkMap.updateChunkTracking`, `MixinGrassBlock` soft-override of interface methods, `MixinBlockTransformer` overload descriptor (already documented as overload-sensitive), client render mixins (`MixinItemInHandRenderer`, `MixinFluidRenderer`, `MixinCustomFeatureRenderer`).
**Fix:** one-time pass compiling each target name/descriptor against the Gradle 26.3 Minecraft source jar.

---

## 1. Mixin / ASM / auxiliary / libraries / modinteract / client / command cluster

_197 files in scope; ~125 read directly, remainder pattern-swept. The active cluster (mixins, networking, ChunkManager, Lua core, AE2/CC integrations, shader system, gametest infra) is well ported; problems are concentrated in silently gutted public API methods with no sanctioned marker._

### CRITICAL

**1.1 `libraries\ReikaPlayerAPI.java:52-54` — STUB: `kickPlayer` gutted**
```java
public static void kickPlayer(ServerPlayer ep, String reason) {
    //todo ep.playerNetServerHandler.kickPlayerFromServer(reason);
}
```
Empty body, no marker. Caller `ModFileVersionChecker.kick()` (`auxiliary\trackers\ModFileVersionChecker.java:68`) logs "kicked" while the player stays connected — the server jar-hash anti-tamper feature is dead.
**Fix:** `ep.connection.disconnect(Component.literal(reason))`, or `DRAGONAPI-PORT` + make the checker fail loudly.

**1.2 `extras\ThrottleableEffectRenderer.java:92-97,130-141,143-199,219-229,239-246` — STUB: particle engine husk**
`doRenderParticles` is only commented-out 1.7.10 tessellator code; `addEffect`'s limiter commented out; `bindTexture` empty; `isParticleVisible` returns `true // todo`; `getBoundingBox` returns static zero AABB; `getParticleCount` never counts. Throttling + rendering both absent.
**Fix:** port to the 26.3 `ParticleEngine` submit pipeline (patterns already proven in `ReikaRenderHelper`/`MixinCustomFeatureRenderer`), or `@Deprecated` behind `DRAGONAPI-PORT`.

**1.3 `command\BiomeMapCommand.java:46,62-78,91-105,164` — STUB: hardcoded debug args**
`String[] args = {"seed=-8335656470636700638", "1000", "1", "1", "false"};` — `/biomepng` always maps one fixed debug seed regardless of input; seed-range parsing and result packet send also commented out (`//todo FIX COMMAND ARGUMENTS`).
**Fix:** real brigadier arguments (`seed`, `range`, `resolution`, `grid`, `fullgrid`); delete the debug array.

**1.4 `command\EntityListCommand.java:82-97` — STUB: `getData` empty**
Body is the commented-out 1.7.10 `EntityList.classToStringMapping` loop. `/entitylist` prints "Found entities:" + nothing.
**Fix:** rebuild on `BuiltInRegistries.ENTITY_TYPE` + `EntityType.getKey`.

**1.5 Whole-file commented-out corpses — 4 files (hard-rule violation)**
- `modinteract\recipehandlers\SmelteryRecipeHandler.java` (1-309)
- `modinteract\recipehandlers\ForestryRecipeHelper.java` (1-223)
- `modinteract\recipehandlers\ThermalRecipeHelper.java` (1-184)
- `modinteract\power\ReikaBuildCraftHelper.java` (1-79)

Every public method lost; comments contain ported 26.x imports, showing a half-attempt then abandonment. No sanctioned marker (deferral for mods absent on 26.3 is legitimate **only if documented** — today it is undocumented commented-out code).
**Fix:** port to modern equivalents or delete files with an explicit ledger note in `PORTING.md`.

**1.6 `extras\ModVersion.java:141-162` — STUB: `readFromFile` returns hardcoded `v0a`**
Body fully commented out, ends `return getFromString("0a");` — breaks `VersionTransitionTracker` and update-checker comparisons.
**Fix:** restore classpath resource loading, or source the version from `IModInfo`.

**1.7 `auxiliary\FileInputThread.java:19-22` — STUB: `run() {}`**
Async file loader does nothing; `getFile()` always `null`. Whole class dead, no marker.
**Fix:** port the loader (image/text/XML/sound via `ReikaFileReader`) or delete class + call sites.

**1.8 `auxiliary\BlockArrayComputer.java:16-30` — STUB: every switch branch empty**
`switch(op) { case FILL: break; case ITERATE: break; case LOAD: break; ... }`.
**Fix:** implement against `BlockArray` or remove.

**1.9 `libraries\io\PacketPipeline.java:60-76` — STUB: `replyToPacket` no-op, `getMinecraftPacket` returns `null`**
Public API silently does nothing; no marker.
**Fix:** implement reply over the payload bridge (`PacketDistributor` to the originating `IPayloadContext` player); delete/`@Deprecated` `getMinecraftPacket`.

**1.10 `auxiliary\trackers\PlayerSpecificRenderer.java:130-205,308-333` — DEAD FEATURE**
`renderAdditionalObjects` is `private` and its only reference sits inside a commented-out `CustomPlayerRenderer` block; the 1.7.10 player-renderer replacement was never re-hooked to the 26.3 pipeline. `registerRenderer` accepts objects that can never render (`PlayerModelRenderer.extractRenderState` unreachable).
**Fix:** hook into the 26.3 player render state/feature pipeline, or `@Deprecated` behind a marker.

### HIGH

**1.11 `libraries\level\ReikaBlockHelper.java:179-183` — STUB: `isUnbreakable` ignores unbreakability**
Hardness check commented out (`// todo if block is unbreakable`); only `SemiUnbreakable` honored. Bedrock/portal-frame treated as breakable by `ProgressiveRecursiveBreaker` et al.
**Fix:** restore `state.getDestroySpeed(world, pos) < 0` + can-harvest variants.

**1.12 `auxiliary\ProgressiveRecursiveBreaker.java:414` — broken comparison**
```java
if (id.defaultBlockState().getProperties() == Blocks.WATER.defaultBlockState().getProperties())
```
Reference-equality on two distinct unmodifiable property maps — always false; water-spill sound branch dead.
**Fix:** `id.defaultBlockState().is(Blocks.WATER)` or `getFluidState().is(Fluids.WATER)`.

**1.13 `auxiliary\ProgressiveRecursiveBreaker.java:413-418` — side-unsafe sound (VERIFY 26.3)**
`world.playLocalSound(...)` on the server tick path; in the 26.2 reference `Level.playLocalSound` is a server-side empty stub, so break/liquid sounds never reach players. VERIFY 26.3.
**Fix:** use `ReikaSoundHelper.playSoundFromServer` (correctly implemented in `libraries\io\ReikaSoundHelper.java:90-119`).

**1.14 `libraries\io\ReikaSoundHelper.java:142-144,164-170` — side-unsafe client sounds**
`playNormalClientSound` / `playSoundAtEntity` call `world.playLocalSound` unconditionally though the class is deliberately server-loadable (comment 122-123); on server levels = silent no-op.
**Fix:** branch on `world.isClientSide()` and packet otherwise.

**1.15 `libraries\ReikaPlayerAPI.java:43-50` — STUB: `transferInventoryToChest`**
Computes a count, then `if (num >= inv.length) { }` — discards the inventory silently.
**Fix:** implement the transfer or remove the API.

**1.16 `modinteract\lua\LuaHasItem.java:28-43` — STUB: 1-arg/2-arg forms commented out**
Only the 3-arg form survives; anything else throws raw `IllegalArgumentException("Invalid ItemStack!")` into CC:Tweaked. Documented signature ("ID, metadata (optional)...") is now false.
**Fix:** restore ID and ID+count matching via `Item.byId`; raise `LuaException` instead.

**1.17 `auxiliary\trackers\PlayerChunkTracker.java:29,43-95` — thread-safety + entity-keyed cache**
Read path `shouldStopChunkloadingFor` does `containsKey` on a plain `HashMap` **outside** the `synchronized` write blocks; can corrupt under concurrent resize. Keyed by live `Player` entities, no logout cleanup; inconsistent lock order (`queued → tracked` at 89-92 vs `queued` alone at 52-54). Class doc claims "MultiThread-safe".
**Fix:** `ConcurrentHashMap` keyed by `UUID`, one consistent lock, logout cleanup.

**1.18 `extras\BlockProperties.java:21-70,84-104` — partial data loss / stale catalogue**
`setFlammable` no longer registers planks/logs/leaves/wool/wooden slabs (all commented with 1.7.10 names `Blocks.LOG`, `Blocks.LEAVES2`, …). Fire/temperature logic answers "not flammable" for the most common flammable blocks.
**Fix:** drive from tags (`BlockTags.LEAVES/LOGS/PLANKS/WOOL`, wool carpets, saplings) or `FireBlock` ignite odds.

**1.19 `libraries\level\ReikaWorldHelper.java:1128-1131` — stale `ListTag` cast (VERIFY 26.3)**
`for (Object o : li) modlist.add((String) o);` over a modern `ListTag` — post-1.21 elements are `Tag`s; this throws `ClassCastException` reading the `WorldID` file. Rest of repo already uses typed access (`ReikaNBTHelper.java:75-83`).
**Fix:** `for (Tag t : li) modlist.add(t.getAsString());`

**1.20 `auxiliary\trackers\KeyWatcher.java:95-96,204` — client class reachable from common code**
`Keys` enum constructor touches `Minecraft.getInstance().options` at static-init; `KeyTicker.tick` uses `Minecraft.getInstance().player`; enclosing class is in common `auxiliary.trackers` with no `Dist` gating — dedicated-server classload = crash.
**Fix:** move enum + `KeyTicker` into `reika.dragonapi.client` (as done for `ClientSounds`, `ClientLinkPrompt`, `ClientAPIPacketHandler`).

**1.21 `modinteract\power\ReikaRailCraftHelper.java:25-34,63-64,75-102` — stale integration, NPE risk**
Reflection targets 1.7.10 identities (`mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox`, `boiler`, `burnTime`); on failure `isFirebox` dereferences a null `Class` → NPE per query. `private static final Fluid STEAM = null;//todo` placeholder.
**Fix:** null-guard all accessors after failed static init; re-map to modern RailCraft or `DRAGONAPI-PORT`.

**1.22 `auxiliary\SettingInterferenceTracker.java:56-59,115-138` — STUB: warning icon dead**
`muteInterference.drawIcon` empty ("TODO: Port to 26.1 rendering API"); `onRender` draws nothing while login messaging still tells players to "see the icon".
**Fix:** port `drawIcon` to `GuiGraphics`, or drop the render path + wording.

**1.23 `auxiliary\trackers\VersionTransitionTracker.java:122` + `auxiliary\PopupWriter.java:37,98-106` — side-unsafe popup path (VERIFY)**
Server login path calls `PopupWriter.instance().addMessage(...)`, but `PopupWriter extends Screen` (client-only). `Dist.DEDICATED_SERVER` branch inside `addMessage` is unreachable; class-loading `Screen` on a dedicated server fails.
**Fix:** route through the existing POPUP packet (`PacketDistributor` to all players).

### MEDIUM

**1.24 `libraries\rendering\ReikaRenderHelper.java:176-198,305-308` — STUB: lighting API no-ops**
`disableLighting`/`enableLighting`/`disableEntityLighting`/`enableEntityLighting` fully commented out; `getMatrix` returns identity "to prevent crashes". Public no-ops consumed by e.g. `SettingInterferenceTracker.onRender`.
**Fix:** delete with `@Deprecated` shims, or implement via `RenderSystem`/render-pipeline state.

**1.25 `client\ClientAPIPacketHandler.java:98-101,121-127` — STUB: dead packet cases**
`GUIRELOAD` body commented out; `MODLOCK`/`OREDUMP` cases commented out entirely — server sends are silently dropped client-side (e.g. `ModLockController.readSync`).
**Fix:** port to 26.3 screen API or remove the IDs from `APIPacketHandler.PacketIDs`.

**1.26 `command\EventProfilerCommand.java:15-24`; `auxiliary\trackers\CommandableUpdateChecker.java:397-428` — STUB: commands ignore args**
`profileevent` hardcodes `{"display"}` (enable/disable unreachable); `/checker` registers a bare literal but requires `args.length == 2` — always "Invalid arguments."
**Fix:** real brigadier arguments.

**1.27 `modinteract\lua\LuaGetSlot.java:33-38`; `LuaGetPlacer.java:27` — CC contract violation**
Returns `Component` (`getDisplayName()`) and `UUID` inside the raw `Object[]` handed to CC:Tweaked, which needs primitives/strings → serialization errors; `LuaGetSlot` also returns `null` for empty slots, contradicting its doc ("Returns: 'Empty'").
**Fix:** `.getString()` the components, `toString()` the UUID, return the documented sentinel.

**1.28 `libraries\level\ReikaWorldHelper.java:1014-1021` — STUB with non-sanctioned marker**
`getBlockMetadata` returns `0`, `setBlockMetadataWithNotify` empty — annotated `MULTIBLOCK-PORT:`, which is **not** one of the sanctioned gates (CHROMA/DRAGONAPI/BLOCK-PORT). Setter silently does nothing for any consumer.
**Fix:** rename to `BLOCK-PORT` or implement via blockstate properties.

**1.29 `auxiliary\ProgressiveRecursiveBreaker.java:95-106` — STUB on a false premise**
"ModWoodList package doesn't exist - commenting out special tree depth handling" — but `modregistry\ModWoodList.java` **exists and compiles**. Special-tree depths (Sequoia 350 etc.) disabled needlessly.
**Fix:** restore the depth table; keep tag-based TODO as follow-up.

**1.30 `modinteract\power\ReikaPneumaticHelper.java:28-39` — STUB: reflection result discarded**
Static block reads `pneumaticCraft.common.Config.fluxCompressorEfficiency` but the assignment is commented out; `airPerRF` is `final` → always default.
**Fix:** non-final field, assign with fallback, modern PneumaticCraft package (VERIFY).

**1.31 `modinteract\power\ReikaRFHelper.java:51-60,86-95,136-175` — silent reflection + invasive fallback**
TE static block has an **empty** catch; `drainStorage` brute-forces "any int field containing 'energy'" on foreign BEs, swallowing `ReflectiveOperationException` — can corrupt unrelated fields with no log trail.
**Fix:** log failures once (`ReflectiveFailureTracker` pattern); restrict fallback to known energy types.

**1.32 `libraries\ReikaIngredientHelper.java:25-29,87-110` — unbounded cache + registry-wide scan**
`EXPANSION_CACHE: ConcurrentHashMap<Ingredient, List<ItemStack>>` — never evicted, never cleared on reload, keyed by non-interned `Ingredient`; fallback scans **every registered item** per expansion (up to 4096 stacks).
**Fix:** canonical fingerprint keys + clear on datapack sync / size bound.

**1.33 `libraries\io\NBTCompat.java:17-20,67-123` — reflection shim for an API that exists natively**
`callGetter` does `getClass().getMethod(...)+invoke` **per call** for `getInt/getLong/getDouble/getBoolean/getCompound`, `catch (Throwable)` everywhere. 26.3 has `getIntOr(key, def)` etc. (already used at `PlayerFirstTimeTracker.java:51`).
**Fix:** direct `getIntOr` calls; delete the reflection.

**1.34 `libraries\ReikaPacketHelper.java:1296-1315,1409-1515` — per-packet reflection**
`sendSyncPacket` resolves protected fields reflectively per send; SYNC decode re-resolves + `f.set` per receive; comment says all known SYNC callers are commented out.
**Fix:** delete `SYNC` (replaced by `BE_NBT_SYNC`), or cache resolved `Field`s.

**1.35 `auxiliary\trackers\WorldgenProfiler.java:21-22,109-111,405` — static `Level` reference**
`static Level currentProfilingLevel` retained across world unload/reload (never cleared in `finishProfiling`); `getLevel().registryAccess()` NPEs if profiling never ran on this side.
**Fix:** keep `ResourceKey<Level>`, resolve on demand, null-guard.

**1.36 `auxiliary\trackers\KeyWatcher.java:180-188` — player-keyed map never cleared on logout**
`KeyState.data: PlayerMap<Boolean>` accumulates across sessions.
**Fix:** clear on `PlayerLoggedOutEvent` / per-session structure.

**1.37 `libraries\ReikaPlayerAPI.java:40` — dead static BE cache**
`static final PlayerMap<SkullBlockEntity> headCache` unreferenced — leak-by-design if revived.
**Fix:** delete.

**1.38 `libraries\ReikaPlayerAPI.java:117-122` — synthetic event misuse (VERIFY)**
`playerCanBreakAt` posts `new BreakBlockEvent(...)` as a permission query; NeoForge's break events aren't protection-query events — may fire unrelated listeners.
**Fix:** use `BlockEvent.BreakEvent`-style guards or block-protection hooks.

**1.39 Command/popup user-facing strings — standards gap**
`command\GuideCommand.java:20-25`, `PopupWriter.java:97-104`, most commands: raw `String` messages, no localization; only `DonatorCommand`/`GetUUIDCommand` use `Component.translatable`.
**Fix:** lang keys + `Component` at API boundary.

**1.40 `auxiliary\DebugOverlay.java:42,54,74-78` — commented-out logic + stale compare**
Commented has-BE precheck (with a dead `getBlockMetadata` call inside the comment), `// TODO 1.21+: setShaderTexture`; `event.getName().equals(VanillaGuiLayers.TITLE)` — VERIFY type on 26.3.
**Fix:** real `level.getBlockEntity(...) != null` check; resolve TODO.

**1.41 `libraries\java\ReikaASMHelper.java:71-73,104-106,1242` — legacy ASM scaffolding**
Hardcoded `C:/Users/Reika/.gradle/.../1.7.10.../srgs/` path; `changeMethodReturnType` empty body (public API silently does nothing); `catch (Throwable)`.
**Fix:** strip SRG constants/empty methods, or quarantine the `asm.patchers` framework.

**1.42 `asm\patchers\Patcher.java:16-38,119-121` — ASM-ClassTransformer-era framework retained**
Obf/deobf name pairs, `cpw.mods.fml` probes, raw `new HashSet()`; no transformer registration exists on NeoForge 26.3. Exactly the "should be mixins now" category.
**Fix:** delete once its last consumer is retired, or `@Deprecated` + ledger note.

**1.43 `libraries\registry\ReikaDyeHelper.java:61-64` — STUB: dye item cache never built**
Logs "Building dye item cache..." then does nothing ("Stub for legacy startup wiring"), no marker.
**Fix:** implement via `DyeColor`/tags or remove.

**1.44 `auxiliary\trackers\ReikaXPFluidHelper.java:19-24` — reflection against pre-1.13 identities**
Targets `openblocks.OpenBlocks$Fluids`, old EnderIO, `immibis.lxp` — all permanently inert; failures logged at INFO only.
**Fix:** fluid **tag** based lookup or `ModList`-gated modern reflection with tracker logging.

### LOW

**1.45 Raw types, pervasive** — `asm\patchers\Patcher.java:23`, `modinteract\lua\library\IconLookupRegistry.java:23`, `auxiliary\trackers\PlayerSpecificRenderer.java:50-52`, `modinteract\lua\DonatorController.java:29-31`, `libraries\level\ReikaWorldHelper.java:1084`. Fix: parameterize.

**1.46 `auxiliary\trackers\TickRegistry.java:58` — unresolved render-tick TODO** — VERIFY `RenderFrameEvent.Pre#getPartialTick` on 26.3; `ReikaRenderHelper.ptick` depends on it.

**1.47 `auxiliary\trackers\SpecialDayTracker.java:27-44`** — commented-out 1.7.10 corpse (`world.provider.dimensionId`). Delete; behavior lives in `MixinEnvironmentAttributeSystem`.

**1.48 `auxiliary\trackers\EventProfiler.java:179`** — non-ASM listeners silently dropped from profiling output; stale `IEventListener` comment at line 11.

**1.49 `auxiliary\trackers\CommandableUpdateChecker.java:45,293-304`** — plain-HTTP endpoint; `BufferedReader`/`FileOutputStream` not try-with-resources. HTTPS + TWR.

**1.50 `trackers\PatreonController.java:107-121`** — `data.get(dev)` NPE when dev unregistered/typo'd (`getModPatrons` guards; these don't).

**1.51 `auxiliary\trackers\PlayerChunkTracker.java:40,79-83`** — stale "inserted via ASM" comment (now `MixinChunkMap`); `emp.xo = emp.zo = Double.MAX_VALUE` entity-field mutation as timeout marker — brittle (VERIFY intentional).

**1.52 `auxiliary\trackers\KeyWatcher.java:81-91`** — `if (!(cfg instanceof Property)) throw ...` assumes oldforge `Property` config; if config moved off `Property`, every `readFromConfig` throws (VERIFY wiring).

**1.53 `libraries\java\ReikaObfuscationHelper.java:21,30-40,160-200`** — `testDeobf()` returns literal `true` with 1.7.10 probe commented out; `isDeObfEnvironment()` misleading. Replace with `FMLEnvironment`-based check.

**1.54 `extras\shader\IrisCompat.java:38-73`** — `Method.invoke` per frame in render path (`MixinCustomFeatureRenderer.buildGroup` calls `query()` per geometry batch). Memoize per frame/stage.

**1.55 `auxiliary\trackers\VersionTransitionTracker.java:7,11,54-58`** — duplicate `ModList` import; unvalidated `parts[0], parts[1]` parse of `modversions.list` (AIOOBE on malformed line).

**1.56 `auxiliary\PopupWriter.java:47,51-53`** — `public static final ArrayList<Warning> list` mutable public; shared mutable render state (`buttonX/buttonY/buttonSize`). Encapsulate.

**1.57 `auxiliary\trackers\WorldgenProfiler.java:346-351`** — `if (a.addSpilledChunk(...)) ;` empty controlled statement, flag discarded.

**1.58 `libraries\level\ReikaWorldHelper.java:98-114`** — try/catch throws `RuntimeException("Could not find GameRegistry IWorldGenerator data!")` — stale 1.7.10 GameRegistry message over commented-out reflection.

**1.59 `libraries\ReikaPacketHelper.java` (various callers)** — packet IDs as `PacketIDs.<X>.ordinal()` on the wire; enum reordering breaks cross-version compat. Use explicit constants.

**1.60 `auxiliary\trackers\ItemMaterialController.java:26-96`** — `@Deprecated` yet still ~50 hardcoded vanilla items ("todo wood types"), commented meta loop. Tag-driven (`#c:ingots/iron`, `#planks`) if revived.

**1.61 `auxiliary\RemoteAssetLoader.java:23,417-421`** — `javax.swing.*` import + empty `DownloadDisplayWindow extends JOptionPane` corpse. Remove.

**Clean reference patterns (explicitly good):** `ChunkManager` (ResourceKey keying + `ServerStoppedEvent` cleanup), `auxiliary\jade\ThermalTileJadePlugin`, `CCCompat`, `MESystemReader`, `ReikaShaderSystem`, `GameTestRuntimeMonitor`, `SyncedRecipeLookup`, `LegacyMotionTags`, `LegacyOreVeins`, `AdvancementHelper`, `LuaMethod` scan-data registration, `AEHooks`/`AECompat`/`CCHooks`, `ModWoodList`.

---

## 2. Base / interfaces / exception / modregistry / io / network / root cluster

_177 files scanned (base 14; interfaces 114; exception 21; modregistry 4; io 6; network 2; root 6). Modern 26.3 names (`Identifier`, `EntityTypes`, `TagValueInput/Output`, `ValueInput/Output`, `preRemoveSideEffects`, `kill(ServerLevel)`, `hasChunksAt`, `neoforge.transfer.ResourceHandler`) were verified against `Sources/` and are **not** flagged. Sanctioned markers were not flagged._

### CRITICAL

**2.1 `base\CoreContainer.java:349-361` — STUB: `findSlot` always returns empty, with an impossible null check**
```java
OptionalInt s = super.findSlot(container, slot);
if (s == null) {           // OptionalInt can never be null → dead branch
    ...
}
return OptionalInt.empty(); // super's result discarded even when found
```
`OptionalInt` is a value type, so the `relaySlots` fallback is unreachable and every caller gets "slot not found".
**Fix:** `return s.isPresent() ? s : (relay match found ? OptionalInt.of(...) : OptionalInt.empty());`

**2.2 `DragonAPI.java:143-146` — STUB: `isSinglePlayer()` hardcoded `false`**
```java
public static boolean isSinglePlayer() {
    return false;//getSide() == Dist.DEDICATED_SERVER && !FMLEnvironment.getDist().isDedicatedServer();
}
```
All single-player-gated behavior across the four mods misfires on integrated servers.
**Fix:** `return FMLEnvironment.getDist().isClient() && ClientEnvironment.isLocalServer();` (mirrors `isSinglePlayerFromClient()` at line 139).

**2.3 `modregistry\ModOreList.java:409-415` — STUB: `getGennableIn` abandoned loop returning `null`**
Loop body is empty (`//TODO incomplete abandoned method`), returns `null` — callers that iterate the result NPE. Violates "PORT FULLY. NO STUBS."
**Fix:** filter `ores` by `canGenerateIn` and return matches (empty list, never null).

**2.4 `modregistry\ModOreList.java:426-443` — STUB: `getOreModFromItemStack` body fully commented out, returns `null`**
Ore→owning-mod attribution is dead (commented `GameRegistry.findUniqueIdentifierFor` remnant at 430).
**Fix:** resolve via `BuiltInRegistries.ITEM.getKey(is.getItem()).getNamespace()` mapped to `ModList`.

### HIGH

**2.5 `modregistry\ModWoodList.java:127-129,478-490` — `logMappings`/`leafMappings` never populated**
The static block fills only `saplingMappings` and `modMappings`; `getModWood(Block)` (344) and `getModWoodFromLeaf` (364) always return `null`, silently disabling every "is this modded wood" check.
**Fix:** add `logMappings.put(id, w); leafMappings.put(leaf, w);` to the static block.

**2.6 `modregistry\PowerTypes.java:18-39` — `RF()`/`FE()`/`HYDRAULIC()` bind to the varargs ctor, permanently "not loaded"**
No no-arg ctor exists, so `RF()` compiles to `this(new String[0])` → `exists = false`; RF/FE interop paths keyed on this enum are disabled forever.
**Fix:** add `PowerTypes()` ctor setting `exists = true`; consider merging RF/FE (same energy type on NeoForge).

**2.7 `io\DirectResourceManager.java:88-91` — `getNamespaces()` returns `null`**
Contractual `Set<String>`; any consumer enumerating this manager NPEs (it is handed to `SoundLoader` consumers at `instantiable\io\SoundLoader.java:78`).
**Fix:** `return Set.of(DragonAPI.MODID);`

**2.8 `io\DirectResourceManager.java:43-51` — `getResource` always reports the resource present**
`Optional.of(ret)` returned for every identifier and every namespace; "not found" is unrepresentable, so consumers fail later with opaque errors. Also `dynamicAssets` is keyed by path only, ignoring namespace.
**Fix:** return `Optional.empty()` for unknown assets; key by full `Identifier`.

**2.9 `base\BlockEntityRenderBase.java:49-53` — STUB: `submit(...)` empty body**
The sole render entry point of the 26.3 `BlockEntityRenderer` interface is a non-abstract no-op; subclasses implementing only `doRenderModel` (the documented contract, line 77) render nothing, silently.
**Fix:** make `submit` abstract, or invoke `doRenderModel` after state extraction.

**2.10 `base\BlockEntityBase.java:299-301` — STUB: `syncTankData()` computes and discards**
Reflection result collected into a local, never used; method never called. Dead.
**Fix:** restore tank sync or delete.

**2.11 `network\DragonPayloads.java:37-225` — dead subsystem: payloads never registered, handlers stubs**
`PlayerDataSyncPayload`, `BlockEntitySyncPayload`, `ConfigSyncPayload`, `PopupMessagePayload` handlers only log; zero repo references to `DragonPayloads.` — never registered with a `PayloadRegistrar`. Looks like a working network layer, isn't.
**Fix:** register + port handlers, or remove until wired.

**2.12 `Tests.java:28` — wrong modid: `@EventBusSubscriber(modid = "DragonAPI")` vs `MODID = "dragonapi"`**
Annotation can never bind — silent no-op subscriptions.
**Fix:** use `DragonAPI.MODID`.

**2.13 `io\CompoundSyncPacket.java:36-45,63-71,161-200` — thread-safety: singleton packet mutates plain `HashMap`s across tick + network threads**
`dispatch` flag not volatile, no synchronization, one shared mutable instance sent to all players (line 328) — the CME it logs at 65-69 is still possible.
**Fix:** synchronize, make flags volatile, build immutable snapshots per dispatch.

**2.14 `base\InertEntity.java:25-29` — STUB: all inert entities serialize as `EntityTypes.ARROW`**
Wrong save/load round-trip, sync type and client renderer ("TODO Add a new entity type").
**Fix:** register a real inert type; make the ctor protected/abstract-requiring.

**2.15 `base\InertEntity.java:110-112` — `getSwimSplashSound` builds a SoundEvent from `Identifier.parse("")`**
`Identifier.parse("")` throws; any splash on an inert entity crashes.
**Fix:** `SoundEvents.GENERIC_SPLASH` or suppress.

### MEDIUM

**2.16 `base\BlockMultiBlock.java:103-109` — dead override pattern**
Plain `hasTileEntity`/`createTileEntity` override nothing on modern NeoForge (class extends `Block`, not `EntityBlock`); never called.
**Fix:** implement `EntityBlock` or delete both.

**2.17 `base\BlockReplaceOnBreak.java:17-20` — re-places the same state**
`return world.setBlock(pos, state, 3);` — the 1.7.10 class replaced the broken block with a *different* block; same-state replacement makes it effectively unbreakable. VERIFY lost target-block parameter.

**2.18 `base\BlockTieredResource.java:45-53` — `getDrops` empty; `canHarvestBlock` always false at API-base level**
If intentional, document; otherwise wire `canHarvestBlock` to `isPlayerSufficientTier` (line 61).

**2.19 `base\DragonAPIMod.java:106-117` — `basicSetup` gutted: file-hash + update-checker registration lost**
`getFileHash()` (98) always `null`; `requireSameFilesOnClientAndServer` feature dead.
**Fix:** port hash computation to NeoForge mod-file paths (`IModInfo`/`ModFile`).

**2.20 `base\DragonAPIMod.java:76-79` — `.get()` on Optional with candid comment**
`return ModList.get().getModContainerById(this.getModId()).get(); //todo this is probably fucked lmao`
**Fix:** `.orElseThrow(RegistrationException::new)`; verify all four mods' `getModId()`.

**2.21 `modregistry\PowerTypes.java:22` — `PNEUMATIC` points at 1.7.10 PneumaticCraft class**
`pneumaticCraft.api.blockentity.IPneumaticMachine` (modern: `me.desht.pneumaticcraft.api...`, VERIFY) — `checkAllClasses` always fails; type permanently disabled.

**2.22 `APIPacketHandler.java:369-376` — commented `PLAYERATTRSYNC` case with raw SRG-era APIs**
`func_150296_c`, `NBTBase.getTag`, `BaseAttributeMap` — attribute sync absent entirely on the port.
**Fix:** port to modern `AttributeMap`/`AttributeInstance` or delete.

**2.23 `APIPacketHandler.java:442,479` — dimension parameter silently dropped**
`world.dimension()/*todo old dimension id's data[1]*/` — client-declared dimension ignored in both biome-map and entity-verify flows. Validate against `world.dimension()` or remove from wire format.

**2.24 `io\DirectResourceManager.java:64-86` — STUB: `initToSoundRegistry` fully commented out**
Dynamic sound accessors never registered; `onResourceManagerReload` no longer injects the manager. VERIFY injection elsewhere; on NeoForge use `AddPackFindersEvent`/custom `PackResources`.

**2.25 `io\CompoundSyncPacket.java:269-278,305-347` — legacy packet + dead tracker**
`type()` constructs a fresh `PacketType` per call (protocol constants should be stable, VERIFY); `CompoundSyncPacketTracker` never registered (diff-sync in `BlockEntityBase` commented at 534-536). Migrate to `CustomPacketPayload`; delete dead tracker.

**2.26 `base\CoreContainer.java:132-135,292-295` — reach/interaction check lost**
1.7.10 `canInteractWith` (`alwaysCan || isStandard8mReach`) replaced with `return !tile.isRemoved();` — menus usable at any distance; `setAlwaysInteractable()` is now an unused knob.
**Fix:** `return alwaysCan || (!tile.isRemoved() && this.isStandard8mReach(player));`

**2.27 `base\CoreContainer.java:31,287` — static mutable `ChestBlockEntity fakeChest`**
Shared mutable BlockEntity handed to fallback `Slot`s. Make it a per-menu field or a dedicated empty `Container`.

**2.28 `Tests.java:31-38,51` — debug item in production + empty `runTests` + unguarded list mutation**
`TEST_ITEM` registered unconditionally; `runTests()` empty (called from `DragonAPI.java:183`); `PopupWriter.list.remove(0)` throws on empty list.
**Fix:** gate behind `!isProduction()`; implement/remove tests; use a checked API.

**2.29 `ModList.java:152-164` — dangling-else dead branch**
Commented-out else statement makes the following `if (condition) { ... "Attempting to load data" ... }` the else-body — the load-data block can never execute.
**Fix:** brace the else; remove the redundant branch.

**2.30 `interfaces\blockentity\WorldRift.java:27` — metadata-era contract**
`int getBlockMetadataFrom(Direction dir);` — implementers must fake an int from a `BlockState`. Add `getBlockStateFrom(Direction)` and migrate.

**2.31 `modregistry\ModWoodList.java:418-424,338-341,446-449,189-195`**
- `getRandomWood` `while (!wood.exists) ...` spins forever when no wood is loaded. Add attempt cap.
- `getPlankID` returns `null //todo fix null`. Resolve via plank tag.
- `canBePlacedSideways` hardcoded false (real check commented at 442-444). Use `hasProperty(AXIS)`.
- Registry miss check `wood_b == null` — vanilla `getValue` returns `Blocks.AIR`, not null (VERIFY 26.3). Use `isAir()`.

**2.32 `exception\ASMException.java:10` — `DEV_ENV = true` hardcoded with `!FMLForgePlugin.RUNTIME_DEOBF` remnant**
SRG diagnostics branch permanently dead. Derive from `!FMLEnvironment.isProduction()`.

**2.33 `base\StructureBase.java:18-26` — client level used without side guard**
`getArray(ClientEnvironment.level(), ...)` on a dedicated-server call passes null/unexpected level. Add client-side guard.

**2.34 `base\BlockEntityBase.java:355-363` — broad exception swallowing in the tick loop**
NPE/CCE/ArithmeticException caught and written as errors — corruption keeps ticking; NPE/CCE in `updateEntity` are almost always port bugs. Narrow or rethrow in dev.

**2.35 `APIPacketHandler.java:427-428` — magic `"dropper"` NBT key**
No constant/doc; `ITEMDROPPERREQUEST` trusts only `distanceToSqr <= 128*128` (no `canRequestSync` like TILESYNC at 331). Constant + permission check.

**2.36 Hardcoded data → datagen/config**
`base\BlockCustomLeaf.java:44-61` (flammability 30/spread 60 for all custom leaves); `modregistry\ModOreList.java:99-102` (truncated 5-hex-digit color literals `0x00977`, `0x0a408` — 1.7.10 typo producing wrong colors); `io\ReikaFileReader.java:128-135` (hardcoded internet-probe URLs → config).

**2.37 `io\ReikaFileReader.java:401-408,439-441` — resource leaks**
`new JarFile(f)` never closed; `copyFile` leaks the `FileInputStream` if `FileOutputStream` throws. Try-with-resources.

**2.38 Residual commented-out bodies (behaviour hidden behind comments — port or delete with note)**
`base\CoreContainer.java:101-122` (ICrafting sync); `base\BlockTEBase.java:42-48,82-90` (hardness/comparator); `DragonAPI.java:94-98,108-111,201-204` (overlays/commands "todo update overlays"); `base\BlockEntityBase.java:403-405` (OC nodes); `io\ReikaMIDIReader.java:84-179` (three full methods); `modregistry\ModOreList.java:453-462` (`getByDrop`).

**2.39 `APIPacketHandler.java:554-559` — ordinal wire format**
`getEnum(int)` maps raw ints to `PacketIDs` ordinals; insertion/reorder corrupts the protocol. Stable explicit ids.

### LOW

- **2.40 Shared static `Random`** — `DragonAPI.java:54`, `BlockEntityBase.java:71`, `BlockTieredResource.java:35`; `ModOreList.java:342` `new Random()` per call. Prefer `RandomSource` per-level.
- **2.41 Raw types** — `ModList.java:137`, `CompoundSyncPacket.java:38-40,114,210,236,259`, `ReikaMIDIReader.java:346,351`, `ModWoodList.java:144,217-223`, `ReikaFileReader.java:752`. Parameterize.
- **2.42 `DragonOptions.java:227-425`** — huge commented-out `ForgeConfigSpec` port attempt; active path is custom `ControlledConfig`. Plan the `ModConfigSpec` migration. Also `DEBUGKEY` label says "LWJGL ID" for a GLFW constant (line 46).
- **2.43 Unused client imports in common classes** — `APIPacketHandler.java:5-6` (`Minecraft`, `ClientLevel`), `BlockEntityRenderBase.java:9,12`, `BlockCustomLeaf.java:7,14,16`. Remove (accidental client classloading risk).
- **2.44 Pointless overrides/dead computation** — `BlockTieredResource.java:75-78` (`onPlace` only calls super), `InertEntity.java:70-87` (fire-tick pass-throughs), `BlockEntityRenderBase.java:62-74` (brightness loop result discarded), `InertEntity.java:64` (`//this.move(...)` — base `tick()` never applies motion).
- **2.45 Empty branches** — `APIPacketHandler.java:245-246` (`default -> {}` undocumented), `CoreContainer.java:198-200,233-235`, `ParticleEntity.java:136-138`. Document or clean.
- **2.46 Nullable annotation inconsistency** — `interfaces\blockentity\MEGridHost.java:3`, `PeripheralRelay.java:3` use `javax.annotation.Nullable`; newer files use jspecify. Standardize on jspecify (VERIFY javax resolves via transitive dep).
- **2.47 Holder-era gaps** — `interfaces\registry\EnchantmentEnum.java:16` raw `Enchantment` (→ `Holder<Enchantment>`); `interfaces\PermaPotion.java:13,17` raw `Potion` (VERIFY 26.3); `interfaces\registry\EntityEnum.java:17` returns an `Entity` *instance* from a registry enum (stale 1.7.10 semantics; should be `EntityType<?>`).
- **2.48 Damage-int remnants** — `ModOreList.getEntryFromDamage(int)` (282-286); `ModWoodList.getLogItemWithOffset(int)` ignores the offset (298-300); `getAllLeaves` meta loop commented (404-406). Migrate to `BlockKey`/state identity.
- **2.49 `ModWoodList.isNaturalLeaf` (454)** — `getMapColor(...).equals(MapColor.PLANT)` fragile; prefer `state.is(BlockTags.LEAVES)`.
- **2.50 Config leftovers** — `interfaces\configuration\BoundedConfig.java:12` unused `ModConfigSpec` import; `BoundedConfig.java:13`/`BooleanConfig.java:12` still import oldforge `Configuration/Property` shims.
- **2.51 `io\ReikaCSVReader.java:21-23`** — private ctor `throw new RuntimeException("cannot be instantiated!")` in a class with a public ctor; foot-gun, delete.
- **2.52 INFO logging on hot paths** — `CompoundSyncPacket.java:197,266`, `ModOreList.initialize` per-entry, `APIPacketHandler.java:95,104,118`. Downgrade to DEBUG.
- **2.53 `io\MusicLoader.java:10-12,32`** — dead code with init-order hazard (`MusicFolder` ctor captures `DragonAPI.instance` before mod ctor may run). Remove or wire explicitly.
- **2.54 `CompoundSyncPacket.java:289`** — `world.getBlockState(...) != null` — never null; dead check.
- **By design, not stubs:** `FractionalButtonGui`, `GuiController`, `OneSlotMachine`, `PartialTank`/`PartialInventory`, `InertIInv`, `TameHostile`, marker interfaces — intentionally empty.

**Standards-gap summary (cluster 2):** `Holder<T>` adoption (EnchantmentEnum, PermaPotion, ModOreList ItemStack identity); tags over reflection-based block lookups (`ModWoodList.loadBlock` should be registry/tag lookups with `ResourceLocation` constants); datagen for leaf flammability + ore colors; `StreamCodec`/`Codec` for the hand-rolled NBT diffing in `CompoundSyncPacket`/`ObjectToNBTSerializer`; `Component` at chat/popup boundaries.

---

## 3. Instantiable cluster

_~262 files scanned (≈50 line-by-line, rest pattern-swept) across data, event, io, math, formula, rendering, particlecontroller, effects, gui, storage, recipe. Root-level `instantiable/*.java` files (ItemReq, TemporaryInventory, etc.) incidentally touched._

### CRITICAL

**3.1 `data\immutable\DecimalPosition.java:251-253` — infinite recursion, guaranteed StackOverflowError**
```java
public boolean setBlock(Level world, Block b) {
    return this.setBlock(world, b);   // calls itself
}
```
**Fix:** `return world.setBlock(this.getCoordinate(), b.defaultBlockState(), 3);`

**3.2 `data\blockstruct\BlockArray.java:1022-1031` — STUB: `setTo(Block)` gutted no-op; `clearArea()` clears nothing**
Body only *reads* the current block and discards it. `clearArea()` → `setTo(Blocks.AIR)` is a silent no-op — hard-rule violation.
**Fix:** `refWorld.setBlock(c, b.defaultBlockState(), 3)` per position.

**3.3 `data\blockstruct\MultiBlockBlueprint.java:27,76-84` — int legacy IDs vs `Block`: always false**
`overrides` is `List<Integer>` via `addOverwriteableID(int)`, but checked with `overrides.contains(b)` where `b` is a `Block` — every placement in `createInWorld` (60-74) is skipped. Whole class silently broken by stale 1.7.10 ID semantics.
**Fix:** `addOverwriteableID(Block)` and compare block identity.

**3.4 `data\blockstruct\BlockArray.java:1330-1341` — iterator skips the last element**
`hasNext() { return blocks.size() > index + 1; }` — final coordinate never delivered; `next()` has no bounds check.
**Fix:** `return blocks.size() > index;`

**3.5 `data\WeightedRandom.java:224-238` — `saveAdditional` never writes the built tag**
Serializes every entry into a local `CompoundTag nbt` (225-237) but never executes `tag.put(s, nbt)`; `load()` (240-255) reads empty compounds — all weighted data silently lost on save/reload.
**Fix:** add `tag.put(s, nbt);` (mirror of `load`).

**3.6 `data\blockstruct\StructuredBlockArray.java:121-129` — invalid cast, CCE on every call**
`if (block.match((BlockCheck) id))` — vanilla `Block` does not implement `reika.dragonapi.interfaces.BlockCheck`.
**Fix:** match against `new BlockKey(id)` or block identity.

### HIGH

**3.7 `data\immutable\ImmutableItemStack.java:27-45` — hashCode breaks equals contract + extremely expensive**
`equals` ignores count (`isSameItemSameComponents`) but `hashCode` encodes the full serialized stack (including count) and constructs a fresh `VanillaRegistries.createWorldLookup()` **per call**. Equal stacks can hash differently → map-key corruption; each hash is a full codec encode.
**Fix:** hash `getItem()*31 + components.hashCode()` (count excluded); cache.

**3.8 `io\LuaBlock.java:329-345` — STUB: enchantment parsing gutted + stale NBT keys**
Body commented out ("todo Enchantment.enchantmentsList[id]"), so `map.put("Enchantments", li)` always stores an empty list; reads 1.7.10 keys (`ench`, `StoredEnchantments`, short `id`/`lvl` — NPE when `lvl` absent). 26.3 uses `DataComponents.ENCHANTMENTS`/`ItemEnchantments`.
**Fix:** port against `ItemEnchantments` or remove the dead path.

**3.9 `rendering\GlowLayer.java:26-29` — STUB: `submit` empty body, renders nothing**
Registered as a `RenderLayer` but draws nothing ("TODO 1.21+: Port to SubmitNodeCollector") — registered-but-gutted renderer.
**Fix:** port the glow pass to `SubmitNodeCollector` / `RenderTypes.entityTranslucentEmissive`.

**3.10 `rendering\TessellatorVertexList.java:42-46,86-89` — STUB: `addToTessellator` empty; `render()` draws nothing**
Whole geometry helper silently emits no geometry.
**Fix:** emit into a `BufferBuilder` from `Tesselator.begin(...)`, or mark unported and exclude call paths.

**3.11 `rendering\ReikaParticleEngine.java:174-196,414-449,300-352` — STUB: render pipeline gutted, particles tick but never draw**
`ParticleList.render()` draw loop is a TODO; `RenderModeFlags.apply()` ALPHA/ADDITIVE/ALPHACLIP cases empty, DEPTH is an empty `if`; `TextureMode.bind()` implementations empty ("RenderSystem.setShaderTexture removed in 26.1"). All registered custom FX render invisibly.
**Fix:** port to the 26.3 submit/RenderPipeline model (as `GlowLayer`/`TruncatedCube` did).

**3.12 `rendering\ReikaParticleEngine.java:45-58` — client-only class in common package, no dist guard**
`static final ReikaParticleEngine defaultCustomEngine = new ReikaParticleEngine() { super(Minecraft.getInstance().level, ...) }` runs at class-init. Any dedicated-server path that loads the class crashes. VERIFY consumers; move behind a `Dist.CLIENT` holder (as `WorldLocation.clientLevel()` does).

**3.13 `data\immutable\DecimalPosition.java:278-281` — STUB: `getAABB(double)` returns constant 1×1×1 box**
Real box commented out.
**Fix:** `new AABB(x-r, y-r, z-r, x+r, y+r, z+r)` / `ReikaAABBHelper`.

**3.14 `data\immutable\DecimalPosition.java:245-249` — STUB: `dropItem` body commented out**
Silent no-op; sibling `WorldLocation.dropItem` (193) *was* ported (but ignores `vscale` with a todo, WorldLocation.java:190-199).
**Fix:** use the WorldLocation-style spawn; restore random-offset/velocity-scale.

**3.15 `data\KeyedItemStack.java:186-190` — `compareTo` stub returns constant `1`**
`compare(x,x) != 0` violates the Comparable contract; sorted collections misbehave (duplicates never coalesce).
**Fix:** implement on item id + criteria.

**3.16 `data\maps\PlayerMap.java:62-64` — `get(String)` looks up `HashMap<UUID,V>` with a `String` key; always null**
**Fix:** resolve via `getPlayerList().getPlayerByName(s)` then UUID, or delete.

**3.17 Entire files commented out — dead "reference" files kept in the compiled source set**
- `rendering\StructureRenderer.java:1-659` (live replacement: `rendering.structure.StructureRenderer`)
- `rendering\LODModelPart.java:1-319`
- `effects\EntityBlurFX.java:1-547` (registration also commented out in `ReikaParticleEngine.java:47-51`)

Hard rule is "pristine or fully ported" — 100%-commented is a third, unsanctioned state, and `EntityBlurFX`/`LOD` removals also drop public API surface.
**Fix:** delete; `Sources/` is the designated reference location.

**3.18 `effects\StringParticleFX.java:9-23` — STUB: particle class neutered**
Constructor discards the `String`; `setScale`/`setLife` empty; `getGroup()` returns `NO_RENDER`. Original rendered text-carrying particles.
**Fix:** port the string-render pass or keep type registered with a real renderer.

### MEDIUM

**3.19 `data\maps\TileEntityCache.java:163-172` — erasure makes the CCE catch dead code**
`V cast = (V) tile;` inside `try/catch (ClassCastException)` — erasure means the cast can't throw there; the CCE surfaces at the caller. Validate against a stored `Class<V>` or drop the misleading catch.

**3.20 `data\immutable\BlockKey.java` — multiple defects on a key class**
- L24 `public BlockState blockID;` — mutable public field on an "immutable" key. Make final/private.
- L36-43: `Block.byItem` returns `Blocks.AIR` (not null) for non-block items and `is.getItem() == null` is never true — any item silently becomes AIR; the `MisuseException`s are unreachable.
- L69: `toString()` returns only the **namespace** (`minecraft`) — debug output actively misleading; return the full `Identifier`.
- L103-114: `saveAdditional`/`load` entirely commented (`todo`) — no working NBT serialization.
- L126-128: `compareTo` multiplies `Block.getId` by 100000 — overflow risk, comparator inconsistency.

**3.21 `data\immutable\Coordinate.java:269-272` — hashCode-only compareTo drops entries in sorted collections**
Two distinct coordinates with colliding hashes compare equal; one is dropped from a `TreeMap`/`TreeSet`. Tiebreak on x/y/z (as `WorldLocation` already does at 401-409).

**3.22 `io\LuaBlock.java:678-690` — comparator contract violation**
`if (o1.equals("type")) return Integer.MIN_VALUE;` even when both keys are `"type"` — TimSort `IllegalArgumentException` risk. Check equality first.

**3.23 `io\RemoteSourcedAsset.java:48,57` — client class referenced from common io code without dist guard**
`ClientEnvironment.resourceStream(...)` unconditionally; `WorldLocation` guards the same reference (WorldLocation.java:243). Server-side `getData()` class-loads `ClientEnvironment`. Add the guard.

**3.24 `io\ControlledConfig.java:243-247` — STUB: `checkReset` gutted to `return false`**
Versioned-config-reset path removed; `versionCheck()` (275-281) dead. Port the marker onto the TOML/ModConfigSpec migration or remove.

**3.25 `io\ControlledConfig.java:381` — `ModConfig.Type.SYNCED` (VERIFY)**
Historically CLIENT/SERVER/COMMON; if `SYNCED` doesn't exist in NeoForge 26.3 this fails at runtime. VERIFY against the NeoForge 26.3 source jar.

**3.26 `data\blockstruct\FilledBlockArray.java:24-31` + `StructuredBlockArray.java:197-218` — documented deferred methods**
`tally()/count()`, rotate overloads, `fillFrom`, `BlockEntityCheck`/`setTile` overloads "left out until they are" — sanctioned-by-note absences, but still unported API surface that 1.7.10 consumer code can call. Track in the ledger until restored.

**3.27 `data\blockstruct\BlockArray.java:946-949` — `addIfClear` liquid/transparent logic commented out**
`addLineOfClear` now stops at any non-air block where 1.7.10 continued through liquids/non-solids. Port to `getCollisionShape(...).isEmpty()` / fluid checks.

**3.28 `data\blockstruct\BlockArray.java:1156-1158` — STUB: `sortBlocksByDistance` empty body**
Reinstate the comparator (`BlockPos.distSqr`).

**3.29 `data\blockstruct\..\event\SetBlockEvent.java:43` — `isWorldgen` stubbed to constant `true`**
Every `SetBlockEvent.Pre/Post` claims worldgen; listeners that skip worldgen sets skip **all** sets.
**Fix:** compute `!ReikaWorldHelper.isChunkPastCompletelyFinishedGenerating(...)` or remove the field.

**3.30 `rendering\ReikaRenderDispatcher.java:7-17` — registration methods silently discard input**
`init()`, `registerBlockRenderer`, `registerRenderer` all empty "Obsolete" bodies — callers believe they registered `IBlockRenderer`s. Throw `MisuseException` or implement against the modern BakedModel/SubmitNodeCollector path.

**3.31 `gui\DummyContainer.java:21-25` — STUB: `stillValid` returns `false`**
Menu closes immediately on any interaction check; 1.7.10's dummy `canInteractWith` returned true.
**Fix:** `return true;`

**3.32 Client-only imports in common data/math/io classes (VERIFY)**
`data\CircularDivisionRenderer.java:5`, `data\Proportionality.java:4-5`, `math\Spline.java:15-16` (`SubmitNodeCollector`, `RenderTypes`), `io\EnumSound.java:3-4` (client sound classes). Dedicated-server classloading = `NoClassDefFoundError`. Verify call sites or split rendering methods into client handlers.

**3.33 `data\blockstruct\CurvedTrajectory.java:116-117` — `copy()` copies `magnitude` into `rotation`**
Forked trails inherit the speed (0.25) as their rotation angle. `t.velocity.rotation = velocity.rotation;`

**3.34 `event\client\BossColorEvent.java:19-21,52-58` — mutable static color cache**
Two-step post-then-read via statics is fragile across frames/threads. Return values directly from `fire()`; drop statics.

**3.35 `data\immutable\WorldLocation.java` — misc**
- L39-51,58: `private Level clientWorld` assigned but never used for resolution (`getWorld()` re-resolves); retains stale client-Level refs. Delete.
- L100-102,384-386: `getChunk()` builds a `WorldChunk(getWorld(), ...)` and `canSeeTheSky()` dereferences `getWorld()` — both NPE when no level is resolvable (its own contract allows null, 224-233). Null-guard or take a level parameter.

**3.36 `data\maps\TimerMap.java:22,82`; `data\maps\PlayerTimer.java:22`** — raw types; `timer.get(val)` unboxing NPE when absent.

**3.37 `data\maps\ValueSortedMap.java:25,76-79,119`** — comparator NPEs on removed/mid-iteration keys; every `put`/`remove` rebuilds the whole TreeMap (O(n log n) per mutation).

**3.38 `data\maps\ItemHashMap.java:282-292,132-141`**
- `ComponentItemKey.asItemStack()` drops component patches (documented TODO) — component identity lost for consumers using `keySet()` stacks for matching. Store the source stack / apply `DataComponentPatch`.
- `add` unchecked self-cast `(ItemHashMap<Integer>) this` — CCE at caller's use site. Specialize.

**3.39 `io\MIDIInterface.java:29-38` — method fully commented out**
`getNoteAtTrackAndTime` inside a block comment; verify consumers (MusicScore/PianoKeyboard path).

**3.40 `io\DirectResource.java:28` — `super(null, null) //todo null for now to compile`**
Compile-driven stub; VERIFY the `Resource` contract in the 26.3 jar and supply real values.

**3.41 Metadata-int semantics left over** — `KeyedItemStack.java:168,231-232` (`METADATA` criterion matched via `getDamageValue()`; on 26.3 damage is a component, overlapping the `NBT` criterion). Consolidate on components.

**3.42 `math\hexgrid\HexGrid.java:332-333`** — `FractionalHex bn = new FractionalHex(a.size, b.q + ...)` uses `a.size` for both endpoints; latent bug for mixed-size grids.

### LOW

- **3.43 `java.util.Random` where worldgen-adjacent** — `BlockArray.java:32`, `MultiBlockBlueprint.java:23`, `effects\LightningBolt.java:32`, `data\CircularDivisionRenderer.java:24`, `DecimalPosition.java:38`, `ReikaParticleEngine.java:32`, `WeightedRandom.InvertedWeightedRandom.java:133`. Normalize to `RandomSource` (main WeightedRandom path already uses it, line 17).
- **3.44 Raw types, ~85 sites** — representative: `PlayerMap.java:20`, `ItemHashMap.java:35`, `TimerMap.java:22`, `ThresholdMapping.java:24,27-28`, `ValueSortedMap.java:25`, `MultiMap.java:41`, `LuaBlock.java:44-54`, `XMLInterface.java:37-38,62`, `ControlledConfig.java:35,62,188,311,483,639,845`, `CustomRecipeList.java:535-536`, `BreadthFirstSearch.java:22-24,92,132`, `SlicedBlockBlueprint.java:37-41`, `BlockArray.java:1110,1201`, `RecentEventCounter.java:17`, `TimedSet.java:18`, `LinearSequence.java:17` (fully raw `LinkedList sequence`), `LastCallTimer.java:19`, `AngleMap.java:21`, `TierMap.java:9`, `CountMap.java:26`, `SubdividedProgressBar.java:11-12`, `CircularDivisionRenderer.java:25-26`, `SortedPairs.java:20`, `Proportionality.java:31`, `PointPath.java:15`, `GappedRange.java:7`, `ExtremaFinder.java:17`, `FluidHashMap.java:25,137,146`, `BranchingMap.java:23,134,224`, `SequenceMap.java:21,132,193,242,251`, `BranchingTree.java:18,86`, `MusicScore.java:28,275,408`, `PiecewiseExpression.java:16`, `StatisticalRandom.java:28,89`. Mechanical fix.
- **3.45 Mutable public static toggles** — `event\SetBlockEvent.java:22,28`, `event\BlockTickEvent.java:27` (`disallowAllUpdates`), `FilledBlockArray.java:38` (`logMismatches`). Document thread-safety or convert to options objects.
- **3.46 Unused/stale imports & dead locals** — `io\SyncPacket.java:5` (`Minecraft` imported in common code), `io\SoundLoader.java:5-6` (duplicate `FMLEnvironment` import), `event\BlockTickEvent.java:19`, `ReikaParticleEngine.java:6`, `LuaBlock.java:315`, `ItemMatch.java:85`.
- **3.47 `e.printStackTrace()` instead of the logger** — `io\RemoteSourcedAsset.java:71,134,140,147,161`, `io\XMLInterface.java:88,93,98`, `LuaBlock.java:382`, `CustomRecipeList.java:98,133`, `ControlledConfig.java:108,270,633`, `BlockArray.java:415,458,…`, `Configuration.java:108`.
- **3.48 Empty catch/else blocks** — `XMLInterface.java:270-272`, `ControlledConfig.java:162-164` (`catch NoSuchMethodError ignored`), `LuaBlock.parseObject` (`catch Exception ignored` at 571-573, 592-594, 599-601…). Log at debug minimum.
- **3.49 `io\SoundLoader.java:74`** — commented-out DirectResourceManager registration call; verify the 26.3 path replaced it.
- **3.50 `io\PacketTarget.java:46`** — `OtherPlayersTarget` `//todo check if this works`.
- **3.51 `event\client\PlayerInteractEventClient.java:20,23`** — legacy `int face`; replace with `Direction`.
- **3.52 `Direction.values()[i]` iteration** — `ReikaBlockPosHelper.java:26`, `CoordHelper.java:13`, `BlockBounds.java:193,205`, `BlockVector.java:32`. Fragile; use `pos.relative()` helpers.
- **3.53 `data\immutable\InventorySlot.java:19,24,51`** — modern `Container.getItem` returns `ItemStack.EMPTY`, never null; null branches dead (harmless). `increment`'s `count <= 0` path unreachable.
- **3.54 Commented-out integration blocks** — `CustomRecipeList.java:45-53,355-532` (Mystcraft/Forestry lookups). Acceptable while unported, but mark with `// DRAGONAPI-PORT:` so they aren't lost.
- **3.55 `io\MapOutput.getFilepath`** — embeds `ResourceKey.toString()` ("DIMResourceKey[...]") in a filesystem path; clean up to the location string.

**Standards-gap summary (cluster 3):** cache `Holder<Item>`/`Holder<Block>` instead of repeated `BuiltInRegistries` lookups (`CustomRecipeList.fullID`, `ItemHashMap.ItemKey`, `BlockKey`); adopt `StreamCodec`/`Codec` for `Coordinate`/`WorldLocation`/`BlockKey` serialization (also structurally fixes the round-trip bugs C5/3.20); `Component` for GUI-destined strings; prefer `pos.offset/relative/below/above` over manual math; datapack-driven config for `TreeReader`'s hardcoded SAPLINGS/SCAN_DEPTH maps if modded-tree parity matters.

**Verified fine (cluster 3):** `ICancellableEvent` usage; `AttackAggroEvent`, `FireSpreadEvent`, `IceFreezeEvent`, `LavaSpawnFireEvent`, `BlockTickEvent`, `GrassSustainCropEvent`, `BlockTransformResultEvent` all extend proper NeoForge `Event` types and post on `NeoForge.EVENT_BUS`; `ManagedItemHandler`/`ResultSlotItemHandler` correctly target the 26.3 `neoforge.transfer` API; `GhostSlot`, `ArmorSlot`, `TruncatedCube`, `ThreadSafeTileCache`/`ThreadSafeSet`, `NBTFile`, `HexGrid` (marker), `TreeReader`, the `oldforge` vendored config parser.

---

## Cross-cluster summary

**Total distinct findings: ~140** (34 CRITICAL, 37 HIGH, 43 MEDIUM, ~26 LOW clusters) across 654 files.

### Most urgent fixes (runtime-breaking / hard-rule violations)
1. **`instantiable\DecimalPosition.setBlock`** — infinite recursion → StackOverflowError (3.1)
2. **`accesstransformer.cfg` SRG names** — AT fails on MojMap runtime (0.1)
3. **`BlockArray.setTo`/`clearArea`, `MultiBlockBlueprint`, `BlockArray` iterator** — core block-struct utilities silently broken (3.2-3.4)
4. **`WeightedRandom.saveAdditional`** — silent save no-op, data loss (3.5)
5. **`CoreContainer.findSlot`** — OptionalInt null-check nonsense, always "slot not found" (2.1)
6. **`DragonAPI.isSinglePlayer()` hardcoded false** — gates all four mods (2.2)
7. **`ModWoodList` never-filled lookup maps + `PowerTypes` varargs ctor** — modded-content detection permanently dead (2.5, 2.6)
8. **`ReikaPlayerAPI.kickPlayer` / file-hash feature** — server anti-tamper dead (1.1, 2.19)
9. **Particle/rendering cluster** — `ThrottleableEffectRenderer`, `ReikaParticleEngine`, `GlowLayer`, `TessellatorVertexList` all silent no-ops (1.2, 3.9-3.11)
10. **Commands with hardcoded debug args** — `BiomeMapCommand`, `EntityListCommand`, `EventProfilerCommand`, `CommandableUpdateChecker` (1.3, 1.4, 1.26)

### Cross-cutting themes (apply as standards going forward)
- **No unmarked stubs** — the dominant violation; every gutted method above needs porting, a sanctioned `DRAGONAPI-PORT:` marker, or deletion with a ledger note.
- **Delete fully-commented corpse files** — reference copies belong in `Sources/`, not the compiled source set.
- **World/dimension keying** — static maps must key by `ResourceKey<Level>` (or `PlayerMap` by UUID) and clear on logout/`ServerStoppedEvent` (`ChunkManager` is the in-repo model).
- **Side gating** — common-package classes touching `Minecraft`/`Screen`/render APIs need dist guards or relocation to `client\` (`KeyWatcher`, `PopupWriter`, `ReikaParticleEngine`, `StructureBase`, `RemoteSourcedAsset`).
- **Reflection discipline** — never against pre-1.13 identities; never in tick/render hot paths (memoize); failures must log via `ReflectiveFailureTracker`, never empty catches.
- **Datagen for data** — hardcoded flammability, colors, lang strings, command-display data all move to providers.
- **`Component`, `Holder`, tags, `Codec`/`StreamCodec`, stable packet IDs** — the modernization checklist for any file touched from here on.
