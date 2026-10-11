# Review of DragonAPI PORT-AUDIT.md — 2026-10-07

The audit is a useful discovery list, but its severities and fixes are not yet an implementation-ready plan.
It identifies additional unfinished DragonAPI APIs beyond the earlier cross-project 19-item audit.
This review read the full document and checked selected priority findings, disputed claims and their
callers against current source. The extended review below covers the subsequently supplied sections 2 and 3.
It is not independent certification of every numbered finding or all 654 source files.
The initial review changed no gameplay/build code and ran no new runtime tests.
The subsequently authorized implementation and its validation are recorded below.

Authority: Minecraft 26.3 / NeoForge 26.3.0.51-beta, using the generated Minecraft sources and cached
NeoForge sources. Earlier successful unit/GameTest launches are evidence for those exercised paths,
not proof that every optional API or client mixin works.

## Initial section 1 review — historical source observations

| Finding | Review |
| --- | --- |
| 0: access transformers | The two `f_...` entries are stale relative to the current named sources, but the recommended `Minecraft minecraft` replacement is invalid: there is no such field in the inspected class. `ReikaRenderDispatcher` does not access either field; its methods are no-ops. `ModelPart.cubes` exists, but this source search found only commented cube-access examples in LODModelPart. Establish actual consumers and AT processing diagnostics before claiming a dispatcher crash or replacing/removing entries. |
| 0: duplicate mixin config | The stale empty dragonapi.mixin.json exists, but the current mods TOML explicitly wires mixins.dragonapi.json, contrary to the claim that it only wires access transformers. No reference to the stale config was found in the inspected resource/template/build wiring. Treat it as cleanup pending packaged-manifest confirmation, not an active failed mixin. |
| 0: mixin target drift | `defaultRequire: 1` is useful fail-fast behavior, not itself a design defect. This is a validation task. Compiling does not exercise every injection descriptor or client-only class; distinguish source/processor checks from actual mixin application in server and client launches. |
| 1.3 biomepng | The hardcoded arguments and missing seed-provider branch are real. The command does not necessarily map the hardcoded seed: parsing removes it, the provider branch is commented out, and the empty provider set falls back to the player's current world. It maps with fixed range/resolution instead of accepting command arguments. |
| 1.5 commented integrations | Incomplete/commented reference files are real, but the suggested deletion conflicts with the owner's instruction to preserve useful original information. Establish the absent dependency and preserve source/behavior behind a documented gate or outside the build until a full port is possible. |
| 1.6 versions | `readFromFile` returns `source` when `FMLEnvironment.isProduction()` is true, and the hardcoded `0a` on the other branch. The production condition is also present in `readFromJar`. The report must describe both paths; fixing only the final return would leave production version detection wrong. |
| 1.12 liquid properties | Comparing property collections by identity is wrong. The 26.3 method returns `List.of(propertyKeys)`, not two unmodifiable property maps as described. Check fluid/block identity and validate the desired water-versus-other-liquid behavior. |
| 1.14 sound helpers | `Level.playLocalSound` has an empty base implementation in 26.3, confirming the server no-op. But `playNormalClientSound` has a client-only contract and its identified packet caller is clientbound. Making it broadcast on a server is not automatically the right fix. For `playSoundAtEntity`, demonstrate an accepted server caller before assigning a gameplay regression. |
| 1.15 transfer | The method performs no transfer. It does not currently clear or discard the player's inventory. The correct consequence is an ineffective API; caller-level item loss would need separate evidence. |
| 1.16 Lua errors | Missing forms and ignored metadata are real. The current CCCompat boundary catches RuntimeException and converts it to LuaException; the claim that IllegalArgumentException escapes raw into CC is wrong. Lua methods should use LuaMethodException and the boundary should keep the dependency-specific exception conversion. Runtime numeric IDs should have a deliberate registry-name migration/compatibility contract. |
| 1.17 tracker | Unlocked reads, unsynchronized isInTick and live Player keys are confirmed. Concurrent invocation is not reproduced; describe a thread-safety contract failure/conditional race, not proven map corruption. No reverse tracked-to-queued nested lock acquisition was found in the inspected method: demonstrate a real cycle before claiming inconsistent lock order/deadlock. |
| 1.20 KeyWatcher | Initializing the outer KeyWatcher and server-safe Key enum does not automatically initialize the nested client Keys enum or KeyTicker. Their client references do not prove the stated dedicated-server crash. Shared Key/KeyWatcher calls already occur in gameplay and server test sources. Separating the client implementation is sensible, but concrete input defects below take priority. |
| 1.23 popup | The public version-notification method directly reaches Screen-derived PopupWriter, which is unsafe if invoked on a dedicated server. No external call to that notification method was found in the searched family Java sources. Retain as a conditional API defect rather than a reproduced normal-login crash. Route messages to the intended player, not automatically all players. |
| 1.27 Lua returns | Non-string Component/UUID return values and missing empty-slot handling are real API-contract issues; exact CC conversion behavior was not reproduced. Modern empty slots use ItemStack.EMPTY, so `is == null` misses the normal empty case. LuaGetPlacer also dereferences a possibly absent placer. |
| 1.28 metadata gates | Renaming MULTIBLOCK-PORT to BLOCK-PORT does not implement behavior. Determine the actual block-state semantics and accepted consumers. A marker documents an unavailable dependency; it does not make a gutted method a complete port. |
| 1.34 sync reflection | The file is libraries/io/ReikaPacketHelper.java, not libraries/ReikaPacketHelper.java. The active periodic sync uses BE_NBT_SYNC; generic sync was already made client-only by the earlier fixes. Resolve compatibility consumers before deleting an API or enum ordinal. |
| 1.36 key cache | PlayerMap stores UUID keys internally, not Player references. UUID entries/stale pressed states need lifecycle cleanup, but this is not a retained-player-entity leak. |
| 1.37 empty cache | headCache is unused and empty. Removing dead code is hygiene; an unused empty map is not a demonstrated entity leak. |
| 1.38 break event | BreakBlockEvent is the current 26.3 event. The suggested old BlockEvent.BreakEvent-style replacement is not a current signature. Synthetic events can invoke additional listeners, but the existing cancellation check also provides protection integration. Audit server protection/interaction checks and parity before removing it. |
| 1.40 debug overlay | The active code already obtains getBlockEntity and checks te != null. The commented has-BE precheck is redundant, not evidence of a missing current check. The event-layer type/TODO checks should be assessed separately. |
| 1.47 SpecialDayTracker | Do not delete the class. Only the old weather method is commented out; calendar methods are live. Accepted VoidMonster MonsterGenerator uses isHalloween(). A weather implementation elsewhere does not replace the calendar API. |
| 1.52 config key parsing | Stronger than the audit's hypothetical TOML concern: the current caller passes DragonOptions.CTRLCOLLECT, an enum implementing StringConfig, while readFromConfig insists on instanceof oldforge.Property. The non-disabled collection-key lookup therefore throws now. |
| 1.59 packet ordinals | Ordinals remain a maintenance concern, but the bridge now explicitly negotiates protocol 2.0.0. They do not establish current same-version cross-channel dispatch. Preserve compatibility or bump the negotiated protocol deliberately when changing wire IDs. |

Other proposed fixes need API verification: 26.3 Tag.asString()/typed tag access rather than the suggested
getAsString spelling; registry iteration rather than assuming EntityType.getKey; pipeline-local rendering
state rather than restoring global fixed-function lighting.

## Additional confirmed input defects

1. **Missing LALT shifts key identities.** The public server Key enum includes LALT after LCTRL;
   private client Keys does not. Keys.getServerKey uses ordinal indexing and sendPacket sends that
   ordinal. PAGEUP consequently becomes LALT, PAGEDOWN becomes PGUP, and every following key is shifted.
   Use explicit key identities, including LALT, with a round-trip mapping regression test.
2. **Client key polling is not wired.** Searches of the family Java source found KeyTicker.instance
   only in its declaration; DragonAPI registers three other tick handlers but never this one.
   The inspected initialization path does not drive the normal polling/sending path. Register the
   client ticker through a physical-client entry point and verify real key press/release behavior.
3. **ReikaRenderDispatcher is itself an unimplemented compatibility API.** Its registration methods
   silently do nothing; this is distinct from the audit's claimed illegal field access. Check remaining
   accepted consumers and preserve the original behavior/reference while choosing a modern model route.

## Initial section 1 findings — before the authorized fixes

- 1.1: kickPlayer is empty; ModFileVersionChecker calls it and logs that a player was kicked.
- 1.3 / 1.4 / 1.26: biomepng/profileevent have hardcoded arguments; EntityListCommand builds an empty list.
- 1.7 / 1.8 / 1.9: FileInputThread.run, BlockArrayComputer operation branches and replyToPacket are empty;
  getMinecraftPacket returns null. Public APIs are incomplete, but priority depends on accepted callers.
- 1.11: isUnbreakable checks SemiUnbreakable but omits ordinary negative destroy speed.
- 1.13: the breaker's server path uses playLocalSound, whose 26.3 base implementation is empty.
- 1.18: BlockProperties' flammable catalogue omits the commented common wood/leaves/wool entries.
- 1.19: WorldID iterates ListTag values and casts them to String; failures are caught and produce NONEXISTENT.
- 1.22 / 1.25: warning icon rendering and the listed client packet cases remain incomplete.
- 1.29: special tree depths are commented out on a false missing-ModWoodList premise.
- 1.32: Ingredient expansion caches by Ingredient indefinitely and has no reload invalidation;
  the ordinary ingredient fallback scans BuiltInRegistries.ITEM.
- 1.33: NBTCompat resolves reflection per lookup and broadly suppresses errors despite native typed APIs.
- 1.35: WorldgenProfiler retains a static Level after finishProfiling.
- 1.50 / 1.55: Patreon lookups dereference absent entries; version-cache parsing assumes two split fields.

