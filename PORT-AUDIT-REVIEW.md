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


## Tiered-resource harvesting/loot contract — D34, 2026-10-11

The missing shared player-removal hook is fully restored. The original false
canHarvestBlock is intentional because this hook owns harvesting independently
of tool suitability and must not also run native playerDestroy. Real drops are
chosen before removal, using NeoForge's pre-damage tool snapshot. Successful
server survival removal dispatches one modern BlockDropsEvent with the original
state, owner, tool, mutable items and XP. Cancellation/replacement are honored.
Creative and unowned generic drops remain empty; sufficient-tier resources retain
the original 2–6 XP, including plants whose standalone modern port had lost it.

All 15 accepted Chroma ores and seven plants now inherit the common contract.
Source count/stage/disguise/tuning/Elemental Stone roller rules remain. Generated
reloadable loot tables use a native dynamic entry instead of empty tables plus
unconditional manual drops. Native plant Fortune/Looting survives final-use tool
breakage. Native BlockItem placement is now gated for plants as well as ores, with
server break effects/removal; creative inspection/building and current host-drop
disguises remain. The latter deliberately retains the accepted port's improvement
over upstream's final tier-only drop-spawn check. Actual recursive miners and
world/Silk Touch helpers recognize these registered resources again; empty
sufficient-tier Silk Touch collections are handled safely.

Six new native fixtures, the final 11-test Chroma tiered suite and all 16 survival
fixtures pass. The survival selection also exercises eight pending overworld
fixtures in the working tree, without checkpointing those unrelated sources.
Together with 65 passing DragonAPI native fixtures these give 91 distinct IDs.
All 141 units pass (93 DA + 34 RC + 8 ReC + 6 Chroma); eight class guards verify
3,266 accepted outputs. Server datagen writes 22 loot tables with matching cache
hashes; DA/Chroma production jars build. Normal Play argument hashes are unchanged.
Source contract: DragonAPI/docs/tiered-resource-contract-port.md. Full evidence:
TestInstance/docs/tiered-resource-contract-2026-10-11.md. Validation is native
headless server behavior, not graphical or authenticated remote-client acceptance.
Only this batch's changes are checkpointed; other rift/worldgen/compatibility/
launcher/IDE work remains separate. Nothing is pushed.


## Wood/ore state and item associations — D36, 2026-10-11

WoodSpecies and ModWoodList now provide complete explicit log/leaf/sapling state
sets, native BLOCK_STATE species items, preserved supplied item components and
independent inventory snapshots. Log offsets index actual states; invalid offsets
cannot alias the preferred log. Registration validates conflicts before replacing
immutable reverse maps. Block-only lookup cannot silently choose between species.
The accepted glow tree resolves after deferred registration and retains its authored
never-decaying leaves. Natural-leaf queries require species membership and respect
native persistence for ordinary trees. Shared legacy field names require verified
associations from their modern external ports; no optional mods or IDs are invented.

TreeType defaults preserve existing providers; recursive breaking consumes exact
declared state sets, leaving other variants of the same block. Original depths,
spatial boxes and diagonal spread remain. Reactor radiation now passes the actual
leaf state. Ore state APIs retain common-tag membership, returned stacks are detached,
and native RNG can be supplied. The historical ore damage argument is a saved ordinal:
getByLegacyOrdinal names it; the deprecated old method preserves ordering and bounds.

Five new unit regressions and four new native catalogue fixtures pass. The full
DragonAPI suite passes 69 native tests; family units pass 146 (98/34/8/6). Eight guards
verify 3,268 accepted outputs, and DA/RC/ReC production jars build. The final focused
run verifies all 28 glow-leaf states against actual loaded sapling loot at the original
guaranteed Fortune-X threshold, component-bearing shared variants and supplied items.
Reactor's small caller adaptation is compile-validated, not a new radiation fixture.
Normal Play argument hashes match; headless checks do not establish visual/remote-client
acceptance. Logs: build/catalogue-family-validation.log,
catalogue-final-focused-validation.log and catalogue-radiation-caller-compile.log.
Contract: DragonAPI/docs/wood-ore-catalogue-port.md. Evidence:
TestInstance/docs/wood-ore-catalogue-2026-10-11.md. D37 plank provenance and D38 ore
product/drop/multiple-mapping/reload work remain separate. Other pending rift,
worldgen, compatibility, launcher and IDE changes remain outside this checkpoint.
Nothing is pushed.


## Complete ore/product catalogue — D38, 2026-10-11

ModOreList.getByDrop now recognizes the original declared product tags and retains
its ore-block fallback. Immutable generations preserve every overlapping ore,
product and source/native-name association; singular adapters consistently choose
original enum order. Returned stacks are detached and classification follows item
tag identity regardless of display components or stack count. Product lookup is
the source's declared-product contract, not an invented exhaustive loot/recipe map.

Full, individual and alias-triggered initialization replace the complete generation,
removing stale associations while preserving held immutable views. Alias validation
precedes mutation; synchronized publication and rollback retain a complete catalogue.
Compound common tags, previous flat spellings, unusual original Monazit/Forcicium
names, Certus gem/crystal tags and historical lapis products are retained. Native
TagKey registration and plural lookup APIs are available. The source CHARGEDCERTUS
option regains its original false default; enabled policy is read at rebuild time.

The root TagsUpdatedEvent listener consumes authoritative server/remote item tags
and skips shared integrated-client notifications. Minecraft 26.3 posts server tag
updates before default item components bind, so snapshots hold Item identities and
build stacks only when queried. No incomplete prototypes are created during binding.

Five new units and two native fixtures pass. Final family validation passes 151
units (103/34/8/6), all 71 required native DragonAPI fixtures, eight accepted-class
guards covering 3,271 primary outputs, and DA/RC production jars. Normal Play
argument hashes match the preceding checkpoint. Native reload fixtures restore all
tag bindings; headless event handling does not establish live remote/visual acceptance
or optional-mod parity. Contract: DragonAPI/docs/ore-product-catalogue-port.md.
Evidence: TestInstance/docs/ore-product-catalogue-2026-10-11.md and
build/ore-catalogue-family-validation.log. D37 plank provenance and D39 keyed-stack
matching remain separate. Pending worldgen, rift, startup compatibility, IDE and
launcher changes remain outside this local checkpoint. Nothing is pushed.


## Keyed-stack component matching — D39, 2026-10-11

Reconciled KeyedItemStack against the original metadata/subtype and NBT rules.
Item/Block constructors regain wildcard variants; ordinary wear is ignored by
default and explicitly selectable. Variant and additional native component maps
are independent immutable projections. Potion base identity and custom fields
are separated; declared family subtypes retain their real canonical defaults.
Rotary coil/charged-tool item-owned adapters preserve their original charge-as-
subtype behavior, while Reactor's non-subtype charged tools retain source policy.

Strict collection equality/order remains separate from explicit fuzzy predicates.
Locks, detached copies, exact/count behavior and registry-aware saves are retained;
old records default to ignored wear. Key construction migrates a private copy.
Railgun ammo joins the existing legacy load migration: typed tier wins and obsolete
DAMAGE is removed. ItemMatch derives extra-data policy after excluding subtype/wear
patches. The actual Chroma tool crate regains independent subtype and extra-data
flags, verified through its production counting path for swords and potions.

Final validation passes 156 units (108/34/8/6), all 74 required native DragonAPI
tests, all 60 required railgun regressions, eight accepted-class guards covering
3,274 primary outputs, and DA/RC/ReC/Chroma jars. Normal Play argument hashes match
the preceding checkpoint. Initial unbound-stack unit cases were moved into native
server fixtures; pure units do not rebind registries. Headless checks do not claim
interactive/remote-client or absent optional-mod acceptance.

