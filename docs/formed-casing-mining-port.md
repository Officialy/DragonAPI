# Formed casing mining progress

The original `de1bcf1f:Base/BlockMultiBlock.java` multiplies block hardness by four
for formed metadata (`meta >= 8`). ReactorCraft's original `59558370` multiblock
base sets hardness 2 and explosion resistance 10. Current accepted casing
registrations retain those positive base properties.

The port instead multiplied native `getDestroyProgress` by four. Minecraft 26.3
computes this progress as player mining speed divided by hardness and the native
harvest modifier. The multiplier therefore inverted the source behavior, making
formed casings four times faster to mine rather than four times slower.

The formed path now divides native progress by four. Unformed progress, native tool,
player/effect and harvest checks, block registration properties and explosion
resistance remain native. No global block-state ID or metadata compatibility table
is introduced. The native state hardness property is static; this correction applies
the original formed multiplier at the actual player-mining hook.

The native regression iterates every accepted casing state in all six original
families (generator, flywheel, turbine, solenoid, heater and injector), including
each separately registered casing part. It confirms original base hardness 2,
positive finite unformed progress, the native harvest formula, and formed progress
exactly one quarter as large with bare hands and an iron pickaxe. It places actual
registered states in a server level, rather than using zero-hardness dummy blocks.
The existing entity-factory and part-transition tests also pass.
Evidence: `build/formed-casing-mining-validation.log`.

Full structure assembly/teardown, survival drop catalogues and D35's separate
BlockTEBase hardness/comparator/menu contracts retain their own acceptance work.