This list records source confirmation, not reproduced end-to-end consequences or acceptance of every
original severity. Remaining IDs are not independently certified by this review.

## Recommended implementation order

1. Restore key polling/mapping/config parsing together; test normal client input and server key state.
2. Restore kick behavior and validate disconnect/mismatch paths; repair production version detection,
   malformed version-cache parsing and WorldID tag decoding.
3. Complete Lua argument/return/empty-state contracts and command trees with live regression coverage.
4. Replace NBT reflection, add cache/session lifecycle handling and verify breakability/protection parity.
5. Resolve client particle/player-renderer/popup work with real client launches; preserve unavailable
   integrations and original references while porting their dependency clusters fully.

Keep confirmed defects, conditional risks, hygiene and verification tasks separate. Deprecating an API,
renaming a gate or deleting useful source is not equivalent to restoring its behavior.

## Authorized implementation — 2026-10-07

The five recommended work groups above have been implemented against the active 26.3 APIs.
This closes the listed defects in those groups; it does not certify every finding in the original 61-item audit.
The original PORT-AUDIT.md and dependency/reference sources are preserved.

| Work group | Implemented behavior |
| --- | --- |
| Input (1.20, 1.36, 1.52 and the additional mapping/polling defects) | Client-only polling is registered during client setup; configurable actions read current key bindings. SDL raw keys include Alt. Explicit 0–27 wire identities replace ordinal mapping. StringConfig enum lookup works. Logout, player replacement and server stop clear pressed state. Shared UUID key state uses a concurrent map. |
| Disconnect, versions and world identity (1.1, 1.6, 1.19, 1.23, 1.55) | kickPlayer disconnects the real connection with a Component reason. ModVersion reads loaded NeoForge artifact metadata and supplied legacy property files, retaining patch/prerelease/build values. Malformed cache lines are tolerated; cache writes are atomic where supported. Version changes notify only the joining player through a clientbound popup packet. WorldID reads typed strings, writes the mod list, keeps long session indices and uses the actual save directory. isSinglePlayer uses the guarded integrated-server query. |
| Lua and commands (1.3, 1.4, 1.16, 1.26, 1.27) | Lua inventory methods accept supported argument forms, validate finite whole-number bounds, honor damage/count matching and emit Lua strings/nil or the documented Empty sentinel. Placer lookup works without a live placer. biomepng has typed range/resolution/grid arguments, seed/list/range support and a real 26.3 biome resolver. entitylist enumerates the actual registry. profileevent and checker have real subcommands/arguments and console feedback. |
| NBT, caches and protection (1.11, 1.32, 1.33, 1.35, 1.38) | Native typed NBT access replaces reflection. Ingredient expansion uses ingredient holders with bounded cache retention and reload/session invalidation; non-simple ingredient predicates compare structurally, including serialized predicates for implementations lacking structural equals. Worldgen profiling releases its Level and session state. Ordinary negative destroy speed is unbreakable. Server interaction protection precedes the current BreakBlockEvent cancellation check, preserving configured admin/singleplayer behavior. |
| Client rendering (1.10, GUIRELOAD in 1.25, 3.9, 3.18 and the PopupWriter icon) | Text particles extract immutable text/positions and submit billboard text through a native ParticleGroup. Both player skin renderers receive glow layers; the player-specific render hook submits the original additional model parts through captured render state. ReikaModel provides the HumanoidModel children required by 26.3. Popup warning artwork is drawn and short popups retain a valid dismissal target. GUIRELOAD resizes the actual current screen. |

**Wire compatibility:** DragonAPI now negotiates protocol **2.1.0**. Key mappings changed and biome
map batches now contain at most 512 samples so their integer payload remains below the existing 4096-int
limit. Clients and servers must update together. Packet subtype ordinals were preserved.

**Save migration boundary:** World IDs and modversions.list now belong to the actual save directory.
Ambiguous legacy files stored in the server working directory are preserved; they are not automatically
assigned to an arbitrary save. A save without its own ID/cache establishes its own baseline.

**Validation:** 49 unit tests passed (DragonAPI 12, RotaryCraft 32, ReactorCraft 5), with no failures,
errors or skips. The complete seven-module active family compiles through :TestInstance:compileJava.
Seven dedicated game-server regressions passed for Lua inventory/placer contracts, key cleanup,
breakability, cancelled protection events, component ingredient predicates, actual command execution
and a live player disconnect. Logs: ../dragonapi-audit-tests.log and ../dragonapi-audit-gametest.log.

A disposable integrated client fixture is opt-in via dragonapi.auditClient and otherwise inert in
TestInstance. It uses a copied GameTest world under ../build/dragonapi-client-audit, never the normal
play-test save. It checks server-decoded jump press/release packets, text extraction, custom player
model submission and a newly written biome PNG. The final run emitted DRAGONAPI_AUDIT_CLIENT_PASS
and completed successfully in 2m 11s. No model bake failures remain in that run. The client log is
../dragonapi-audit-client.log.
Physical hardware input, human visual assessment and a separately packaged production distribution
remain unverified.

Client launching also exposed ChromatiCraft piston-target overlay UVs outside the texture bounds.
ChromaModelProvider now emits explicit [0,0,16,16] overlay UVs while preserving the slightly enlarged
anti-z-fighting geometry, axis placement, tint and glow. :ChromatiCraft:runClientData succeeded and
regenerated all six model variants. The subsequent family client launch has zero piston-target/model
bake errors. Log: ../dragonapi-audit-datagen.log.

## Remaining work and limits

- ReikaRenderDispatcher, the optional legacy particle engines, legacy threading/packet construction
  helpers and the inventory-to-chest API still need full behavior ports after identifying accepted
  consumers. They were not deleted, stubbed or declared complete by this pass.
- EventProfiler command parsing/reporting is restored; automatic legacy event-bus listener
  instrumentation is still unported. Enabling the command alone does not measure every NeoForge
  listener. The obsolete online update endpoint is not reactivated by the checker command repair.
- The stale unused mixin config and access-transformer entries remain maintenance findings; no fake
  Minecraft.minecraft field replacement was applied. Client/server launches exercise active mixins,
  but they do not prove every optional mixin path or packaged metadata configuration.
- Other client packet TODOs and unavailable-mod integrations require their own full dependency ports.
  Preserve the original sources and documented gates until those implementations land.
- Five pre-existing RotaryCraft item texture files fail native PNG decoding: advancedgear, railgun_ammo
  and target are empty; molten_hsla_bucket contains an ACE database header; tungsten_alloy_spring
  contains an OLE compound-document header. Git HEAD contains the same invalid bytes. Restore verified
  original artwork rather than substituting another item's image. These remain visible missing-texture
  errors in the family client log, separate from the fixed piston-target model failure.

The client also reports a background ChromatiCraft 4x4 puzzle-generation timeout and a Windows
performance-counter lookup error. Those were not addressed by this DragonAPI API pass; client smoke
success does not mean the entire family log is free of errors.


## Extended review of sections 2 and 3 — 2026-10-07

Read both added sections in full, checked the priority defects and relevant family callers against
current source, and checked disputed API claims against the generated 26.3.0.51-beta Minecraft sources
and FancyModLoader 12.0.8 sources. This is a review pass: no gameplay, build configuration or resource
implementation was changed. A small disposable JVM probe exercised the current ValueSortedMap source.
The earlier 49 unit tests / seven GameTests / client launch were not rerun for this review and do not
cover the newly confirmed defects below.

The audit now contains **170 numbered entries** (61 in section 1, 54 in section 2, 55 in section 3), plus
three build/resource headings. The document's approximately 140 distinct findings is not a reproducible
count without a deduplication rule: entries overlap and some numbered entries bundle several defects.
Do not present either count as independently reproduced runtime failures.

### Confirmed priority defects

Locations below are current source locations, rather than historical line numbers from the audit.