Source pins and matching contract: DragonAPI/docs/keyed-stack-component-matching.md.
Evidence: TestInstance/docs/keyed-stack-matching-2026-10-11.md and
build/keyed-stack-family-validation.log / keyed-stack-railgun-validation.log.
D40 typed-map accumulation and D41 excluded fuzzy-set consumers remain for full
subsystem ports; D37 still needs authentic plank provenance. Earlier pending
worldgen, rift, compatibility, launcher, IDE and D31 work remains outside this
local checkpoint. Nothing is pushed.


## Packet pipeline — D43 and Chroma caller follow-up, 2026-10-12

Completed PacketPipeline's native clientbound/serverbound packet construction,
namespaced connection-scoped replies and functional custom codec registration.
Each pipeline and helper share one actual bridge. Incoming handlers bind only
their received native context; nested handlers/failures restore or clear that
scope. Explicit flow/context overloads cover native callers. Custom encoders and
decoders register as bounded native PLAY payloads, use authoritative handlers,
reject duplicate/foreign/late registrations and preserve existing channel IDs,
protocol 2.1.0, legacy types and default byte framing. Original getMinecraftPacket
was active; its original reply implementation was commented out, so this also
completes the previously exposed intended reply API rather than inventing a
claimed original gameplay regression.

Eight new units and four new native fixtures cover default bytes, both directions,
codec execution, malformed/foreign rejection, nesting, exceptions, namespace
isolation and actual NeoForge registration/native stream-codec round trips.
Final checks pass all 78 DragonAPI native tests, eight crafter regressions, 164
units (116/34/8/6), eight class guards covering 3,280 primary outputs and DA/RC jars.
Normal Play hashes remain unchanged and its namespace property does not enable
the new fixture channels. Controlled receive contexts capture replies in tests;
actual remote socket delivery is not claimed. Existing deliberate tick-failure
crash reports remain expected fixture diagnostics, not failing suites.

D41 review corrects the earlier broad FabricationRecipes description: useNBT=false
stores ignore-extra-data keys while query methods construct strict keys. The full
fabrication subsystem port must use explicit recipe matching and preserve useNBT.
Its exported heterogeneous union is iterated by existing NEI callers. Current
ItemMagicRegistry keyed calls pass its own exported keys; stack overload callers
use the registry's declared policy. Pristine excluded classes are retained.
D40/D41 remain open for their full subsystem ports. D44 dormant transport,
D45 tank sender semantics, D46 legacy sub-IDs and remote acceptance remain open.

Contract: DragonAPI/docs/packet-pipeline-port.md.
Evidence: TestInstance/docs/packet-pipeline-2026-10-12.md and root build/
packet-pipeline-{final-validation,final-units,crafter-validation}.log.
The Chroma caller correction is in ChromatiCraft/docs/chroma-crafter-port.md.
Earlier pending worldgen, rift, compatibility, launcher, IDE and D31 work is
preserved outside this selective local checkpoint. Nothing is pushed.


## Native compound batching — D44, 2026-10-12

Completed the selected migration through the existing channel bridge. The
unregistered DragonPayloads early design has no repository senders/registrations;
its incomplete handlers and the former compound code are preserved byte-for-byte
as committed-source references under DragonAPI/docs/reference, outside runtime
source/resource roots. No registered content was excluded to obtain a green build.

CompoundSyncPacket is now a server queue/snapshot API which emits the existing
BE_NBT_SYNC payloads in native PacketDistributor bundles, at most 256 per bundle.
No extra transport/payload registration or wire change is introduced. Channel IDs,
protocol 2.1.0, default frames and initial vanilla full sync are retained. The
COMPOUNDSYNC option is appended without shifting old ordinals, defaults false and
wires the real BlockEntityBase periodic branch. Mode transitions carry baseline/
removal history, force authoritative state and cancel stale queued updates.

Queued tags are detached and compared to the consumed baseline, preserving
coalesced changes/removals/reversions. Failed sends retain pending work; newer
reentrant revisions survive flush. Real tile identities, weak references, loaded
chunks, dimensions, ranges and negotiated channels govern delivery. Per-dimension
4/20-tick clocks avoid duplicate callbacks; unload/stop clear state. Snapshot codecs
preserve signed int coordinates and registry identities, bound counts/sizes and
retain prior state/buffer indices after malformed reads or failed writes. Existing
compound receiver implementations retain a detached adapter. The casting table's
explicit empty output remains, with obsolete removal commentary corrected.

Validation passes all 84 DragonAPI native tests, eight crafter regressions, 170
units (122/34/8/6), final six compound fixtures, eight guards covering 3,280 primary
outputs and DA/RC development jars. Retired classes/reference files are absent
from runtime jars; archived bytes and normal Play argument hashes are verified.
Controlled transport captures are not actual remote socket/render acceptance.

Contract: DragonAPI/docs/compound-sync-port.md.
Evidence: TestInstance/docs/compound-sync-2026-10-12.md and root build/
compound-sync-{family-validation,final-validation,crafter-validation}.log.
D44 is complete. D45 tank sender semantics, D46 legacy sub-IDs, excluded Chroma
subsystem ports and release/remote acceptance remain open. Earlier pending worldgen,
rift, compatibility, launcher, IDE and D31 changes remain outside this selective
local checkpoint. Nothing is pushed.


## Explicit tank sender and menu recipients — D45, 2026-10-12

Completed tank synchronization through the existing native channel bridge. Original
source c29c4fcc also left base syncTankData dormant; it is now a real public opt-in
refresh, with no added automatic reflective broadcast on every BE update. The
named-field sender retains the original 24-block same-dimension radius, with an
explicit clientbound PacketTarget overload. Snapshot reads require the owning
server thread and a loaded/live tile. Unbound channels and invalid fields cannot
produce partial frames.

Thirteen accepted RotaryCraft tank menus use CoreContainer's owner-targeted helper
instead of repeating nearby broadcasts per viewer. The reservoir's physical
dedicated-server gate is replaced by logical-server routing; Steam's initial
sendAllDataToRemote path is included. Existing nearby/tool sends and full/periodic
BE tags remain. Pristine excluded Chroma/integration source is retained.

TankSyncData caches inherited instance/subtype field discovery, rejects static,
null and shadowed targets, owns detached snapshots and validates the complete
canonical body. All four active DA/RC/ReC/EC receivers share decoding before
mutation, reject serverbound TANK and only resolve loaded client tiles. Unknown
named fluid IDs cannot fall back to the old fluid. Fill/switch/empty/capacity
semantics and existing same-fluid components survive; deprecated level-only direct
updates cannot invent an empty tank's fluid. Existing protocol 2.1.0, ordinals,
payload IDs and canonical bytes are preserved. New fluid components/type identities
remain a deliberate future wire migration, with component state supplied by BE tags.

Three units, five new native fixtures, all 89 native DragonAPI regressions, all
178 family units and eight crafter regressions pass. A final focused rerun validates
the consolidated receivers. Eight guards cover 3,285 outputs (DA 670 / RC 1,023);
development DA/RC jars build. Normal Play arguments are unchanged. Captured native
transport calls are not actual remote socket/render acceptance.

