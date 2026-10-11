# Shared machine-block contract — D35, 2026-10-11

Source parity reference: DragonAPI upstream commit
`c29c4fcc2d3a65566783a4a163d8e682116e868c`, `Base/BlockTEBase.java` and
`Base/TileEntityBase.java`. Current API references are the generated Minecraft
26.3 / NeoForge 26.3.0.51-beta source jar and cached NeoForge 0.51 source jar.

## Mining protection

The original block's contextual hardness returned -1 when the tile's persisted
`no_mine` flag was set. Modern block states cache their destroy speed, so the shared
block checks the actual tile in `getDestroyProgress` and returns zero for protected
tiles. Otherwise it delegates to native mining progress, retaining player/tool,
harvest-check and subclass behavior. `SemiUnbreakable` exposes the same contextual
flag to shared machine-breaker queries instead of relying on cached hardness.
Absent block entities use ordinary mining behavior.

RotaryCraft's pipe override checks protection before instant mining. The already
pending `PIPEHARDNESS` implementation is included and validated in this batch:
the live synced float is clamped to 0..1; zero mines instantly, and nonzero hardness
scales native progress without replacing player/tool harvest checks. Changes to
the setting after block registration affect subsequent queries. Clearing protection
restores exactly the configured progress.

The saved `no_mine` key is preserved. It is also carried in common sync tags, so
incremental tile packets and initial chunk snapshots carry protection. Changing an
attached server tile requires the owning server thread and immediately dispatches
the existing native delta packet; unchanged values dispatch nothing. Detached
tiles can be configured without world access. Existing full-save and native
ValueInput paths retain the flag. This is the original mining restriction, not a
new claim system or a prohibition on creative/admin/code-driven world replacement.

## Comparator output

The original public `getComparatorOverride()` returns the cached `RedstoneTracker`
value. That API is restored. The existing shared lifecycle continues sampling the
authored `getRedstoneOverride()` every eight server ticks. The shared block exposes
native analog output in every direction, clamped to the legal 0..15 range; absent
tiles return zero. Existing explicitly specialized block overrides remain in use.

Changed samples notify ordinary neighbors once and invoke native
`updateNeighbourForOutputSignal`, allowing comparators to update through an adjacent
solid conductor. The previous code dispatched the same ordinary neighbor update
twice and omitted the conductor path. Comparator caches remain runtime state and
are recomputed after load rather than added as stale persisted values.

## Interactions and menu synchronization

RotaryCraft's common machine handler implemented only `useItemOn`. Native empty-hand
interaction instead calls `useWithoutItem`, so the original empty-hand machine
actions and GUI opening were unreachable. The common empty-hand hook now enters the
same item handler with `ItemStack.EMPTY`; existing subclass overrides, crouch rules,
access checks and concrete action/sync behavior remain authoritative. A passive
failure in that item handler becomes PASS for empty-hand use, retaining ordinary
interaction fall-through; passive pipe clicks do not dispatch full tile NBT.

The original blanket full-NBT broadcast on every right-click remains superseded by
the modern menu and tile synchronization paths. Slots use native menu content/slot
packets; registered DataSlots carry fields such as Blast Furnace progress and
temperature and Crystal Brewer time. Rotary IO torque/speed/power and Fractionator
progress remain in their shared/subclass periodic sync tags, while Reactor CPU view
data has an explicit opening snapshot and subsequent change payloads. No missing
base-level menu field requires reintroducing the click-wide broadcast. This review
does not certify every excluded container or every specialized screen.

## Verification boundary

Six native fixtures cover actual registered Rotary machines, Reactor coolant cells,
Electri batteries and Chroma brewers; mining flag set/clear/native save-load;
direct and conductor-mediated comparator transitions; incremental/initial native
flag packets and worker rejection; live pipe-hardness settings; and actual empty-hand
Blast Furnace menu inventory/data opening and subsequent data changes through a
player connection without a redundant full tile packet. The test observes both
vanilla DataSlot packets and NeoForge's negotiated full-width DataSlot payloads.
Tests restore the in-memory
pipe setting without writing a TOML file.

Final commands/results and checkpoint scope are recorded in
`TestInstance/docs/machine-block-contract-2026-10-11.md` in the parent repository.
The headless connection is a native GameTest mock; real authenticated remote-client
and graphical-screen acceptance remain separate tasks.
