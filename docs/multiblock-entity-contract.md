# Multiblock entity factory review — D32

The historical `BlockMultiBlock.hasTileEntity` and `createTileEntity` methods did not
override native 26.3 hooks and had no accepted callers. They are removed. The base
remains an ordinary Block: making every casing an EntityBlock would invent entities
for casings that identify their controller elsewhere.

All six accepted ReactorCraft casing families were reviewed. Generator and flywheel
use named PART states. Heater, injector and solenoid casing identities use registered
individual blocks. None hosts a local block entity. Their controller lookup remains
`getTileEntityForPosition` and is not an entity factory.

Turbine casing is the actual exception: it already implements native EntityBlock, and
its `newBlockEntity(BlockPos, BlockState)` creates TileEntitySteamInjector only for
TurbinePart.ELEMENT. Shell and housing deliberately return no entity.

The real defect was a same-block variant transition. Native BlockEntityType validation
only recognizes the turbine block identity, so changing ELEMENT to SHELL left the
injector and lubricant capability attached to a shell. The injector now overrides
native `isValidBlockState` to require ELEMENT as well as the registered block type.
Minecraft's actual LevelChunk setter then removes it and creates no replacement for
shell/housing. Its warning about the old mismatched entity during this deliberate
transition accompanies the correct cleanup; it is not a test failure.

FORMED changes keep the same element entity and tank. Changing back to ELEMENT creates
fresh empty storage. Native NBT loading retains tank contents, and removing the block
clears its entity. No generic fake entity factory or fallback stub is introduced.

Sources: DragonAPI `de1bcf1f:Base/BlockMultiBlock.java`, current accepted ReactorCraft
casings and the generated Minecraft 26.3.0.51-beta LevelChunk/BlockEntity source.
Two native tests cover the real registered factories across all casing families,
same-block part changes, fluid capability cleanup and native persistence. Both pass
with all 127 affected-module unit tests in `build/multiblock-contract-final.log` (45s).

This resolves the historical entity-hook finding. Full multiblock assembly/teardown,
drop/catalogue migration and formed hardness behavior are separate gameplay work;
these fixtures do not claim acceptance for those paths.