Contract: DragonAPI/docs/tank-sync-port.md. Evidence:
TestInstance/docs/tank-sync-2026-10-12.md and build/tank-sync-*.log. D45 is complete;
D46 explicit sub-IDs/negotiated schemas and remaining ports/remote acceptance stay
open. Validation includes earlier working-tree changes; this selective checkpoint
preserves worldgen/rift/compatibility/launcher/IDE/D31 work outside its commits.
Nothing is pushed.


## Explicit stable packet identities — D46, 2026-10-12

After committing/pushing all earlier work at root 2073045 and its pinned module
heads, migrated accepted packet discriminators from enum ordinals to explicit
wireId values. Seven tables preserve all 353 published numbers: category types,
DragonAPI controls, private field-sync primitive tags, Rotary/Reactor/Electri
controls and Chroma's complete 212-entry enum. Immutable ID maps reject unknown
values; duplicate/negative tables fail initialization. Chroma no longer clamps a
malformed control into another operation. All old constructor payload arguments
remain unchanged, including counts, type flags and variable-data markers.

The Gradle accepted-source inventory identifies/migrates 101 direct ordinal lines
and the blower's indexed packet control; private sync tags/category encoding use
the same API. Gameplay payload enums remain intact. Excluded pristine consumers
retain source parity and must adopt wireId during their complete subsystem ports.
The actual bridge registrar uses PROTOCOL_VERSION = 2.1.0: no number/frame/schema
change is introduced. Future incompatible IDs/schemas require deliberate version
negotiation; IDs must never be recycled.

Four new units and two native fixtures check scrambled tables, duplicates,
negative/unknown IDs, independent published table fingerprints, all public lookups,
native category round trips and old RAW bytes. All 91 DragonAPI native tests,
182 family units and five packet-selector regressions pass (the latter repeats
the two new fixtures and additionally covers Sonic/Defoliator/packet authority).
Eight guards verify 3,287 outputs; development DA/RC jars build. The final 3,461-file
accepted-source audit finds no packet-discriminator ordinal encoding, and all 353
constructor payloads are checked against the pushed checkpoint. Normal Play hashes
are unchanged. Actual remote socket/render acceptance remains separate.

Contract/mapping: DragonAPI/docs/packet-id-port.md and packet-ids-protocol-2.1.json.
Evidence: TestInstance/docs/packet-id-2026-10-12.md and build/packet-id-*.log/json.
D46 is complete for the accepted port; D47 dimension-field semantics is next. This
continuation is committed locally after the already verified published checkpoint.


## Dimension-scoped entity and biome packets — D47, 2026-10-12

Replaced ignored numeric dimension slots, zero placeholders and ResourceKey hashes
with explicit namespaced DIMENSION keys in six strict RAW packet bodies. The actual
bridge registrar now negotiates protocol 2.2.0; protocol 2.1 peers must update both
ends. All 353 historical category/control/tag numbers stay unchanged. Legacy or
malformed bodies and wrong directions are rejected before gameplay dispatch.

Restored verifyClientEntity and preserved the original existence/class-hash check.
The server ignores requests for a dimension the player has left; failure replies
retain the requested dimension/class, and only a matching client entity is removed.
Server entities cannot be removed through this operation. Player implementations
use the common Player class hash across logical sides; other entities retain exact
class-name hashes. Biome PNG start/data/end
frames retain the captured source dimension even if the completion callback moves
the player. Maps capture the actual source Level/registry and discard stale sessions;
client level unload/logout clear unfinished maps. Existing names, samples, batching,
padding, colors, grid, PNG/CSV exports and callbacks remain.

The formerly ineffective custom BIOMECHANGE client receiver now writes native quart
palettes across all loaded sections, preserves neighbors, clears tint caches and
invalidates renderer sections. It uses a namespaced biome key rather than registry
iteration order and does not generate missing chunks. The authoritative server
helper retains native FillBiomeCommand synchronization with correct inclusive 26.3
height bounds. Excluded pristine 1.7.10 consumers remain untouched.

Three new units and four native fixtures cover schema goldens, custom keys/Unicode,
malformed/old/trailing/truncated frames, entity/class/dimension checks, Nether stream
frames, callback dimension changes, full-height/neighbor palettes and stale map
sessions. The opt-in disposable client fixture covers production request/reply and
client palette application. Normal Play retains its separate arguments/directory.

Contract: DragonAPI/docs/dimension-packet-port.md. Final evidence and acceptance
boundary: TestInstance/docs/dimension-packet-2026-10-12.md. D47 is complete for the
accepted port; D48 EventProfiler listener instrumentation is next. Actual remote
client and Iris/Distant Horizons acceptance remain open.

D47 final validation: 185 family units and all 95 required native DragonAPI tests
pass. Eight guards verify 3,290 primary outputs; development DA/RC jars build. The
disposable integrated client passes native entity request/reply, stale reply guards,
client biome palettes, PNG export and the existing full audit, then exits cleanly
in 3m 20s. Normal Play argument hashes are unchanged. This batch is checkpointed
locally; ongoing IntelliJ workspace.xml changes are preserved outside the checkpoint.


## Donation head renders and personal Play identity — 2026-10-12

At the owner's request, fully ported the original spinning HSLA steel gear,
spinning toroid-magnet head render and ChromatiCraft detached-pylon flare. The
previous gear method omitted its draw; Reactor/Chroma renderer files were excluded
legacy code. They now submit the original model/sprite geometry through native
26.3 item/deferred render paths. Their original translations, animation clocks,
scales, pitch handling and priorities are retained, including RenderMagnet's
internal detached 0.4 scale/0.6 offset and the pylon's separate non-world card branch.
Deferred callbacks capture pose/color data and do not follow a reused player stack.

Client-only load-complete subscriptions restore the original Reika donor + Patreon
registration through one shared PlayerSpecificRenderer helper. Registrations are
idempotent, including duplicate roster entries and developer mirrors. Reika and
Officialy previews use the actual NeoForge production flag rather than interpreting
26.3's unobfuscated classes as a developer environment. Pylon preview construction
is deferred until first rendering, after the block registry is bound; constructing
it while NeoForge discovers subscriber classes failed the first client validation
and was corrected. The excluded legacy client proxies remain pristine/excluded.

Play now uses OfficialyAwsome / bb5029b7-9381-4d99-aaa9-106da41aa659. Mojang's public
session profile confirms that account and signed skin. Passing the default dev
token explicitly enables 26.3's native public skin/profile lookup without storing
account credentials. Automated Checks keeps its test identity unless the explicit
donatorIdentityAudit property is supplied; it uses the separate disposable directory.
The optional officialy_glow mask is missing and has not been replaced with invented
art. The three restored head effects use existing original models/sprites.

Contract/source pins and validation: TestInstance/docs/donator-head-renders-2026-10-12.md.
Logs: build/donator-render-family-validation.log and donator-render-client-validation.log.
The client fixture checks identity/skin, exact registrations, idempotence, native
gear items, deferred toroid/pylon geometry, pose restoration, transparent ordering
and the real third-person render path. Existing family regression/acceptance work
continues independently; D48 EventProfiler remains next on the numbered backlog.

Final head-render validation: 185 family units, all 95 required native DragonAPI
GameTests and eight guards (3,293 accepted primary outputs) pass. The disposable
client uses the requested OfficialyAwsome UUID, loads its downloaded skin, checks
all three original head renders and idempotence, then reaches the real third-person
render path. The existing full audit/PNG export also passes; clean exit in 3m 40s.
Default Play and Automated Checks launch files are regenerated separately after
the opt-in identity test. This continuation is checkpointed locally.

## D48 — actual EventProfiler listener measurement, 2026-10-13

