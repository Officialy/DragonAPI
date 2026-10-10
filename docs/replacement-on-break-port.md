# Replacement on player removal — D33

The port previously set the world position to the old state on every player removal.
The original DragonAPI `de1bcf1f:Base/BlockReplaceOnBreak.java` is abstract and delegates
replacement identity and metadata to two abstract hooks. Its nested self-replacing
policy explicitly chooses itself and the same metadata. There are no other original
or accepted project subclasses/callers in the current seven-mod source trees.

The modern base is abstract again. Its final native onDestroyedByPlayer callback
requests a complete replacement BlockState from the subclass and installs it using
the native UPDATE_ALL flags, preserving every selected state property. The hook
receives the actual level, position, old state, player, tool, harvest flag and fluid
context. It does not guess a default replacement, silently return the original state,
or create a new loot path. A null subclass result reports the class and position.

The original nested self-replacing policy is restored as BlockReplaceSelfOnBreak.
It retains the exact old state and the native setBlock return value. It is static
because the original enclosing instance was not used by the policy; its protected
constructor remains suitable for concrete subclasses.

Generated Minecraft 26.3.0.51-beta ServerPlayerGameMode captures the old block/entity
before invoking this hook. After successful removal it calls destroy and then
playerDestroy on the original block/state, with the old entity and copied tool.
The replacement callback therefore installs the chosen state without manually
harvesting or dropping the replacement, avoiding duplicate or wrong-block drops.
Canceled break events and player restrictions run before the hook.

This is a full port of the generic original API and self policy, compiled against
the current native signature. There is no accepted concrete consumer to exercise
in a production world. Validation is compilation and affected unit suites;
actual gameplay/drop acceptance must accompany a future concrete replacement block.
The legacy two-hook metadata API becomes the complete-state hook when such a
consumer is ported. No invented concrete block, recipe or loot table is added here.