| ID | Current source evidence and consequence | Review of the proposed repair |
| --- | --- | --- |
| 3.1 | DecimalPosition.java:251–253 calls the same setBlock(Level, Block) overload without a terminating condition. Every invocation recurses until StackOverflowError. The ItemStack overload also reaches it. | Port the coordinate/state mutation path and check return semantics. This is the clearest immediate fix. |
| 3.2, 3.4, 3.28 | BlockArray.java:1022–1031 reads and discards states in setTo; clearArea delegates to it. hasNext at 1331–1332 excludes the last position, including the only position in a one-entry array. sortBlocksByDistance at 1156–1158 does nothing. | Repair as a block-array cluster; verify empty/single/multiple-entry iteration, mutation, clear, ordering and exhausted-next behavior. Keep list/set and subclass data consistent. |
| 3.3 | MultiBlockBlueprint.java:27 stores Integer overwrite entries but canPlaceBlockAt at 76–78 checks a Block. The base implementation never accepts a populated overwrite list. | Migrate to block/state identity with a deliberate compatibility contract. Do not describe subclasses overriding canPlaceBlockAt as necessarily broken. |
| 3.5 | WeightedRandom.java:224–238 builds entries/total/max/dynamic but never attaches the compound to the caller's tag. Loading a fresh tag then returns early because the label is absent. | Attach the serialized compound and test a real weighted-data round trip. The audit's claim that load always reads an empty compound is inaccurate: it checks contains first. |
| 3.6 | StructuredBlockArray.java:121–129 casts Block to BlockCheck inside its data loop. Ordinary vanilla blocks do not implement the interface. | Non-empty arrays throw; empty arrays return zero. Match block identity or deliberate state criteria. new BlockKey(id) alone means the default state and would undercount logs with another axis or machines with another facing. |
| 2.1 | CoreContainer.java:350–361 discards the OptionalInt returned by super. Its null branch is unreachable for the inspected superclass implementation. The relay branch also ignores the supplied container and attempts to return a backing-inventory slot index. | Preserve found **menu indices**. Resolve the relay's intended identity/index contract; changing null to isEmpty alone does not repair all of this method. OptionalInt is an ordinary reference type; non-null is established by the superclass implementation, not a Java value-type rule. |
| 2.26, 2.27 | CoreContainer.java:292–295 permits any distance while the tile remains live; alwaysCan is ignored. The fallback slot uses a shared static mutable chest. | Restore reach, same-level/live-tile checks and the intentional always-interactable contract. Use a per-menu fallback with explicit insertion/removal behavior. Dependent menu overrides and packet checks must be assessed separately. |
| 3.7 | ImmutableItemStack.java:28–44 compares item/components but, for patched stacks, hashes a serialized stack including count through a freshly built vanilla registry lookup. Equal patched stacks with different counts can hash differently. | Use component-aware count-independent hashing. Also close the mutable-reference escape at getItemStackReference:22–24 before caching hashes or claiming the wrapper is immutable. 26.3 already provides ItemStack.hashItemAndComponents. |
| 3.15, 3.21, 3.22 | KeyedItemStack.compareTo returns 1, including self-comparison. Coordinate.compareTo uses only hashCode. LuaBlock's string comparator returns MIN_VALUE for comparing type with itself. | Restore comparator reflexivity/order and define equality compatibility. Preserve Coordinate.coordHash because its documented callers use it for seeded generation; change the ordering, not the seeded hash formula. |
| 3.37 | ValueSortedMap.java:118–120 orders keys by values without a key tie-breaker. Distinct equal-valued keys disappear from the sorted view. A supplied Comparator<V> receives K keys instead of values. | This is stronger than the audit's unproven removed-key NPE claim. Repair the representation/comparator contract and setter rebuild semantics; preserve all equal-valued keys. |
| 3.38 | ItemHashMap.java:282–292 reconstructs component keys without their patch. Iterated keys therefore lose their identity. | Apply the saved patch through the real public ItemStack.applyComponents(DataComponentPatch) API at Minecraft source line 869. Internal access/reflection is unnecessary. Keep integer accumulation out of an unconstrained generic map API. |
| 2.5, 2.31 | ModWoodList.java:478–490 never writes logMappings or leafMappings. getRandomWood at 418–424 can loop forever if no catalogue entry exists. Plank lookup is null; sideways placement is always false. | Populate supported entries and choose from an explicitly computed non-empty available set. A universal plank tag cannot identify the correct planks for a particular species; preserve verified original associations. |
| 2.6 | PowerTypes RF, FE and HYDRAULIC use the empty varargs constructor; it sets exists=false. ElectriTiles.java:150–153 visibly gates RF cable/battery availability on PowerTypes.RF. | Define RF/FE support against the actual modern integration. Do **not** blindly add a no-arg constructor returning true: that also invents Hydraulic availability. No in-game RF machine availability test was run in this review. |
| 2.3, 2.4, 2.19 | ModOreList.getGennableIn and getOreModFromItemStack are incomplete and return null. DragonAPIMod.basicSetup still does not compute/register file hashes or initialize its update checker. | These are real incomplete APIs. Restoring kickPlayer did not restore the whole jar-mismatch feature. Verify per-ore host semantics, ownership filtering and packaged artifact paths; namespace alone does not establish that an arbitrary item is an ore. |
| 2.7, 2.8, 3.40 | DirectResourceManager.getNamespaces returns null; getResource fabricates a resource for every identifier and ignores namespaces. DirectResource uses null PackResources metadata. | Port manager/resource ownership together. DirectResource overrides open, so null streamSupplier does not automatically break that method; inherited sourcePackId still dereferences the null pack. Expose real namespaces and missing-resource behavior while retaining supported filesystem/classpath assets. |
| 3.29, 3.31, 3.33 | SetBlockEvent declares every set to be worldgen; DummyContainer.stillValid always returns false; CurvedTrajectory.Trail.copy writes magnitude into rotation. | Source defects confirmed. Derive generation context from the actual call path; test dummy-menu use; copy the complete velocity state. Do not infer every event is posted or every dummy menu is registered from these method bodies. |

**Coordinate counterexample:** (0,1,0) and (0,0,31) both hash to 29822 under the current formula.
They are unequal but compare as zero, so a sorted set treats them as the same entry. This is a
source-level worked counterexample, not a launched Minecraft reproduction.

**ValueSortedMap runtime probe:** compiled the exact current ValueSortedMap.java with Java 25 into
../build/dragonapi-audit-review-probe and ran two cases. A String→Integer map containing first=7 and
second=7 reports size=2 but keySet=[first], values=[7]. Setting Integer::compare and putting a String
key produces ClassCastException because the value comparator receives that key. Output is preserved
in ../dragonapi-audit-review-probe.log. No accepted game source was modified by the probe.

### Findings requiring correction or narrower wording

| ID | Correction |
| --- | --- |
| 2.2, 3.9, 3.18, 3.55 | Already repaired in the authorized pass: isSinglePlayer delegates to the guarded client helper; GlowLayer submits the emissive model; StringParticleFX has a native text group; MapOutput uses a safe dimension path. The audit's urgent-fix list still describes their old bodies. Mark resolved with the existing evidence instead of fixing again. |
| Earlier review's client-status row | Corrected here: the repaired paths are 1.10, GUIRELOAD in 1.25, 3.9, 3.18 and PopupWriter artwork. They are not repairs of ThrottleableEffectRenderer (1.2), RailCraft integration (1.21), SettingInterferenceTracker's icon (1.22) or generic lighting methods (1.24). Those remain pending. |
| 3.25 | ModConfig.Type.SYNCED is correct. FancyModLoader 12.0.8 ModConfig.java:103–111 declares it as server-instance configuration synchronized during connection. Replacing it with historical SERVER/COMMON names would regress this 26.3 code. |
| 2.42 | The active ControlledConfig already builds/registers ModConfigSpecs and migrates legacy defaults. The remaining commented DragonOptions attempt is cleanup/reference information; a whole migration is not still absent. DEBUGKEY's input-code label is a separate correction task. |
| 2.15 | The claimed Identifier.parse("") parser exception is false in the generated 26.3 source. isValidPath accepts the empty string and parse gives it the default minecraft namespace. The resulting sound has no useful registered event/asset; repair that contract, but do not call it a guaranteed parser crash. |
| 3.20 comparator | The actual expression is 100000 * Integer.compare(...), so its result is only -100000, 0 or 100000. The asserted multiplication overflow does not occur. Other BlockKey findings—mutable state, non-block items becoming AIR, incomplete serialization and namespace-only diagnostics—are confirmed. |
| 2.14 | The legacy world-only constructor does use ARROW, but there is also a constructor taking the proper EntityType. Accepted ReactorCraft EntityRadiation and ParticleEntity paths use that constructor. The claim that all inert entities save as arrows is wrong; audit the remaining legacy constructor callers and the accepted source boundary. |
| 2.9 | Empty base submit is real. doRenderModel returns a visibility/pass boolean; merely calling it emits no geometry. RotaryTERenderer and ReactorTERenderer implement submit themselves. Treat inherited-no-op subclasses as the defect; do not claim all family machine rendering is dead or reintroduce live entities into deferred rendering. |
| 2.10, 2.11, 2.13, 2.25 | The tank helper, unregistered typed-payload drafts and old CompoundSyncPacket are incomplete/dormant. The active bridge and BE_NBT_SYNC are distinct working paths. CompoundSyncPacket's singleton/race design is unsafe if revived, but no active dispatch or current race was reproduced. Do not register both network systems without a complete behavior/protocol migration. |
| 2.12, 2.28 | Tests uses the wrong modid, but the inspected class has no SubscribeEvent methods for that annotation to bind. The debug item is actually registered through DragonAPI.java:103 independently of the annotation. Its server use reaches client PopupWriter and shift-use removes index 0 without a size check—stronger concrete concerns than the annotation alone. Preserve registry/save compatibility when separating development-only tools. |
| 2.16, 2.17, 2.18 | The historical BE hooks, same-state break replacement and empty tier-resource defaults need consumer/source-parity review. Not every multiblock must own a BE, and a generic EntityBlock conversion or invented replacement block can change behavior. The examined BlockChromaTiered consumer remains legacy source; distinguish it from accepted current content. |
| 3.27 | The preserved old addIfClear condition explicitly excludes LiquidBlock. The audit's assertion that it originally continued through liquids is unsupported and contradicted by that reference. Non-solid handling is incomplete; blindly accepting every empty collision shape can admit fluids and change traversal semantics. |
| 2.33, 3.12, 3.23, 3.32 and import-only findings | Class package/import presence alone does not prove dedicated-server failure. Distinguish client-only methods from actual common call paths. RemoteSourcedAsset.getData directly invokes a client resource helper before its filesystem fallback; StructureBase's implicated method is specifically the display-preview method. Those require explicit side contracts and caller checks. |
| 2.37 | getFileInsideJar(File,...) loses ownership of the opened JarFile; closing it before the returned stream is consumed is also wrong. Return a stream that owns/closes both, or fully read the entry within a scoped jar. copyFile(File,File,...) does leak the input when opening the output fails. |
| 3.19, 3.36, 3.38 accumulation | Generic-cast/unboxing concerns are real but narrow the statements. V extends BlockEntity erases to BlockEntity, so the local TileEntityCache cast cannot validate the specific subtype. TimerMap.get unboxes missing values; PlayerTimer.get already defaults to zero. ItemHashMap's self-cast itself is erased; an incompatible stored value can fail at the Integer read. |
| 3.42 | HexGrid interpolation constructs its result with the explicit size argument and does not use the endpoint FractionalHex sizes. Changing bn's a.size to b.size alone does not alter that output. Establish whether mixed-grid inputs are supported and check distance/world-coordinate semantics before calling this a demonstrated defect. |
| Broad standards suggestions | Use modern native APIs where they preserve behavior. Flammability defaults are block/fire hooks, not an existing vanilla datagen schema. Direction.values iteration is valid for visiting all six faces; it is not intrinsically an ordinal-wire bug. Record concrete behavior/performance evidence before escalating hygiene recommendations. |