The earlier command-only limitation is resolved: NeoForge.EVENT_BUS is constructed
as a native EventBus subtype with the original builder, and enabling profiling
temporarily instruments the selected native listeners. Cancellation, priority,
inheritance, registration/unregister and native exception handling are preserved.
Nested posts keep separate timers; event fires are actual posts, and reports include
per-body calls and failures. Stop restores original listeners and retains a coherent
immutable report. Exact concrete class and initiating-thread scope are explicit.

Client shortcut classes no longer load eagerly in common code. Ambiguous nested
aliases report a status instead of selecting whichever was registered last, and
the command parser now accepts binary class names containing $. Reflection is
isolated to the bus-8.0.5 adapter, with unsupported instrumentation reported before
starting a session. Legacy explicit timing hooks remain available.

Contract/source pin: docs/event-profiler-port.md. Seven new units pass, alongside
the complete 192-unit family. All 96 required native DragonAPI fixtures pass. The
new global-bus fixture profiles real block changes and the existing Chroma network
subscriber. The separate disposable client also measures real native client-event
callbacks, passes the complete existing audit and exits cleanly in 3m 38s.
Evidence: TestInstance/docs/event-profiler-2026-10-13.md.

## D49 — PlayerChunkTracker synchronization and native loading, 2026-10-13

Confirmed unsynchronized reads/tick flags, missing lifecycle cleanup and obsolete
movement-field writes. The existing MixinChunkMap froze client view updates while
native player loading tickets still moved. The tracker now runs mutation/callback
work on its owning server and binds server + UUID to the exact player and level.
Concurrent queries and snapshot iteration preserve reentrant registrations without
overwriting newer entries. Lifecycle cleanup is scoped and prevents resurrection;
terminal identities are weakly held rather than retaining old worlds/players.

The native skipPlayer hook removes only the suspended player's loading contribution.
Expiry/stop restore native tickets and the current view through move/updateChunkTracking,
including without further movement. Original 40-tick grace, true-condition reset,
dead-player behavior and same-owner condition renewal are retained. xo/zo are never
rewritten. Callback failures release suspension, log the error and preserve restoration.
The old PlayerManager bridge is retained against ChunkMap.

Contract: docs/player-chunk-tracker-port.md. All 192 family units and 100 required
native DragonAPI fixtures pass, including four new ticket/view/lifecycle/worker
regressions. The disposable integrated client also passes movement/view restoration
and actual logout/unload/server-stop cleanup, then exits cleanly in 3m 31s with the
complete existing audit passing. Eight guards verify 3,297 accepted primary outputs.
Evidence: TestInstance/docs/player-chunk-tracker-2026-10-13.md.

## D50 � updater remains inactive, 2026-10-14

The owner explicitly requires the port updater to remain inactive until Reika's
release permission exists. No maintained feed, scheduled fetch or update login
hook is configured. The inherited legacy endpoint remains excluded. D50 stays
open; runtime transitions and notifications are deferred by this instruction.

DragonAPI/docs/update-checker-inactive.md records the restriction and source pin.
UpdateFeedClient is fully written future transport/parser infrastructure with no
runtime callers. Four local-only regressions cover bounded HTTP transport, missing
Last-Modified and malformed/unavailable feeds; failures never become comparison
versions. No release or feed data was published.

## D51 � packaged FILEHASH and native client lifecycle, 2026-10-14

The original ClientLoginEvent depended on an unsent legacy LOGIN packet. A
client-only native LoggingIn/LoggingOut subscriber now raises the original common
events, including automatic FILEMATCH dispatch after connection/reconnection.
The updater remains inactive. Hash registration tolerates parallel mod initialization;
read-only HashCheckEvent reports decisions after enforcement, while cancellation
remains exclusively on the original HashKickEvent. Protocol 2.2.0 and historical
packet IDs/bodies are unchanged.

The opt-in harness loads eight immutable private packaged jars in separate JVMs
and directories. Server binds IPv4 loopback only, with an ephemeral port; Play's
identity, saves and audit-disabled configuration are preserved. Only one inert
META-INF entry differs between DragonAPI variants; every original entry has
identical bytes. Matching archives pass, enabled mismatch disconnects with the
actual reason/hashes, saved-and-synchronized FILEHASH=false permits the mismatch,
and the original event cancels enforcement on the final reconnect.

Final run 20261014-d51-f passed the required packaged GameTest and all four
client/server decisions. State is ALL_PASS with final client acknowledgment PASS.
Server exits cleanly in 4m 22s; matching client in 1m 42s; different client in 1m 41s.
Fixture corrections save config before synchronization, close the actual connection
before reconnecting and enter terminal completion before client teardown.

Contract: DragonAPI/docs/packaged-filehash-port.md. Evidence/rerun commands:
TestInstance/docs/packaged-filehash-2026-10-13.md. All 196 family units and 100
ordinary native DragonAPI GameTests pass; eight output guards verify 3,302 accepted
primary outputs. D51 is complete. D50 remains owner-deferred; mixed-protocol peer
negotiation, authenticated remote hosting and shader/LOD acceptance remain open.

The ordinary disposable integrated client audit also passes and exits cleanly in
3m 56s, including native automatic login dispatch and the complete prior audit.
Actual PlayerChunkTracker logout/unload/server-stop cleanup still passes. Normal
Play retains OfficialyAwsome, bb5029b7-9381-4d99-aaa9-106da41aa659 and auditClient=false.
Evidence: build/packaged-hash-integrated-client-validation.log. Checkpointed locally.

## D55 � obsolete packaged transformers and duplicate mixin config, 2026-10-14

Verified native 26.3 sources, current family consumers and actual baseline jars.
DragonAPI's two SRG field transformers have no current consumers: the old Minecraft
block-renderer field is gone, and ModelPart traversal uses public native visit.
The historical audit's suggested Minecraft.minecraft replacement is also absent.
RotaryCraft's four SRG ShapedRecipe parser transformers target removed methods;
current original-recipe datagen uses ShapedRecipePattern.of and native codecs.
Both unused files and their explicit TOML registrations are removed.

The unused dragonapi.mixin.json is unreferenced in source/build/TOML/manifest,
has empty arrays and names a nonexistent plural package/refmap. It is removed;
the real mixins.dragonapi.json and mixins.rotarycraft.json remain byte-identical
with their metadata entries preserved. All deleted declarations and their intent
are recorded in DragonAPI/docs/resource-ownership-d55.md. ReikaRenderDispatcher
and LODModelPart source references remain available for later full porting.
No accepted Java content, recipes or rendering logic is removed or excluded.

Rebuilt jar inspection confirms no obsolete resources or transformer declarations;
31 DragonAPI mixin entries and RotaryCraft's entry remain packaged. All 196 family
units and 100 required native DragonAPI GameTests pass; eight output guards verify
3,302 primary outputs. Fresh packaged clients/server pass matching, enabled kick
with exact reason, disabled acceptance and original cancellation. Clean exits:
server 3m 16s, matching client 59s, different client 1m 2s. A first socket-abort
attempt is preserved separately and is not counted as acceptance; the unchanged
strict assertions pass on the restarted sequence.

D55 is complete. D56's complete active/optional mixin and distribution matrix
remains open. Play configuration is preserved; updater stays inactive per owner.
Evidence: DragonAPI/docs/resource-ownership-d55.md and build/d55-*.log.
This batch is checkpointed locally.

## D56 complete — runtime mixin/distribution validation, 2026-10-14

