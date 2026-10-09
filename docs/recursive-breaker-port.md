# Recursive breaker fluid and sound review — D21, 26.3

Source reference: original DragonAPI `de1bcf1f`, `Auxiliary/ProgressiveRecursiveBreaker.java`.
Current API references: the generated Minecraft 26.3.0.51-beta source jar, especially `Level`,
`ServerLevel`, `ClientboundSoundPacket`, `ClientLevel` and `SoundEngine`, and the cached matching
NeoForge source jar's `PlayLevelSoundEvent` and `PlaySoundEvent`.

## Confirmed defects and source contract

The original breaker chose the swim sound for water material, the ghast-fireball sound for other
liquids, and the block's break sound for solids. The port compared the default states' property
collections by reference. This is not fluid identity and cannot distinguish water from lava.
Its liquid calls and the shared solid sound helper then called `Level.playLocalSound` during
server ticking. In 26.3 that method's base implementation remains empty; `ServerLevel` uses
`playSeededSound` for actual sound broadcasts.

The breaker now captures its actual admitted BlockState before removal. A fluid is standalone
when its legacy block identity is the state's block identity. Water-tagged source and flowing
fluids select `GENERIC_SWIM`; other standalone fluids retain `GHAST_SHOOT`. Waterlogged solids
retain their actual block sound. Solid sounds use NeoForge's state/world/position/player-aware
SoundType lookup, including its volume and pitch.

Native `Level.playSound(null, ...)` broadcasts at the block centre. Passing no excluded entity
lets the initiating player hear the server-driven operation too. NeoForge cancellation and sound,
category, volume and pitch modifications run before native distance/dimension filtering and
`ClientboundSoundPacket` delivery. Drops-disabled breaking still plays its sound; `doBreak=false`,
pass-through cells and callback-denied cells remain silent.

## Shared sound helpers and accepted callers

The same empty server method affected the common break/step/place, explicit block sound,
normal ambient and entity sound overloads. These now select local playback for a client Level
and native broadcast for a server Level. Their categories, positions, explicit/default volume
and pitch, and client distance-delay flags remain intact. Block helpers look up the actual state
when it is the named block; callers naming a removed block or a not-yet-placed replacement retain
that block's default state as their sound source.

Accepted breaker callers are ChromatiCraft's Excavation Star, Fertility Seed and modern cliff
stone transparification. Their callbacks, resource consumption, loot, breadth-first waves,
matching and update flags remain intact. The cliff callback's additional placement sound uses
the repaired shared helper. ReactorCraft cooling/extinguish effects also reach that helper.
Legacy Fluid/Transition Wand, AbilityCalls and OreOverlayRenderer files remain outside the active
ChromatiCraft source allowlist and were not treated as accepted consumers. Special tree scan
depth restoration remains the separate D22 task.

## Validation

Nine new native server regressions cover source/flowing water and lava, solids, waterlogged
solids, silent nonbreaking paths, fourteen shared helper overloads, and sound-hook cancellation
and modification. Real player connections verify exactly one expected packet to the miner and
a nearby observer, native range exclusion for a distant observer, categories and volume/pitch.
Position checks retain the native packet's float decoding precision, including distant world
origins where double-precision half-block coordinates cannot be represented by a float.

All 27 native DragonAPI tests and the 85 family unit tests pass in
`build/recursive-breaker-validation.log` (1m 7s). All fifteen accepted wand regressions pass in
`build/recursive-breaker-wand-callers.log` (2m 16s); the cliff transparification regression passes
in `build/recursive-breaker-cliff-caller.log` (39s). The isolated client audit passes in
`build/recursive-breaker-client.log` (1m 42s), including all prior water/turbine/gear/railgun/dropper
checks. Its new `SOUND_AUDIT_CLIENT_PASS` verifies local block/ambient/entity helper submission and
the decoded server breaker water sound at the real client sound engine. Normal Play's argument
files remain unchanged; it does not create audit fixtures or collect sounds. Actual remote-client
and shader/LOD acceptance remain separate open tasks.