### Additional source issues found while checking the additions

- DecimalPosition.negate at 259–261 returns the original coordinates without negating them. The method
  name and implementation disagree; verify and restore its original math contract with negative/fractional tests.
- ImmutableItemStack.getItemStackReference exposes the internal mutable stack. This undermines the
  immutable map-key guarantee even after a count-independent hash repair; do not cache a hash while
  retaining that escape unchanged.
- ItemHashMap.ItemKey.equals accepts ComponentItemKey by item identity, while ComponentItemKey.equals
  rejects plain ItemKey and additionally compares components. Equality is asymmetric; the subclass also
  computes a different hash. Repair key equality symmetrically as part of the component-key work.
- OtherPlayersTarget currently queries the nearby ServerPlayer list without excluding its source player.
  The TODO is not evidence of failure, but the implementation does not implement an obvious 'other
  players only' contract. Compare the original targeting contract before preserving or changing that behavior.

### Recommended next implementation order

1. Repair position/block-array/blueprint mutation and iteration, including DecimalPosition recursion,
   negate/radius/drop behavior and StructuredBlockArray block matching. Add real world mutation checks.
2. Repair persistence and key/collection contracts: WeightedRandom round trips, ImmutableItemStack,
   Coordinate/KeyedItemStack/LuaBlock ordering, ItemHashMap component reconstruction/equality and
   ValueSortedMap equal-value/custom-comparator behavior. Preserve seeded hash compatibility.
3. Repair CoreContainer slot lookup/relay/fallback and reach semantics with meaningful menu tests.
4. Repair the RF/FE availability gate and supported wood/ore catalogue behavior, honoring unavailable
   dependencies and the actual source allowlists.
5. Port dynamic resources/sounds as a complete owned resource-pack path, then restore file-hash
   registration and the remaining rendering/integration clusters against verified consumers.

Retain original reference sources. A new marker, an exception, or deletion is not a substitute for a
full behavior port. In particular, do not follow the audit's blanket deletion recommendations or add
unsupported registrations merely to silence a finding. Low-priority/raw-type/logging clusters remain
review leads rather than individually reproduced bugs; this pass does not certify them all.


## Extended audit implementation — 2026-10-08

Implemented the five recommended priority groups against the generated Minecraft 26.3.0.51-beta
and cached NeoForge sources. Historical findings above describe the pre-repair source. This pass
repairs confirmed behavior; it does not certify every one of the audit's 170 numbered entries.

| Group | Result | Regression evidence |
| --- | --- | --- |
| Positions and structures | DecimalPosition performs real block placement/item drops and fractional negate/radius math. BlockArray mutates/clears the world, visits its last entry, removes the returned iterator entry and orders distance as the original inward comparator did. Structured counts use block identity across properties; removal, offsets and flips preserve subclass data. Filled arrays also preserve placement overrides, including world-free layouts. Blueprints store Blocks; the legacy integer overload explicitly means the current registry runtime ID. | Unit empty/single/multiple iterators, exhaustion/removal/distance and decimal math; server placement, drops, structured state variants, blueprint overwrites, offsets/mirrors and full BlockState codec round trips. |
| Persistence and collection identity | WeightedRandom attaches its tag, derives totals from loaded entries, handles replacement/removal/clear and updates historical options. ImmutableItemStack returns copies and hashes item/components independently of count. KeyedItemStack equality includes matching criteria; explicit matches/match methods provide fuzzy matching. Component ordering uses weakly interned component snapshots, including transient components, without requiring serialization. ItemMatch and the grinder's locked-seed checks use the intended predicate. ItemHashMap reconstructs patches and has symmetric key equality, defensive views, consistent clone/mode behavior and a typed integer accumulation helper. Coordinate ordering is lexicographic; its seeded hash is unchanged. Lua/ValueSortedMap comparator contracts and missing TimerMap values are corrected. | Weighted persistence/dynamic/history/zero-boundary tests; coordinate hash collision and unchanged seed hash; value-sort ties/custom comparator; server count/component identity, transient components, iteration/clone, strict ordering and fuzzy matching. |
| Menus and inventory | CoreContainer relay slots are real hidden menu slots with valid backing indices. Native findSlot retains menu indices. Fallback slots are per-menu, empty and noninteractive. Reach requires the same level, the current live tile and normal eight-block range unless deliberately overridden. Invalid clicks/transfers are rejected; DummyContainer is valid for its slotless contract. InventorySlot marks partial mutations dirty and prevents negative/full-slot growth. | Real engine menu GameTest covering relay identity/menu index, fallback behavior, reach override and removed tiles. |
| Availability and catalogues | RF/FE use native NeoForge availability; Hydraulic remains unavailable until its integration exists. Current Chroma glow log/leaves/sapling identifiers populate and lazily refresh the wood maps. Random wood selection has a finite available set; axis and natural-leaf queries use actual state properties. Planks require a real loaded crafting recipe yielding a plank block. Ore ownership filters actual ores before identifying a loaded owner; generation variants use native stone/deepslate/netherrack tags and original fallback locations. Catalogue reload runs after DefaultDataComponentsBoundEvent, avoiding the earlier tag event's unbound component defaults. | Server RF/FE and ore filtering/tag variants; menu test also verifies current Chroma wood mappings/axis support when loaded. |
| Resources, sound and lifecycle | DirectResourceManager registers an owned native client pack with full identifiers, real namespaces/listing, correct missing-resource behavior, valid source-pack metadata and reload invalidation. Filesystem/classpath/dynamic resources retain their sources; sound registration and SingleSound playback use the working paths with normal data-generated sound definitions. Jar-entry streams close their owned jar, copy paths close inputs on output-open failure, and both copy streams close even on a close failure. Basic setup computes/registers packaged hashes; FILEHASH controls checking after config loads. Client resource/preview calls have explicit physical-client contracts. BlockEntityRenderBase.submit is abstract; accepted renderers already implement it. Additional fixes preserve velocity copies, exclude the source player from OtherPlayersTarget, use Direction inside client interaction events and keep the registered debug item side-safe. | Server owned-resource metadata/listing/cache invalidation and missing-resource checks; dedicated startup and all accepted family sources compile. Native client pack reload verification is recorded below after completion. |

The final unit/server command was:

```powershell
.\gradlew.bat :DragonAPI:test :RotaryCraft:test :ReactorCraft:test :TestInstance:runGameTest -PgameTestSelector=rotarycraft:dragonapi_* -PexcludeExternalMods --console=plain
```

It passed: **57 unit tests** (DragonAPI 20, RotaryCraft 32, ReactorCraft 5), zero failures/errors/skips,
and **all 11 required dedicated-server GameTests**, with the active family modules compiled. Log:
`../dragonapi-extended-validation.log` (BUILD SUCCESSFUL, 46 seconds). These are focused contracts,
not complete gameplay coverage of the family.

### Remaining boundaries

- No verified 26.3 version feed is configured. The inherited obsolete HTTP feed remains gated;
  custom supported endpoints can register. Computing artifact hashes does not establish a valid feed.
- No accepted Hydraulic integration or verified replacement for the historical Pneumatic probe exists.
  Neither is reported available by inventing a class name or enabling an unsupported power system.
- Glow-wood plank lookup has no accepted species-specific crafting recipe to resolve yet. The helper
  queries actual loaded recipes; it does not fabricate a plank association. Legacy wood integrations
  still need full subsystem ports against their target APIs.
- ItemHashMap integer accumulation is now `ItemHashMap.add(ItemHashMap<Integer>, ItemStack, int)`.
  The excluded pristine Chroma crafter's old instance call must migrate when that file is fully ported.
  Other excluded heterogeneous KeyedItemStack consumers must adopt explicit fuzzy predicates as needed.
- The old world-only InertEntity constructor, dormant typed payload/CompoundSyncPacket drafts,
  historical block hooks, tier resources, dispatcher/particle engines, missing integrations and the
  audit's low-priority raw-type/logging clusters remain tracked port work. Active code was not excluded
  to produce a passing result, and preserved original implementation/reference information was retained.
- Five invalid original RotaryCraft PNGs still require verified original artwork. This pass has not
  invented substitutes. Physical input, human visual quality and shader-pack compatibility are not
  implied by automated client checks.


### Extended client verification

The opt-in TestInstance fixture ran via `:TestInstance:runClient --init-script
build/dragonapi-audit-client.init.gradle` in the copied `build/dragonapi-client-audit` save. It registered
a temporary namespaced filesystem asset, awaited Minecraft.reloadResourcePacks, then read the exact
bytes through Minecraft's actual resource manager and verified sourcePackId=dragonapi/direct_assets.
It also verified current Chroma glow-wood mappings/axis support, real server jump press/release decoding,
text-particle extraction, player-model submission and a fresh biome PNG. Temporary assets were removed.
`../dragonapi-extended-client.log` contains DRAGONAPI_AUDIT_CLIENT_PASS and BUILD SUCCESSFUL (1m 57s).

This fixture remains inert without dragonapi.auditClient. The log still includes the five known invalid
original RotaryCraft textures, background Chroma 4x4 puzzle-generation timeout and Windows counter
lookup errors; passing these targeted assertions does not imply those unrelated findings are repaired.