All 45 configured mixin classes (46 target bindings) are verified through actual
native transformation, required-injection checks and exported merged-origin or
accessor-interface evidence. The audit rejects missing registration, duplicates,
missing target evidence and absent targets from installed optional owners.
Dedicated-server scope excludes the 14 client bindings explicitly.

Five fresh packaged client/real-dedicated-server pairs pass: neither optional mod
(30 server / 44 client transformed, two absent); Chunky alone (31 / 45, one absent);
DH alone (31 / 45, one absent); both (32 / 46); and both on the server with installed
Iris/Sodium/DH/Chunky on the client (32 / 46). All peers exit cleanly. Installed
optional jars are unchanged. These are private test copies, not published releases.

DragonAPI declares JAVA_25. The already-registered population mixin is preserved.
The real dedicated boot exposed ChromatiCraft common startup loading client
ColoredAltarEffects/Particle. Common ColoredAltarNetwork now owns the unchanged
altar_visual version-1 codec and server sends; native client-only payload-handler
registration invokes the existing main-thread effects. All fields, recipient radius
and original shock/bolt/break/fragment behavior remain. The source allowlist adds
the complete common network class; no behavior/content is excluded or stubbed.

The source-folder base client passes the full existing audit in 4m 22s. The final
isolated integrated DH/Chunky run passes 46 client and 32 integrated-server bindings,
actual provider/listener lifecycle, callbacks/rebinding/shutdown and the full
existing audit; clean exit in 3m 47s. All 196 family units and 100 native DragonAPI
fixtures pass. Eight guards verify 3,305 accepted primary outputs.

Audit runs have separate directories, test identities and bounded heaps. Loopback
servers have explicit connection/polling settings. Only native warning-only
Continue Launch actions are acknowledged; fatal loading screens fail. Existing
acceptance reports are rejected by paired launch tasks, requiring fresh IDs.
Earlier fixture/native-window failures are preserved separately, not counted as
acceptance. Normal Play keeps its identity and disabled audit flag. Default audit
arguments are restored after optional runs. The updater stays owner-deferred.

Contract, full binding inventory, evidence and rerun commands:
DragonAPI/docs/mixin-runtime-matrix.md. D56 is complete; D57 live ASM/obfuscation
helpers and environment detection are next. Public authentication and visual
shader/LOD acceptance remain separate open tasks. Checkpointed locally.


## D57 — Retire legacy ASM and obfuscation framework (2026-10-14)

Removed ReikaASMHelper, Patcher, ASMException, MissingASMException and
ReikaObfuscationHelper from DragonAPI Java sources. Removed the already-excluded
ChromaASMHandler from ChromatiCraft Java sources. All six files are preserved
byte-for-byte as noncompiled .java.reference documents; the original paths and
SHA-256 digests are in DragonAPI/docs/legacy-asm/manifest.json. No runtime
transformer framework, SRG map, legacy opcode reflection or hardcoded development
probe remains in the accepted slice. No gameplay hook was removed: LuaBlock was
the only nonframework ASM-helper caller; the Chroma handler was never active.

LuaBlock delegates scalar parsing to plain Java LuaValueParser. Numeric hint
widths, integer/long/double/string inference and overflow fallback are preserved.
Boolean hints now work, and unrelated bracket text no longer steals the datatype
match. Malformed annotations retain their text. Two regression tests exercise
actual LuaBlock map export and preservation rather than the parser implementation.

Active recipe-cache and item-diagnostic callers use the real 26.3 FML 12.0.8
FMLEnvironment.isProduction() flag; DragonAPIMod names/logs development mode
accurately. Readable names are not used to detect development. Excluded original
ChromatiCraft reflection/dev callers remain untouched until their full subsystems
are ported; the archive README lists the replacement rules and original active
patch behaviors. Moving reference code does not complete those behavior ports.
Existing accepted Mixins, accessors and TestInstance's useful ASM inspection remain.

Validation: all 198 units pass (DragonAPI 145, RotaryCraft 34, ReactorCraft 8,
ChromatiCraft 11), with no failures/errors/skips. All eight accepted-class guards
pass. Native DragonAPI run passes all 100 required fixtures and the strict common
MixinAudit (30 transformed bindings, two absent optional integrations). Source
snapshot digests match; fresh DragonAPI/ChromatiCraft jars contain neither retired
classes nor reference archives. No stale retired class outputs remain. Evidence:
build/d57-family.log, build/d57-native.log, build/d57-proof.json and
build/mixin-audit/20261014-d57-native/gameTest-GameTestServer.json.

Normal Play and updater inactivity are preserved. Local checkpoint only; unrelated
.idea/workspace.xml edits are excluded. D57 is complete; D58 is next.


## D58a — Command/chat correctness and generated translations (2026-10-14)

D58 remains open. This batch fixes the actual item/coordinate/block chat format
exceptions, readable entity names and the no-op writeLocalString. The latter now
sends an intact translatable component through a client-only, player-null-safe
writeComponent path. ClientEnvironment explicitly marks nullable player/level/
resource returns and logs version-reflection failure with its cause.

Entity-list invalid-side and console/client failures use failure feedback and
return zero; headers, guide custom name, modular-logger state/error feedback and
biome-map success/error wrappers are translatable components. A native client
language provider emits 17 entries, preserving all six original language entries.
The duplicate manual language resource is removed after comparing generated values.
The produced jar contains exactly one en_us resource matching all generated keys.

Biome-map player/argument routing now uses a typed MapTarget record, replacing
Object[] and unchecked casts. Removed unused private start-time argument and
unused imports. Map failure logging includes transfer hash, dimension and throwable.
Packet decode/handling errors include type/packet and cause in one logger event,
without a separate stderr stack trace. Wire IDs, payload formats, map sampling,
permissions and callback behavior are preserved.

Validation: native client datagen and all 198 family units pass with zero failures,
errors or skips. Native run passes 101 required fixtures and strict common mixin
audit (30 transformed bindings; two optional integrations absent), clean exit in
1m 38s. New/expanded regressions cover original formatting failures, invalid-side
return status, console/client rejection, lazy guide creation retaining its key and
console biome target argument offset. All accepted-class guards pass. Evidence:
build/d58-command-family.log, build/d58-command-native.log and
build/d58-command-proof.json. Localized visual presentation was not exercised by
this server test. Normal Play and inactive updater remain unchanged.

Remaining D58 work: profiler/pregeneration and other public command translations,
biome validation detail translations, remaining raw/generic types and null
contracts, and contextual exception logging throughout accepted libraries. No
claim that D58 as a whole is finished. Local checkpoint; unrelated IDE edits kept.


## D58b — Profiler and bounded-population feedback (2026-10-16)

Profiler and pregeneration command feedback use generated translation components.
All six profiler error states and five population states have keys; 40 language
entries now generate. Profiler errors send failure feedback and return zero.
Numeric diagnostics use Locale.ROOT. Handler count/time/call/failure details remain
in both chat and contextual logs. Population status uses immutable typed snapshots;
the historical mutable diagnostic-string list API and original text are preserved.
No admission, queue, persistence, permissions or population control behavior changed.

Validated on the owner's existing local NeoForge 26.3.0.76 setting: 201 family units
(147 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft), zero failures,
errors or skips; 101 native fixtures and strict common MixinAudit pass, clean exit
in 1m 14s. Two new unit tests cover immutable snapshots, state transitions and
feeding/save/reload recovery. Native commands cover missing profiler arguments,
invalid event return status and population start/status/pause/resume/cancel.
Evidence: build/d58b-family.log, build/d58b-native.log, build/d58b-proof.json.
D58 remains open. Existing IDE, version, datagen-provider and other mod changes
are preserved outside this scoped checkpoint. Updater stays inactive. Next:
reflection correctness, types/null contracts and contextual failure handling.


