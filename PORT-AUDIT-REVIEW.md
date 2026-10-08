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