### 2026-10-08 — First project-backlog implementation batch

N01: ITEMDROPPERREQUEST now requires a live, nonempty item in the requester's ServerLevel,
loaded chunk, bounded distance and NeoForge's actual entity-tracking list. This is an informational
read: tracking players need neither ownership nor a machine menu. Replies read the original
`dropper` identity from `Entity.getPersistentData()` rather than searching serialized entity root
NBT. Restored ReikaItemHelper getDropper/setDropper preserve the original UUID/player contract;
getDropperId also supports offline credited players. Malformed, noncanonical identities are ignored
on both server and client without changing the existing packet IDs or wire layout.

N02: ReikaRFHelper no longer searches arbitrary foreign integer fields or private storage objects.
Native SimpleEnergyHandler draining uses its public setter, preserving forced sapping and change
notification. Other handlers extract through committed transactions; impossible extraction results
roll back. Explicit, exact-type adapters support integrations with a deliberate forced-drain API
and must report the amount removed and notify their own dirty state. The unsided and six face
capabilities share one total budget and identity-based alias deduplication. Unknown restricted
foreign buffers still need a verified explicit adapter; reflective mutation is not a substitute.

N05: PlayerMap.get(String) resolves canonical UUID strings, including uppercase hex, to the UUID
key already used by the map. Null, malformed/shortened UUID aliases and absent identities return
null. Names are not silently converted into guessed identities.

New tests cover UUID lookup/removal, forced draining, long-backed foreign stores, unrelated energy
fields, committed/rolled-back extraction, explicit adapters and aliased capability budgets. The
native dropper GameTest checks actual tracking, persistent and offline identities, malformed values,
empty/removed entities and a stale tracker beyond the distance limit. Reactor turbine restoration
and original sprite recovery are detailed in ReactorCraft/PORTING.md and RotaryCraft/docs/asset-recovery.md.

Validation: 65 unit tests passed (DragonAPI 25, RotaryCraft 32, ReactorCraft 8), including the
new UUID, forced/transactional/adapter/alias-budget and native turbine mesh tests. All 12 required
`rotarycraft:dragonapi_*` dedicated GameTests passed. `build/backlog-first-validation.log` records
BUILD SUCCESSFUL in 1m 45s with the active family compiled on Minecraft 26.3 / NeoForge 26.3.0.51-beta.

The disposable native client passed in 2m 5s (`build/backlog-first-client.log`):
TURBINE_AUDIT_CLIENT_PASS verifies registered normal stages 0–4/high-pressure stages 0–6, wear,
formed flywheel/six-face caps, original texture decoding, per-type animation rates and callback
correctness after the same state object is re-extracted. DROPPER_AUDIT_CLIENT_PASS verifies an
actual client request/server reply/client decode for a tracked item credited to an offline UUID.
Malformed client UUID aliases do not overwrite valid metadata. DRAGONAPI_AUDIT_CLIENT_PASS also
retains the existing resource reload/key/text/player-render/biome-PNG checks. Recovered target,
base railgun and molten-HSLA sprites decode natively; their earlier bad-PNG diagnostics are gone.

The first client attempt stalled at the fixture's initial screen gate after resource reload; the
opt-in fixture now disables focus-loss pausing and closes its initial vanilla PauseScreen. This
handling affects only audit runs. The final passing run retains the known two unrecovered Rotary
asset errors, other missing-model texture warnings, Chroma 4x4 puzzle timeout and Windows counter
errors. Automated native geometry checks do not claim human visual or Iris/shader acceptance.

Final ReactorCraft regression rerun after the per-type animation-rate changes: all 8 tests passed
(`build/backlog-turbine-final-tests.log`, BUILD SUCCESSFUL in 20s).

The maintained full checklist is `../PROJECT-BACKLOG.md`; N01/N02/N04/N05 are checked off,
N03 remains partial, and N06/N07/N08 plus the wider dependency-ordered port work remain open.


## 2026-10-08 — Second backlog batch: original assets and RayBlend completion

Resolved N03's remaining source provenance: tungsten-alloy material is the original COMPACTS
metadata 12, type 6 on sheet 0, hence items.png cell 108. The recovery script checks both original
atlas hashes and verifies four sprites as exact 16x16 RGBA pixels. The zero-byte advancedgear PNG
is preserved under RotaryCraft/reference/legacy-assets/, outside the atlas scan; worm gear, CVT,
coil, high gear and creative coil retain generated special item models and original 3D geometry.
The material's misleading original springtungsten name had caused a second mismatch: it was
registered as a charged ItemCoil. Its unchanged ID now uses the original stackable crafting-material
behavior. Full railgun sprite-group/projectile porting is tracked separately as RC19.

N06 baseline: the native layout test reported 9 omitted rooms out of 240 planned rooms but passed.
It now requires every authored profile, along with connected, correctly sized, nonuniform regions,
original compactness caps, valid sudoku colours, cages, starting crystals and room/door geometry.
Failed region attempts previously left cells removed from the remaining pool, and rejected full
shapes were retried unchanged. New attempts return all owned cells and slots before reseeding;
frontier weighting measures the region plus its candidate instead of the frontier itself.
Insertion-ordered point collections avoid identity tie-breaking in colliding hash-map tree bins.
Work is bounded at forty room attempts, eighty region attempts and eighty start candidates.
Exhaustion throws for a new plan; it never admits a shortened authored progression.

Layout version 1 is persisted in structure pieces and floor save/client tags, and participates in
the plan cache key. Missing version fields mean legacy version 0. Existing floors and chunk-clipped
pieces replay the old algorithm, including any rooms that were historically omitted. A recalculated
live generator resolves the version of matching saved floors for solving/force-opening. Unsupported
future versions are rejected rather than silently reinterpreted. The committed pre-fix implementation
(ChromatiCraft 6e3638aaea2f82583d180548787bfcd91c9399a4) independently produced twelve golden plans;
all matched legacy replay before those hashes were retained in the permanent regression suite.

Validation: all 240 authored rooms in the original 36-layout seed matrix generate reproducibly;
128 additional widely spaced seeds (including Long.MIN_VALUE/Long.MAX_VALUE) also complete and
match their twins. All four native rayblend_* tests pass, including actual old/new piece and floor
persistence, client-tag replay and existing puzzle-solving/reward behavior. The permanent unit
suite passes 69 tests: DragonAPI 25, RotaryCraft 33, ReactorCraft 8, ChromatiCraft 3.
Logs: build/backlog-rayblend-baseline.log, build/backlog-rayblend-ordered.log,
build/backlog-rayblend-compatibility.log and build/backlog-second-units.log. The legacy compatibility
fixture intentionally replays an old omitted 4x4 room; its historical diagnostic does not indicate
failure of version 1. The independent comparison source/fixture was temporary under build/ and is
not included in production sources or release jars.


Final native client validation passed in 2m 1s (build/backlog-second-client.log).
GEAR_ITEM_AUDIT_CLIENT_PASS resolves actual generated native item models and verifies original
3D geometry for worm gear, CVT, coil, high gear and creative coil. It verifies the tungsten
material's real reloaded components (stack size 64, no damage/charge) and absence of the empty
advancedgear resource. The original four recovered sprites decode natively. TURBINE,
DROPPER and DRAGONAPI_AUDIT_CLIENT_PASS retain the first batch's regression coverage.
No corrupt-PNG error or new 4x4 generation failure appears in this client run. A full decode scan
passes all 2179 RotaryCraft PNGs. Current RotaryCraft/ChromatiCraft jars contain neither the
archived obsolete PNG nor temporary reference/test classes. Other missing-texture warnings and
Windows OSHI/Perflib diagnostics remain; this does not claim actual Iris/Distant Horizons/shader
acceptance. N03/N06 are closed in PROJECT-BACKLOG.md; N07/N08 and RC19 remain open.

Git diff whitespace checks pass for the changed source and ledgers. The user's IDE-maintained
.idea/workspace.xml still has its pre-existing/dynamically updated whitespace; it was not edited.
This batch and the earlier first-batch changes remain uncommitted and unpushed.


## 2026-10-08 — RC19 shared recipe-display regression and integration

The full RotaryCraft railgun port exposed a current shared recipe display defect:
ReikaRecipeHelper selected an ingredient-slot index inside each alternative array, collapsed
empty shaped cells and lost component-bearing ingredient displays; cooking used the output as
its input. The helper now retains shaped Optional cells and all alternatives, resolves native
component-bearing slot displays, and falls back to already-resolved tag holders without requiring
an integrated server on a remote client. Cooking displays use the actual ingredient. The railgun
handbook reads the synced native recipe catalogue instead of its old empty list stub.

Native regressions check all fifteen upgrade centers, typed and legacy tiers, empty cells,
shapeless book ingredients and raw-iron cooking input. RC19's full suite passes 60/60 tests
(build/railgun-gametest.log). Final family unit tests pass 69/69 including all 25 DragonAPI tests;
the disposable native client resolves all sixteen actual railgun recipes and their prior-tier
component-bearing inputs (build/railgun-final-validation.log, BUILD SUCCESSFUL in 1m 50s).
Native projectile tracking, models and deferred rendering also pass. Datagen and packaged helper,
railgun classes and assets pass verification; see build/railgun-artifact-verification.json and
RotaryCraft/docs/railgun-port.md. This closes the display defect exercised by RC19, not every
historical recipe-helper API or project-wide remote-client acceptance item. Actual remote-client
reload/reconnect and Iris/Distant Horizons/shader validation remain open. Changes are uncommitted
and unpushed, together with the preceding backlog batches.


## Lua enchantment export — D18, 2026-10-09