## D58c — Java 25 reflection correctness (2026-10-16)

Private primitive readers now handle static fields, inherited fields and restore
access on failed reads. Historical null/sentinel behavior remains; failures use
contextual logger events with causes, and debug chat tolerates an uninitialized
config. Nullable lookup returns and optional selectors are explicit.

Replaced obsolete Field.modifiers mutation with the actual Java 25 Field.set
contract from installed JDK 25.0.2 sources: ordinary non-static final fields are
writable for reconstruction before publication, without changing final modifiers.
Access override is restored after success and failure. Static/record/hidden final
writes fail clearly; there is no Unsafe or JVM-internals fallback. Missing fields
report NoSuchFieldException. Parent constructors are no longer falsely returned
as inherited constructors; inherited ordinary method lookup is preserved.

Invocation caches are ClassValue-owned concurrent maps keyed by method name AND
immutable parameter types. Exact runtime-type convenience calls retain their API;
a new explicit-signature overload supports primitive/interface/nullable arguments.
Different overloads no longer reuse the first cached method. Failures retain null
results with actual invocation causes logged. No legacy ASM/SRG machinery returns.

Six regressions cover field inheritance/static/sentinels, constructor ownership,
reconstruction and access restoration on failure, prohibited final writes,
concurrent alternating overloads, and primitive/interface/null arguments.
All 207 family units and 101 native fixtures pass on the existing local 26.3.0.76
setting, with strict common MixinAudit. Evidence: build/d58c-reflection.log,
build/d58c-family-native.log and build/d58c-proof.json. D58 remains open.
The prior D58b ledger count is corrected to 201: the owner's newer ChromatiCraft
suite contains 12 tests. Existing version/datagen/IDE edits stay outside commits.


## D58d — Command failure handling and validation translations (2026-10-16)

/getuuid resolves its entity before scheduling feedback, so console use produces
the normal checked Brigadier syntax error rather than a wrapped RuntimeException.
/debugtest now requires administrator permission and supports authorized console
feedback; rejected sources cannot toggle the global flag. Feedback is translated.
Biome range/resolution/grid/seed validation details use stable reason keys and
nested translatable components, including malformed-number feedback. Limits and
seed/map algorithms are preserved. The Jade temperature row keeps its server
value as a translation argument; all 50 language entries come from datagen.

207 units pass with no failures/errors/skips; 101 native fixtures and common
MixinAudit pass, clean exit in 1m 1s. Native tests check entity/console UUID paths,
permission rejection without debug mutation, authorized console toggle (restored
in finally), and invalid numeric/grid map arguments. The temperature test checks
exactly one translated row with the server's numeric argument, and DC engines
still have none. Its prior hardcoded-English assertion failed as expected during
migration and was replaced; the rerun passes. Fresh jar has 50 entries.
Evidence: build/d58d-family.log (initial tooltip expectation), build/d58d-native.log
(final passing checks) and build/d58d-proof.json. D58 stays open; no updater/feed
behavior or normal Play setup changed. Existing owner edits remain untouched.


## D58e — Honest array types, null contracts and class loaders (2026-10-16)

The one-argument collectionToArray now declares its actual Object[] result instead
of an unsound generic E[] cast. A factory overload supplies correct runtime types
for empty, mixed-subtype and nullable-element collections. There are no in-tree
callers of the old one-argument method; its erased JVM descriptor remains unchanged,
but typed source callers must use the factory overload. Reverse map lookup handles
null values with first-match order preserved. Random helpers declare nullable
results; empty removal never draws from RNG or mutates immutable collections.
Nonempty selection/removal order and seeded draw count are preserved.

Class initialization uses the supplied Class's defining loader. Failure logs keep
class/loader context and full causes without dereferencing a nullable exception
message. Existing metadata export on linkage failures remains. Five regressions
cover arrays, null lookup, immutable empties/no draws, seeded list/set removal and
an independently defined class initialized in its own loader.

212 family units and 101 native fixtures pass on local NeoForge 26.3.0.76, with
strict common MixinAudit, clean native exit in 1m 52s. Evidence:
build/d58e-library.log, build/d58e-family-native.log and build/d58e-proof.json.
An opt-in LanguageClientChecks fixture now checks all generated entries in the
actual client Language and captures the two intact translated messages through
NeoForge's native local-chat hook. It restores audit chat state/listeners. The
19-class TestInstance slice compiles, but this new graphics check has NOT run:
an existing gotoClient game was active and was left untouched. It runs during the
next ordinary automated client audit, and is inert in Play. D58 remains open.


## D58f — Bulk stream reads and constructor diagnostics (2026-10-16)

streamToBytes uses native bulk reads through EOF rather than boxing every byte.
It does not close caller-owned streams; IOException still returns null, now with
stream-type context and cause in one logger event. Nullable array result is explicit.
Constructed collections accept subtype inputs/constructors, preserve order and
never expose a partial result. Constructor failures identify the entry index and
output class with the original target exception as cause. The lookup overload
retains its historical null-on-failure contract and structured diagnostics.

Three regressions cover short bulk reads/all byte values/ownership, failure without
closing caller input, and ordered construction/missing signatures/actual target
failure. All 215 units and 101 native fixtures pass; common strict MixinAudit passes.
Final native build exits cleanly in 1m 42s. Evidence: build/d58f-stream-construction.log,
build/d58f-family-native.log and build/d58f-proof.json. Client language acceptance
remains compiled but deferred while the existing game is active. D58 remains open.


## D58g — Local reader and directory traversal correctness (2026-10-16)

The no-filter directory overload now passes a null ARRAY rather than one null
extension. It no longer throws on the first ordinary file. Empty/null filters list
all files, overlapping extensions include a file once, and original case-sensitive
suffix matching and traversal order remain. Per-call canonical directory identities
bound cycles/aliases without a global cache or Windows link-creation privileges.

Local File/String readers build a decoder before opening a handle, report
file/path/charset with causes, and explicitly declare nullable results. The old
InputStreamReader replacement policy for malformed bytes is preserved; a direct
Files.newBufferedReader migration would silently change it to REPORT. Stream
wrapping failures leave caller-owned input open; successful readers own the input
until closed. Line-consuming helpers preserve partial-result/close policy while
logging causes contextually. Resource-root Class is parameterized and resource
read errors retain path/class/line context. Network helpers are unchanged.

Four regressions cover nested unfiltered traversal, overlapping filters and case,
a canonical directory cycle, UTF-16/non-ASCII and malformed-byte replacement,
reader ownership and wrapping/missing-file failure. All 219 family units pass
(165 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft), zero failures,
errors or skips. 101 native fixtures and strict common MixinAudit pass, clean exit
in 1m 53s. All eight accepted-output guards pass. Fresh jar has exactly one
50-entry generated language resource. Evidence: build/d58g-files.log,
build/d58g-family-native.log and build/d58g-proof.json.

The six completed D58b–g batches add 20 unit regressions. The client language
fixture compiles but remains unexecuted: another gotoClient launch is active and
was left untouched. Existing owner version/datagen/IDE changes are uncommitted;
all completed changes here are scoped local checkpoints. Updater stays inactive.
D58 stays open for remaining library generics/nullability and contextual logging,
remaining UI/integration translations, and actual client-language acceptance.
Checklist remains 44 complete / 119 open / 163 total.


## D58h — Writer ownership, safe binary replacement and real client language acceptance (2026-10-16)

