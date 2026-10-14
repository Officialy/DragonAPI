# Retired ASM source references (D57, 2026-10-14)

These are byte-for-byte source snapshots, not Java sources, resources, runtime
transformers, or candidates for re-enabling in a build allowlist. The manifest
records the original locations and SHA-256 hashes. ChromatiCraft owns its snapshot
in `ChromatiCraft/docs/legacy-asm/`.

## Modern ownership

- NeoForge public APIs/events first; narrowly targeted Mixins when no hook exists.
- Native accessors/invokers or justified access transformers for member access.
- ASM inspection in TestInstance's MixinAudit remains useful; it does not invoke
  Reika's retired framework. Mixin's own ASM dependency must remain available.
- LuaBlock scalar conversion now uses LuaValueParser, with no ASM dependency.
- Accepted recipe caching and diagnostic callers use FMLEnvironment.isProduction()
  directly. DragonAPIMod calls its flag isDevelopment and logs development versus
  production. Neither readable member names nor an SRG probe defines development.
- ReikaObfuscationHelper's unregistered reflection maps and hardcoded development
  result are retired. Its original code and registration comments are preserved.
  Excluded ChromatiCraft source still references this historical API. Those files
  must port against native loot/events/accessors and the loader flag as their full
  subsystems land; do not restore the empty maps or introduce placeholder methods.

## Unfinished ChromatiCraft behavior references

The old ChromaASMHandler was already excluded and not running. Moving it here
changes no active gameplay behavior and does not complete these behavior ports:

| Original active patch | Behavior to preserve when its subsystem lands |
| --- | --- |
| ENDPROVIDER | End island bias via ChromaAux.getIslandBias |
| REACHDIST | Client reach adjustment via ChromaAux.getReachDistanceClient |
| COLLISIONBOXES | Custom entity/block collision boxes |
| ENTITYPUSHOUT | Client push-out rules |
| STOPSLOWFALL | Living entity slow-fall handling |
| TRANSPARENCY1–4 | Dirt, grass, liquid and Forge-fluid opacity overrides |
| TEXTURELOAD | Original sprite-load callback |
| LOREHANDLER | Lore resource stream loading |
| ROSETTAHANDLER | Rosetta resource loading |
| STOPLIGHTUPDATES | Client light-update suppression |

The snapshot also retains commented historical patches (character width,
world lighting, entity collision, ray traces, flower cache, F3 coordinates and
End sky), complete instruction bodies, descriptors and original registration.
No replacement is claimed merely because the reference was relocated. Existing
accepted Mixins remain registered and unchanged.

## Validation

See DragonAPI/PORT-AUDIT-REVIEW.md and ReactorCraft/PORTING.md for D57 results.
The archives must match the manifest; runtime jars must not contain any of the
retired classes. LuaBlock tests exercise actual map export, boxed numeric widths,
overflow fallback, malformed hints and preservation of unrelated brackets.