ItemStackLuaBlock exports the item registry identifier rather than a translation key. Applied
and stored enchantments are exported separately under their native component names, with sorted
registry identifiers and exact levels (including explicitly empty component maps). Custom data
remains separate and the source stack is never changed. This repairs the missing enchantment
information; it is not a claim that every unrelated item component now has a human Lua adapter.

NBTLuaBlock derives readable enchantment details from old numeric IDs, namespaced list IDs,
modern component maps and historical component maps nested under levels. Its explicit
HolderLookup.Provider overload resolves actual registry names on either server or remote client;
without a provider it prints canonical identities and levels. Numeric vanilla IDs use the
existing source-backed 1.7.10 mapping. Unknown old mod IDs are identified as unknown and retained,
never assigned a guessed modern identity. Applied and stored lists are both handled; malformed
entries and every original field remain available. Parsing disabled means no derived display.

Written map/list containers now carry an explicit type: empty maps/lists, singleton lists,
mixed primitive/record lists and their ordering survive object export. Written lists also emit
anonymous record entries in their original order in Lua text, instead of sorting numeric keys
lexically or moving all records after scalar values. Existing parsed-script inference is retained.

Five focused unit regressions pass. Native integration checks actual registered sword components,
both enchantment kinds, registry ID/count/custom data, source conservation and original numeric-ID
name resolution with an explicit level registry. All 13 DragonAPI native tests pass in
build/lua-enchantment-native.log (46s); the module-family unit results total 74 (30 DragonAPI,
33 RotaryCraft, 8 ReactorCraft, 3 ChromatiCraft). Initial family run:
build/lua-enchantment-units.log; final DragonAPI/Reactor rerun: build/lua-enchantment-native.log.

Final D18 unit/jar verification also passes in build/proxima-water-client-final.log; final family total 74/74. Final client water audit passes independently; see the 2026-10-09 ReactorCraft ledger and build/proxima-water-final-verification.json.

## ControlledConfig format resets — D19, 2026-10-09

Historical source at cc8a1c0b supplied the expected format ID in the constructor and checked its
Control property before loading values. Later original source disabled that contract. The port's
remaining reset implementation wrote an empty report and deleted the config even if preservation
failed. The API now supports an explicit fourth constructor argument for the legacy format ID;
existing unversioned callers are retained without assigning them a guessed policy.

Initialization/reload checks the raw marker before validation, orphan cleanup or saving. Missing
and matching markers retain custom values; malformed markers abort without rewriting the file.
A mismatch preserves every on-disk byte in a unique sibling .reset-backup-UUID file before
clearing parsed primary data and restoring original additional-option defaults. Existing backups
are never replaced, and a failed copy aborts before mutation. Explicit protected resets regenerate
the legacy config without recursive init or duplicate spec registration. The first .pre-toml
backup is retained; non-file sources/backups fail before parser normalization. Control markers
survive cleanup even when ordinary options share that category.

Loaded ModConfigSpecs/TOML values remain authoritative. No TOML reset marker is invented, no
registered spec is replaced and no TOML file is edited by the legacy reset. Segmented values
retain their original separate-file authority: fixed the second primary read which previously
overwrote them with defaults. Their normal legacy normalization/save behavior is retained.

Seven focused regressions pass against the real legacy parser and loaded ModConfigSpec/TOML data.
Family suites total 81/81 (37 DragonAPI, 33 RotaryCraft, 8 ReactorCraft, 3 ChromatiCraft); all
13 native DragonAPI GameTests pass in build/config-reset-validation.log (1m 45s). The final
Control-category edge case passes the full DragonAPI unit suite/jar in build/config-reset-final.log
(20s). Full source contract and boundaries: docs/controlled-config-migration.md. Remote-client
reload/reconnect remains N07. Work remains uncommitted and unpushed.

## Block-array API restoration — D20, 2026-10-09

Restored the deferred array rotation, material tally/filter, blueprint import and selected-NBT
block-entity surface against de1bcf1f and current 26.3 sources. Coordinate-only rotations retain
original state/check orientation; every position-indexed layer, including placement overrides,
moves without reading destination blocks. Exact integer pivots, order, recalculated bounds,
independent alternative containers, clear/copy/replacement/merge consistency and world-free
declared layouts are covered. Material counts preserve actual components without changing
display stacks, skip empty/unitemized/flowing block items and retain first-alternative semantics.

Blueprint import supports all horizontal directions and retains anti-mapping constraints. The
original import/checker reference conventions remain; rectangular checker indexing and invalid
rows/directions are corrected without transposing existing definitions. Native entity checks
capture copied selected fields, require exact state/class/field equality, preserve weak prototype
references and overlay selected data through TagValueInput/loadWithComponents without clearing
unselected inventory or relocating entities. Native source/flowing fluid family policies,
standalone-fluid constraints and soft-space allowances are restored too. Full contract:
docs/block-array-port.md.

Four new unit regressions and five new GameTests pass. Final totals: 85/85 family unit tests
(41 + 33 + 8 + 3), all 18 native DragonAPI tests, and the isolated client audit with existing
water/turbine/gear/railgun/dropper/resource/tracking checks. Logs: build/block-array-validation.log
(1m 50s) and build/block-array-client-final.log (1m 41s). Normal Play launch arguments remain
unchanged. D20 and previous pending repairs are included in the requested local checkpoint;
remote-client and actual shader/LOD acceptance remain separate open tasks.

## Recursive breaker fluid identity and shared server sounds — D21, 2026-10-09

Confirmed findings 1.12/1.13 against the original breaker and actual 26.3 Level/ServerLevel sources.
Property collections are not fluid identity; playLocalSound still has an empty server base method.
The breaker now uses the captured actual state, standalone fluid identity and water tags, retaining
source/flowing water versus other-liquid sounds and treating waterlogged solids as solids. Native
server broadcast reaches the initiating player and nearby observers, preserves state-aware break
sound parameters and respects native range/dimension filtering and NeoForge sound hooks.

Accepted caller review also confirmed the shared sound helpers' server no-op, including finding
1.14. Block break/step/place, explicit block, normal ambient and entity overloads now retain local
client playback and use native server broadcasts. Removed/replacement block fallback, positions,
categories, explicit/default volume/pitch and local distance-delay semantics are retained.

Nine new native tests, covering fourteen shared helper overloads and actual player connections,
pass alongside all previous DragonAPI tests: 27/27. Family unit suites remain 85/85. Log:
build/recursive-breaker-validation.log (1m 7s). All fifteen accepted wand tests and the cliff
transparification test pass in build/recursive-breaker-wand-callers.log (2m 16s) and
build/recursive-breaker-cliff-caller.log (39s). The isolated client audit passes in
build/recursive-breaker-client.log (1m 42s), including local/decoded-server sound-engine checks
and all previous water/turbine/gear/railgun/dropper checks. Normal Play argument hashes remain
unchanged. Full source/caller contract: docs/recursive-breaker-port.md. This completed batch is
included in the local checkpoint; D22 and remote-client/shader acceptance remain open.

## Original special-tree scan limits — D22, 2026-10-09

Resolved finding 1.29: ModWoodList exists and compiles, so its four original depth rules must not
remain commented out. TreeType now supplies the original thirty-wave default; ModWoodList
overrides exactly Sequoia 350, Twilight Oak 200, Darkwood 32 and Giant Pink Tree 180. The tree
factory consumes that policy while retaining its original per-tree spatial box, native log/leaf
state matching, diagonal spread and explicit enqueueing. Similarly named Natura Darkwood,
BOP Redwood and Pink Birch retain the ordinary limit. Original boxes remain unchanged.

Three unit regressions cover every catalogue limit, inherited defaults and the four original
boxes. Six native regressions exercise exact 30/32/180/200/350-wave boundaries, state variants,
other species, one-shot completion and real supported ChromatiCraft glow-wood bounds/corner leaves.
External optional forests are not installed or impersonated: wave fixtures delegate real catalogue
policies through registered vanilla-state descriptors, while source boxes are verified separately.
Full contract and validation boundary: docs/tree-breaker-port.md.

All 88 family unit tests and all 33 native DragonAPI tests pass in build/tree-depth-validation.log
(1m 38s); all accepted consumers compile. Normal Play argument hashes match the previous checkpoint
and automated checks remain disabled there. This completed batch is included in the local checkpoint.
D23 is next; D36 wood associations and actual remote-client/shader acceptance remain open.

## Live common-material flammability — D23, 2026-10-09

Resolved finding 1.18 against the original de1bcf1f catalogue and actual 26.3 FireBlock/NeoForge
sources. The block-only catalogue lacked common wood/leaves/wool/slabs. The world helper already
consulted the contextual hook, but passed a dry default state, defeating waterlogging. It now
uses the actual state through both hook and fallback, retaining original UP-face and chest/trapdoor
exceptions. The catalogue queries native fire data live, covering modern species, doubled wooden
slabs and every wool colour/shape while respecting fireproof Nether wood. Public initialization
remains functional. Note block and jukebox retain their original classifications through the
generated reloadable dragonapi:legacy_flammable tag; native fire spread odds are unchanged.

Three new units and seven native fixtures cover common/state-sensitive catalogue data, generated
exceptions, native tag-reload apply/restoration, temperature ignition, the public laser beam and
ReactorCraft graphite/plasma. Tag tests validate rebinding rather than claiming an entire resource
pack reload. All 91 family units (47 + 33 + 8 + 3), 38 native DragonAPI tests and two ReactorCraft
consumer tests pass. Datagen and packaged-tag inspection pass; normal Play argument hashes match
the previous checkpoint and automated checks remain disabled. Logs: build/flammability-datagen.log,
build/flammability-validation.log (3m 16s), build/flammability-reactor-validation.log (38s).
Full contract: docs/block-flammability-port.md. This batch is included in the local checkpoint.
D24 dye-cache identity/reloads and actual remote-client/shader acceptance remain open.