Line writers now close their owned writer after write/flush failure as well as
success. File output no longer wraps PrintWriter, which swallowed actual write
failures. Charset-default overloads preserve platform behavior; explicit charset
writer/line-file overloads support portable formats (including the original
forward reference in RedecorateCommand). Encoders retain replacement policy and
are prepared before opening/truncating files. Null-line text, trailing platform
line separators, quiet error flags and ownership semantics remain. Stream encoding
no longer redundantly closes its writer. Failures carry file/charset/cause context.

Binary output validates the complete payload before touching the destination,
writes a sibling temporary file, then atomically replaces the destination where
supported (replace fallback otherwise). A failed write cannot first delete the
old data. Parent creation and byte values are preserved; incomplete temporaries
are cleaned up. Four new regressions cover exactly-once close, early write failure,
explicit/default charset and stream agreement, null payload without data loss,
full byte values, nested parent creation and temporary cleanup.

223 family units pass (169 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. 101 native fixtures and strict common MixinAudit pass;
all eight accepted output guards pass; clean native exit in 1m 59s. Evidence:
build/d58h-writers.log, build/d58h-family-native.log and build/d58h-proof.json.

The previously pending client-language acceptance is now COMPLETE. Once no game
client was active, the private automated client ran successfully in 4m 15s. Its
StableClientLauncher snapshot loaded all 50 generated entries, resolved the numeric
temperature argument, and delivered both intact translated messages through the
real NeoForge local-chat hook, restoring listener/chat state. The complete existing
client audit also passed, including donation rendering, artefact zone, water puzzle,
models, sounds, packets, assets, biome image and chunk-tracker shutdown. Strict
client 44 and integrated-server 30 bindings pass, with two absent optional targets.
This client snapshot preceded writer compilation; the new writers were separately
validated by their unit tests and final native build. Log: build/d58-language-client.log.
Reports: build/mixin-audit/20261016-d58-language-client/.

Default automated-client arguments restored (auditClient=true, no mixin debug/audit
flags). Play arguments/config, updater inactivity and existing owner edits preserved.
Local checkpoint only. D58 remains open for the remaining accepted-library type,
null and exception-logging cleanup and untranslated integration/UI text. Checklist
remains 44 complete / 119 open / 163 total.


## D58i — Runtime type identity and typed value sorting (2026-10-16)

Primitive inference now recognizes actual boxed Java values rather than matching
simple class names. Unrelated Integer/Boolean/Character/Void classes retain their
real types; class tokens remain Class values. Null still fails because it has no
inferable runtime class, now with an explicit diagnostic. Object-class arrays no
longer use a raw Class allocation.

Package helpers delegate to Class.getPackageName(), including component-package
semantics for arrays, java.lang for primitive/void types, and the empty unnamed
package. Top-level lookup handles these cases without substring exceptions.

Value sorting preserves key/value types and stable encounter order for equal
values, returning the existing LinkedHashMap-backed ordered copy without mutating
the input. Natural sorting requires Comparable values; the comparator overload
supports non-comparable values and explicit null ordering. ReverseComparator is
generic. Existing erased public descriptors remain; the natural-sort source API
now rejects Object-valued maps without an explicit comparator. There are no
in-tree consumers requiring migration. The obsolete commented duplicate and raw
private comparator have been replaced by the typed implementation.

Three regressions cover all eight wrappers and name collisions, null/empty type
inference, nested/array/primitive/unnamed packages, stable natural/reverse sorting,
input preservation, empty maps and custom null/non-comparable ordering.
226 family units pass (172 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and eight accepted-class guards
pass on the owner's current NeoForge 26.3.0.76 setting; clean exit in 2m 2s.
Evidence: build/d58i-types.log, build/d58i-family-native.log and build/d58i-proof.json.

D58 stays open for remaining accepted-library null/type and contextual logging
cleanup and untranslated integration/UI text. The next concrete review targets
are safe numeric parsing and class-resource/metadata diagnostics. Checklist stays
44 complete / 119 open / 163 total. Local checkpoint only; existing owner edits,
normal Play configuration and the inactive update checker remain untouched.


## D58j — Safe decimal parsing and class diagnostics (2026-10-17)

safeIntParse/safeLongParse now parse the complete signed value, including the
minimum int/long values, and return the historical zero fallback for overflow
instead of throwing during text input or JVM-argument handling. Input syntax is
preserved: optional ASCII minus and ASCII digits; no plus, whitespace, Unicode
digits, fractions, exponents or hexadecimal. Nullable input is explicit. The
shared character check avoids compiling a regex for each parse.

Class-resource reads use InputStream.readAllBytes and retain the empty-array
fallback and ownership/closure contract. Missing resources identify class, path
and loader; read failures carry contextual logger events and causes. Class dumps
use try-with-resources so a failed write also closes the output, and null payloads
are rejected before truncating an existing file. Existing naming behavior remains.

Metadata export uses a real throwing writer instead of a disk PrintWriter that
hid write/flush errors. Parent creation and the default charset/replacement policy
are preserved. Recoverable reflection/linkage/annotation failures retain partial
reports and now include section labels and full failure traces in both contextual
logs and the report. Catch-all Throwable recovery is removed: writer IOExceptions
reach the export boundary for logging/closure; VM-fatal errors are not swallowed.
The existing General/Internal Classes/Constructors/Fields/Methods information is
retained. Null classes are rejected before touching output.

Four new regressions cover boundary/overflow/syntax and 1,000 seeded numeric
round trips, owned resource success/failure/missing/null cases, a deliberately
unavailable signature dependency with preserved report sections/full causes,
writer IOException propagation, and byte-exact class dumps/null-payload retention.
230 family units pass (176 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and all eight accepted-class
guards pass on NeoForge 26.3.0.76; clean exit in 2m 25s. Evidence:
build/d58j-diagnostics.log, build/d58j-family-native.log and build/d58j-proof.json.

D58 remains open for other accepted-library raw types, nullability and remaining
integration/UI translations. Checklist stays 44 complete / 119 open / 163 total.
Local checkpoint only; existing owner edits, Play configuration and the inactive
update checker remain untouched.


## D58k — Class lookup and nested collection contracts (2026-10-17)

Class availability probes still avoid initialization and use the thread context
loader. With no context loader they now fall back to this library's defining
loader rather than accidentally probing only bootstrap classes. Missing classes
remain quiet; linkage failures return unavailable with debug cause/context.
getClassNoException explicitly returns nullable Class, preserves its historical
initialization/defining-loader behavior, and reports linkage/initializer failures
with full contextual causes before returning null. Fatal VM errors escape both
recovery paths.

Nested map helpers now iterate values directly. The old keySet/get traversal
mutated access-ordered LinkedHashMaps and could throw ConcurrentModificationException.
Counting remains shallow; null scalar entries still count as one. Flattening and
combining accept covariant concrete nested maps/collections, preserve encounter
order, duplicates and null elements, and return independent mutable copies. Null
nested collections still fail rather than silently discarding data.

Class-selected collection sorting has a typed collection/base-class/comparator
contract while preserving its original natural-vs-hash selector, stable ties and
copy-before-sort behavior. The necessary runtime Comparable cast is confined to
the documented legacy Class-selector boundary. exceptionMentions no longer takes
a raw Class. Public erased descriptors remain unchanged. FluidHashMap's accepted
size caller and GeoStrata's optional Chisel availability probe retain their roles;
Chroma PoolRecipes can pass concrete nested collections without casts.

Six new regressions cover context/null-context lookup, initialization timing,
first/repeated initializer failures, unavailable linkage vs fatal errors,
access-ordered nested maps and collections, covariance/nulls/duplicates/independent
copies, shallow counts, inherited Comparable ordering and stable hash collisions.
236 family units pass (182 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and all eight accepted-class
guards pass on NeoForge 26.3.0.76; clean exit in 2m 36s. Evidence:
build/d58k-contracts.log, build/d58k-family-native.log and build/d58k-proof.json.

D58 stays open for remaining accepted-helper contracts and untranslated integration/
UI text. Checklist remains 44 complete / 119 open / 163 total. Local checkpoint
only; existing owner edits, Play configuration and inactive updater are preserved.


## D58l — Bounded rotations, resource locations and module-aware discovery (2026-10-17)

List rotations now normalize the signed distance with floorMod before native
Collections.rotate. Positive/right and negative/left behavior is retained, with
work bounded by list size rather than requested distance. Integer.MIN_VALUE no
longer becomes a no-op through abs overflow; normalization also avoids the raw
MIN_VALUE negation edge in Java 25's large linked-list rotate implementation.
Both helpers are generic, retain duplicates/null elements, support fixed-size
mutable lists and skip logical no-ops without mutation. Existing music-key
rotations retain their values and direction; the Note-only import is unnecessary.

Class locations use the qualified binary resource name, fixing nested classes.
File URLs convert through URI/Path for native decoded paths (spaces, plus signs,
Unicode, percent/hash characters); jar/module/other URLs remain intact rather
than losing an assumed file prefix. Missing/null resources return explicit null;
conversion failures include class/resource/cause diagnostics.

Package discovery now respects package boundaries and recursive subpackages,
retains trailing-dot/empty-package behavior and parent/abstract/deprecated filters,
and avoids initialization. A missing context loader falls back to the defining
loader. Guava remains for ordinary classpaths; native NeoForge ModuleClassLoader
configuration and parent configurations supply named-module resources that Guava
cannot see. Module readers/streams close, configuration diamonds and duplicate
class names are deduplicated, and scan/load IO failures retain full context.
No private reflection, transformer bypass or ASM framework is introduced.
API references: installed Java 25 Collections source and the current FML 12.0.12
ModuleClassLoader source used by NeoForge 26.3.0.76.

Five regressions cover both list types through extreme distances and bounded
writes, nulls/duplicates/fixed-size/no-op rotation, nested/file/jar/jrt locations,
encoded paths, package boundaries/subpackages/filters, actual NeoForge discovery
and parent JDK modules. Runtime fixtures compile fresh unowned packages with the
JDK compiler so NeoForge's class-loading guardian stays active and real modifiers
and annotations are tested in foreign jar loaders.
241 family units pass (187 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and all eight accepted-class
guards pass; clean exit in 2m 18s. Evidence: build/d58l-boundaries.log,
build/d58l-family-native.log and build/d58l-proof.json.

D58 remains open for other accepted-helper contracts and untranslated integration/
UI text. Checklist stays 44 complete / 119 open / 163 total. Local checkpoint only;
existing owner edits, normal Play configuration and the inactive updater remain.


## D58m — Diagnostic thread lookup and JVM utility contracts (2026-10-17)

Thread lookup no longer sizes an array from ThreadGroup.activeCount and then
ignores enumerate's actual count. That two-snapshot estimate could truncate
results or leave null slots during thread churn. The supported Java 25
Thread.getAllStackTraces snapshot supplies live platform-thread references;
name/aliveness checks return a nullable match without retaining a cache. Null
queries return null. Duplicate names remain unspecified, and a match can end or
rename after inspection. This remains an expensive diagnostic helper, with the
same platform-only scope as ThreadGroup enumeration; virtual threads are not
silently claimed to be discoverable. getTopLevelThreadGroup behavior is retained.
Installed JDK 25.0.2 Thread/ThreadGroup sources establish those scope contracts.

JVM arguments now form an immutable typed startup snapshot in VM input order,
instead of a mutable raw HashSet behind a wrapper. Presence/modifier/integer
queries retain their formats and missing-result sentinels, with deterministic
first-prefix selection in VM order. Modifier absence is explicitly nullable.
Runtime-version and client-launcher errors use contextual logger events/causes;
existing version and launcher fallback results remain. The obsolete pre-Java-9
version comment and unused reflection import are removed; current documentation
matches Runtime.version's component order. Existing heap parsing is retained.

Five regressions cover controlled live/terminated platform-thread lifecycles,
nested and duplicate-name groups, absent/null lookup, virtual-thread exclusion
and shared root-group behavior, immutable runtime argument order/presence/defaults,
and native Java version components/bounds. Tests release and join only their own
threads; there is no new gameplay hook, scheduler or global thread registry.
246 family units pass (192 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and all eight accepted-class
guards pass on NeoForge 26.3.0.76; clean exit in 1m 58s. Evidence:
build/d58m-thread-jvm.log, build/d58m-family-native.log and build/d58m-proof.json.

D58 remains open for remaining helper diagnostics and untranslated integration/
UI text; D59's broader cache/session/thread ownership audit also remains open.
Checklist stays 44 complete / 119 open / 163 total. Local checkpoint only;
existing owner edits, normal Play configuration and inactive updater are preserved.


## D58n — Publication checkpoint and reflective string-reference diagnostics (2026-10-17)

At the owner's explicit request, all outstanding changes were committed and
pushed before continuing: seven Officialy mod forks (VoidMonster master, others
26.3) and the root 26.3 branch. This includes the six previously preserved data
provider API updates, NeoForge 26.3.0.76 setting, IDE state and prior local port
checkpoints. Root checkpoint 3e5d11c was published. Follow-up root 120fa07 fixes
ReactorCraft/ChromatiCraft .gitmodules URLs to the Officialy forks that actually
host their port commits; all working trees were clean at publication. Broad
staging was initially rejected by automatic approval review under the prior
exclusion boundary; verified single-file staging was then approved under the
owner's newer all-changes instruction. No changes remain blocked from that push.

Continued locally with ReikaStringParser's reflective field/method/enum paths.
Failures now carry the complete reference and real exception cause, rather than
printing a stack trace and discarding its cause in a new RuntimeException.
InvocationTargetException is unwrapped; target Errors propagate directly. Static
members use null receivers with explicit static checks, enum/method calls use real
empty parameter lists, and Class/Enum types and map/set constructors are generic.
Public/inherited numeric lookup, declared enum methods, return-value toString,
class initialization and the int/float/double/long field contract are retained.
Unsupported field types keep the original NOT FOUND result.

Member separators are parsed from the right, preserving the existing format
while supporting binary nested-class names. Missing delimiters, names and class
parts now fail contextually instead of incidental substring/reflection errors.
Five regressions cover original and nested/inherited references, all supported
numeric types and unsupported results, missing/inaccessible/instance members,
enum constants, exact reference/cause preservation, checked/null/fatal invocation
results, malformed expressions and existing direct-format behavior.
251 family units pass (197 DragonAPI, 34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft),
zero failures/errors/skips. All 101 native fixtures, strict common MixinAudit
(30 transformed bindings, two optional absences) and all eight accepted-class
guards pass; clean exit in 1m 6s. Evidence: build/d58n-string-references.log,
build/d58n-family-native.log and build/d58n-proof.json.

The D58n continuation is a separate LOCAL checkpoint after the requested push.
D58 remains open: format-expression wrapper grammar still needs its own review,
along with remaining helper diagnostics and integration/UI translations. Checklist
stays 44 complete / 119 open / 163 total. Normal Play and inactive updater remain.