## Dye cache and resistor interaction — D24, 2026-10-09

Finding 1.43 is resolved against DragonAPI de1bcf1f and ElectriCraft V31a 950badc.
ReikaDyeHelper now maps all sixteen native colour tags to immutable item-identity lists,
reads the actual typed DYE component without caching stack state, preserves multi-colour
membership and original metadata/RGB/name contracts, and rebuilds on the root 26.3 tag
event with the integrated-client static-data guard. Manual initialization is functional.

The accepted ElectriCraft resistor branch is restored through native held-item interaction,
with original band/facing/flip geometry, lime-to-GREEN mapping, rejected multiplier colours,
server-only mutation, survival/creative/offhand handling, current recalculation and sync.
The same obsolete method's motor upgrades retain 2/4/1000 A limits and reject duplicate or
lower tiers; the existing transformer menu remains reachable. No authored recipes/data change.

Four new units and six native tests pass: 51 DragonAPI + 33 RotaryCraft + 8 ReactorCraft
units and all 19 ElectriCraft native tests (build/dye-cache-validation.log, 40s). Reload
fixtures apply real tag bindings, exercise the server dye callback and registered client
event listener, restore originals in finally, and cover 56 resistor orientation/band cases.
They do not claim a full resource-pack reload, real remote connection or broad electricity
network acceptance. Packaged classes and normal Play's audit-off/no-quickplay snapshot run
are verified in build/dye-cache-final-verification.json. Full contract: docs/dye-cache-port.md.
D24 is included in the local checkpoint; D25 MIDI consumers are next.


## MIDI import and music consumers — D25, 2026-10-09

Finding 3.39 and the commented reader methods/raw MIDI collections are resolved against
DragonAPI de1bcf1f and RotaryCraft b2288638. Restored legacy track/grid queries retain their
original contracts. Score/array imports share a chronological tempo timeline, retain
channel programs and velocities, pair overlapping and velocity-zero releases, support
SMPTE frame timing and safely handle short/invalid tempo metadata. Original 5 ms units,
GM instrument/pitch mapping and consumer scaling remain explicit and unchanged.

MusicScore writes chords into their channel compounds and reads actual native compound
lists, derives caches from notes, preserves independent copies, updates edit/transpose
metadata and merges grid-aligned chords. RotaryCraft's disk consumer again restores original
notes/rest offsets, MIDI import clears previous music and all 49 piano IDs reach GuiMusic.
The six original instrument choices initialize with the restored plank icon.

Seventeen new units pass with all 109 affected-module units (67 DragonAPI, 34 RotaryCraft,
8 ReactorCraft; build/midi-validation.log, 1m 4s). Native MusicClientChecks verifies every
key, black precedence, non-left/outside rejection, original texture and highlighted GUI
geometry through the actual client. The full separate audit passes in 3m 24s, including
existing GPU/water/input/resource/model checks (build/midi-client-validation.log).
The callback fixture records IDs before packet sending; audible playback/music delivery,
remote sessions and excluded Chroma music subsystems are not claimed. Packaged classes
and normal Play's unchanged argument hashes are verified. Contract: docs/midi-score-port.md.
D25 is locally checkpointed; D26 TileEntityCache subtype review is next.


## Typed tile caches and unresolved locations — D26/D27, 2026-10-10

Original DragonAPI de1bcf1f cache/location contracts are retained with explicit Class<V>
validation, class/interface support, atomic typed insertion and real NBT subtype filtering.
Untyped location/value maps remain usable; NBT resolution fails before clearing an untyped
cache. Reactor control rods, pebble beds and Chroma crystal-network/pylon caches use actual
tokens. Malformed/missing/wrong-subtype entries cannot escape as a delayed caller-side cast.

Chunk/move/copy identity works without a live level. WorldLocation retains no client Level,
uses actual state properties and safe unresolved/read sentinels, restores loaded-chunk and
sky queries, numeric/current dimension persistence and dimension-aware ordering/squares.
Original face-hit identity and randomized item drops/velocity scaling are restored.

Nine new units pass with all 118 affected-module units (76 DragonAPI, 34 RotaryCraft,
8 ReactorCraft). Four native fixtures pass, including the actual CPU reload consumer,
class/interface filtering, missing dimensions/chunks, state properties, face hits and drops.
Crystal-network compilation passes. Logs: build/cache-location-validation.log (1m 2s),
build/cache-location-native-final.log (40s). Contract: DragonAPI/docs/cache-location-port.md.
No arbitrary LocationEntry serializer, real remote client or full reactor-network acceptance
is claimed. Normal Play is separate. Completed work is locally checkpointed; D28/D29 are next.


## HexGrid size semantics and inverted weighted selection — D28/D29, 2026-10-10

HexGrid's suspected endpoint-size bug is not observable: lerp discards both size fields and
uses the explicit output size. Original cube-space semantics remain, now documented and
covered by mixed-size/reverse/zero-length/adjacency regressions. No physical-grid semantics
are invented; deferred Chroma lore rendering remains open.

InvertedWeightedRandom now uses RandomSource, handles empty and zero-mass pools, rejects
invalid/unrepresentable weights before mutation, selects half-open intervals correctly and
retains append-only duplicate results. Seed/RNG control and clear/reuse are functional.
Original and current sources have no inverted persistence API or project consumers; main
WeightedRandom NBT coverage is explicitly separate. No new persistence format is invented.

Seven new regressions and all 125 affected-module units pass (83 DragonAPI, 34 RotaryCraft,
8 ReactorCraft; build/hex-inverted-validation.log, 1m 4s). Contract and source evidence:
DragonAPI/docs/hex-inverted-review.md. Completed work is locally checkpointed; D30 is next.


## Inventory and resource empty contracts — D30, 2026-10-10

Legacy arrays and native inventories now handle null/EMPTY/zero-count slots consistently.
Transactional full-fit insertion rolls back failure; administrative output writes retain
actual per-item capacities; partial insertion conserves counts/components across slots.
Snapshot-backed Container merges/decrements persist. Closing/draining use EMPTY, spills
clear before spawning and cannot duplicate, and last-block selection retains its identity.
Item Cannon consumption, original capacity gate/sound, target persistence and bottom-only
extraction are restored through native inventories and capabilities. Resource Optional,
IoSupplier and IOException contracts were reviewed and already behave correctly.

Six new native fixtures and all 44 family-loaded DragonAPI cases pass, alongside 127 units
(85 DragonAPI, 34 RotaryCraft, 8 ReactorCraft; build/inventory-family-full-compile.log, 1m 14s).
RotaryCraft-only broad selection requires Chroma for the glow-tree case. A full compile
restored missing accepted Chroma class outputs before family validation. Cannon transfer
is isolated from power acquisition; complete powered-chain/GUI/audio acceptance remains
separate. Details: DragonAPI/docs/inventory-empty-port.md. D17 player-to-chest transfer
remains open; unique-stack counting does not implement it.


## Native multiblock entity contract — D32, 2026-10-10

Removed non-overriding hasTileEntity/createTileEntity hooks after reviewing all six accepted
ReactorCraft casing families. Only turbine ELEMENT hosts an injector; other casings retain
controller lookup without invented local entities. SteamInjector now validates the actual
ELEMENT state, allowing native same-block part changes to remove its obsolete entity and
lubricant capability. FORMED changes preserve the entity and tank, detached NBT retains
contents, and restoring ELEMENT creates fresh storage. Two native regressions and all 127
affected units pass (build/multiblock-contract-final.log, 45s). Native mismatch warning at
variant transition accompanies expected cleanup; full assembly/teardown/drop/hardness
acceptance is separate. Details: DragonAPI/docs/multiblock-entity-contract.md.

D31 remains open: scalar metadata cannot describe actual 26.3 state properties; the accepted
rift has a full-state getter but its shared interface still publishes a fake-zero metadata
contract. Migrate the exported contract and actual callers deliberately, without inventing
global integer state IDs or pretending a zero metadata value preserves variant behavior.


## Replacement states, registrations and accepted client outputs — D33/D52, 2026-10-10

Restored BlockReplaceOnBreak's original abstract replacement contract and explicit nested
self policy using complete native states. The 26.3 player-removal lifecycle retains the
original state/entity/tool for harvesting; no duplicate manual loot path is added. There
are no accepted concrete consumers, so concrete gameplay/drop acceptance accompanies a
future consumer rather than an invented production block. Details: replacement-on-break-port.md.

DragonAPIMod now resolves the real loader container with class/ID/lifecycle/registration
context in failures. One new native family fixture verifies all eight actual registered
IDs and constructed base-mod instances, plus missing-ID diagnostics. The full family
DragonAPI suite passes 45 cases; final affected units pass 127. Logs:
build/registration-family-validation.log and build/final-source-and-launch-validation.log.

An incomplete incremental ChromatiCraft output was reproduced despite successful compileJava.
Root build configuration now compiles its full accepted slice when needed and verifies
primary accepted class outputs before both client preparations. All eight guards pass
(3,251 outputs); a disposable missing-output fixture is rejected without deleting real
classes. Normal Play arguments retain their original SHA256 hashes. The isolated client
audit passes artefact brightness/animation, blue puzzle water, 24 adjacency GUI models
from the immutable snapshot, rendering, music, sound, input and resource checks:
build/night-client-validation.log. D33 was compiled afterward, not claimed as a concrete
client test. Details and acceptance limits: TestInstance/docs/overnight-2026-10-10.md.


## Registered inert entity identities — D54, 2026-10-10

Reviewed all direct and indirect InertEntity subclasses and the active source boundary.
Every accepted consumer already supplies its actual EntityType; the unused world-only
constructor assigning ARROW is removed. Excluded pristine legacy consumers remain intact
and must acquire their own typed registration when fully ported. No universal placeholder
entity type is introduced. Details: DragonAPI/docs/inert-entity-identity.md.

The new native family fixture creates ten accepted inert types from their real registered
factories and checks both runtime identity and native serialized ID. Full family DragonAPI
regressions pass 46 cases, all affected units pass 127, and accepted class outputs pass
across eight modules (build/inert-identity-final-validation.log). This is constructor/save
identity coverage; per-entity gameplay, rendering, payloads and full persistence behavior
retain their own acceptance requirements. The overnight handoff records nine completed
backlog items and remaining priorities; unrelated worldgen/IDE changes remain uncommitted.


## Player transfers and formed-casing mining — D17, 2026-10-11

Checked latest working progress: the full-state D31 rift port and installed DH/Chunky
startup repair have successful recorded validation, but remain separate pending changes.
The existing formed casing multiplier inverted the original hardness rule. Native mining
progress now divides by four; every real casing variant retains positive base hardness 2
and the native harvest formula. All three native multiblock regressions pass with bare
hands and an iron pickaxe (build/formed-casing-mining-validation.log).

D17 now implements the original array API's intended transfer operation; the original
body itself never transferred anything. All main/hotbar slots move only accepted amounts,
with component identity, stack limits and exact leftovers retained. Equipment/offhand
remain equipped. A native Container overload honors filters, resource-dependent capacity,
snapshot writeback and dirty notifications; self-targets change nothing and non-owning
threads fail before inventory access. The existing Inventory insertion overload remains.
Contracts: DragonAPI/docs/player-chest-transfer-port.md and formed-casing-mining-port.md.

All 48 family DragonAPI native tests and 135 affected units pass (87 DA + 34 RC + 8 ReC +
6 Chroma). Both strengthened transfer fixtures pass again after equipment/dirty coverage
improvements. Eight accepted-output guards verify 3,262 current primary outputs. Evidence:
build/player-chest-transfer-validation.log and player-chest-transfer-final-validation.log.
Normal Play argument hashes match; saves, installed mods and unrelated working changes
are preserved. No push. Full structure/drop, powered-cannon and remote/menu acceptance
remain separate; D35 is a different base-class contract. The next focused work is D53,
then D16. Checkpoint boundary: TestInstance/docs/continuation-2026-10-11.md.


## Native tick failure policy — D53, 2026-10-11

The shared BlockEntityBase lifecycle no longer swallows broad programming errors
and continues into synchronization, callbacks and machine logic after failed
animation/initialization. Failures reach the real 26.3 LevelChunk ticker: complete
crash report by default, or native entity removal when the owner's existing
NeoForge option enables it. Production code does not alter that option. Existing
caller-local handling remains; writeError logs the complete Throwable with type,
class, position, dimension and logical side before its optional chat display.
Contract and sources: DragonAPI/docs/block-entity-tick-failures.md.

All three new native fixtures pass, including the formerly swallowed five error
classes plus illegal state/linkage errors, both lifecycle hooks, both native
policies, no retry after removal, healthy replacement and explicit handled-error
logging/continuation. The strengthened family suite passes all 51 native tests;
all 135 affected units pass. All 151 registered-machine lifecycle cases pass under
the stricter policy. Eight output guards verify 3,263 accepted primary classes.
Evidence: build/tick-failure-focused-validation.log, tick-failure-family-validation.log
and tick-failure-machine-validation.log. Normal Play argument hashes match.

Only this batch is checkpointed. Other pending rift, worldgen, compatibility,
launcher and IDE work remains separate. Nothing is pushed. Next: D16's block-array
computation threading contract. Boundary: TestInstance/docs/tick-failure-continuation-2026-10-11.md.


## Block array computation and authorized fills — D16, 2026-10-11

Original and ported BlockArrayComputer branches were empty, with no real callers
beyond an unused private instance. Explicit inputs now define coordinate-NBT LOAD,
ordered snapshot ITERATE and full-state FILL using the existing array contracts.
BlockArray.getComputer captures current membership/bounds; no task retains or mutates
its source array. Copied NBT and immutable positions/results permit worker computation
with world-free callbacks. Missing inputs, concurrent execution/configuration and
callback failures report explicitly without a stale successful result.

FILL capture/execution require the owning server thread, loaded chunks, valid bounds,
expected full states and explicit caller actor/claim authorization. Every target is
preflighted before writing; denial, failed authorization and stale preflight change
nothing. Native UPDATE_ALL placement preserves neighbor/client updates and returns
actual changed counts. Explicit server submission works from workers. Later targets
changed by neighbor effects stop with partial-progress context; native world updates
are not transactional and no unsafe rollback is attempted. Contract/source boundary:
DragonAPI/docs/block-array-computer-port.md.

Coordinate NBT again writes original x/y/z, reads both original and affected-port
pos.getX()/pos.getY()/pos.getZ() keys, and derives loaded limits from accepted members
while retaining the persisted legacy limit tag. Six new unit regressions and five
native fixtures pass; the final family suite passes all 56 native tests and 141 units
(93 DA + 34 RC + 8 ReC + 6 Chroma). Eight guards verify 3,264 accepted primary outputs.
Evidence: build/block-array-computer-focused-validation.log and
block-array-computer-family-validation.log. Normal Play argument hashes match.

Only D16 code/tests/notes are checkpointed; pending rift, worldgen, compatibility,
launcher and IDE changes remain separate. Nothing is pushed. Boundary:
TestInstance/docs/block-array-computer-continuation-2026-10-11.md.


## Bounded callback flood-fill crash — 2026-10-11

The supplied server-tick report exhausted the stack in
BlockArray.recursiveAddCallbackWithBounds. Its overflow diagnostic then failed to
bootstrap string concatenation on that exhausted stack, producing a secondary
BootstrapMethodError. The truncated report does not establish the initiating item.
Both bounded callback APIs now share a constant-stack queue traversal, preserving
fixed-origin versus predecessor callbacks, inclusive depth and spread/distance
settings. Explicit/array/native bounds and optional loaded-chunk checks precede
callback reads. Independent visitation handles existing membership and later
admission after an edge rejection. Admission is deterministic breadth-first;
throwOverflow uses parameterized logging for remaining recursive search APIs.

The accepted Subterranean Route Finder depends on minimum height, then a separate
path search, so does not depend on the previous DFS insertion order. A real sneaking
tool fixture traverses a sealed 2,156-cell space and verifies actual start/deepest
trail placement and retained supporting stone. Three native search regressions
include a 20,000-cell line, limits, clamps, callback contracts and exception identity.
All seven selected item fixtures and all 59 DragonAPI native fixtures pass; all
141 units pass (93 DA + 34 RC + 8 ReC + 6 Chroma). Eight accepted-class guards verify
3,264 primary outputs. Logs: build/callback-search-focused-validation.log,
cave-exit-deep-validation.log and callback-search-family-validation.log.

Other recursive block/fluid/spherical APIs and incremental scheduling remain for
separate work. Large connected spaces can still be expensive within one tick.
Contract: DragonAPI/docs/bounded-callback-search-crash.md. Evidence/checkpoint scope:
TestInstance/docs/callback-search-crash-2026-10-11.md. Only this fix, fixtures and
notes are checkpointed; other pending changes are retained. Normal Play argument
hashes match. No live-client or optional-mod claim is made. Nothing is pushed.


## Shared machine-block mining/comparator/menu contract — D35, 2026-10-11

Original BlockTEBase/TileEntityBase source contracts restore contextual no_mine
protection through native mining progress and shared SemiUnbreakable queries.
The saved key remains; initial/delta sync carry it. Changed attached-server flags
require the owning thread and dispatch one native delta, while unchanged setters
dispatch nothing. Native saved loads retain protection; clearing restores the exact
ordinary/configured mining result. The pending Rotary PIPEHARDNESS implementation
is incorporated with native zero/nonzero/live/clamp checks and a protection guard
before instant mining. This supersedes the earlier compile-only pipe validation.

getComparatorOverride again exposes the existing eight-tick server sample. Native
shared block outputs clamp it to 0..15 on every face, with zero for absent tiles.
Changed samples notify ordinary neighbors and native comparators behind a solid
conductor, replacing a duplicate ordinary notification. Existing specialized block
overrides remain authoritative; runtime caches are recomputed after load.

Rotary empty-hand interaction now reaches the common original machine actions/menu
logic with EMPTY, preserving concrete handlers and access/crouch checks. Passive
failure becomes PASS. Native Blast Furnace opening delivers current inventory,
progress and temperature, and subsequent data updates reach the same connection.
NeoForge full-width DataSlot payloads and vanilla packets are observed; menu opening
and passive pipe clicks do not broadcast redundant full tile NBT. Existing menu
slots/data, periodic IO/Fractionator tags and explicit Reactor CPU view payloads
make the removed blanket click sync unnecessary for this shared contract.

All six focused native fixtures and the final 65-test DragonAPI native suite pass;
all 141 units pass (93 DA + 34 RC + 8 ReC + 6 Chroma). Four mod-family consumers,
actual comparator transitions, saved flags, native flag packets, worker rejection,
live pipe settings and real menu connection updates are exercised. Eight guards
verify 3,265 accepted outputs; DA/RC production jars build. Logs:
build/machine-block-focused-validation.log and machine-block-family-validation.log.
Contract/source pin: DragonAPI/docs/machine-block-contract-port.md. Full evidence:
TestInstance/docs/machine-block-contract-2026-10-11.md. Normal Play hashes match.
Native headless mocks are not authenticated remote-client or graphical acceptance.
This batch and the related pending pipe implementation are checkpointed; other
rift/worldgen/compatibility/launcher/IDE changes remain separate. Nothing is pushed.
