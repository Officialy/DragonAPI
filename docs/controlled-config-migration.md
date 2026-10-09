# ControlledConfig format IDs and safe legacy resets

The historical source at DragonAPI commit `cc8a1c0b` accepted a config format ID in the
constructor. It stored that ID under `Control` / `Config ID - Edit to have your config
auto-deleted` and regenerated the config when the stored ID differed. Later original
source removed the constructor argument and disabled the check; the port inherited the
disabled check and an unsafe, empty backup report followed by file deletion.

The 26.3 API supports an explicit format ID again:

```java
new ControlledConfig(mod, options, ids, LEGACY_CONFIG_FORMAT_ID);
```

The existing three-argument constructor remains unversioned. None of the current family
callers has been assigned a guessed format ID. A format ID represents an intentional legacy
schema/default reset policy; a mod update or Minecraft version change is not itself a reset
request. The old property name is retained for compatibility, but its comment describes the
new archival behavior accurately.

On initialization and reload, a versioned config checks the marker before orphan cleanup,
option validation and saving. An absent marker is initialized to the declared ID without
resetting custom values. A matching marker retains those values. A different integer marker
triggers the following behavior:

1. Copy every byte of the current primary `.cfg` to a new sibling
   `<filename>.reset-backup-<UUID>`. Previous backups are never replaced.
2. Clear the parsed legacy categories/children and restore registered additional options'
   original defaults, rather than their previously loaded values.
3. Install the current marker, load the primary option/ID defaults and retained segmented
   values, and save the regenerated legacy config. The marker survives cleanup even when a
   mod also declares ordinary options in the `Control` category.

The protected `resetConfigFile()` API performs the same preservation and regeneration
explicitly, without recursive initialization or duplicate spec registration. A malformed
marker or a failed backup aborts before clearing settings or overwriting the original file.
The integer getter's automatic repair is deliberately avoided when validating the marker.

Before the legacy parser first normalizes an existing file, `initProps()` preserves its exact
bytes in the existing `<filename>.pre-toml` migration backup. That first backup is retained
across subsequent resets. A non-file source or non-file migration backup fails clearly.

Existing client/synced TOML specs remain registered and authoritative. A legacy reset never
clears a loaded TOML spec, changes its values, edits a TOML file, or derives a new reset request
from a mod release. Reset legacy values can become migration defaults when no TOML exists;
they do not replace values from a loaded TOML file. Segmented values are loaded from their
own files and are no longer overwritten by a second read from the primary config. Their
existing normalization/save behavior remains part of normal legacy loading.

Seven focused regressions in `ControlledConfigTest` exercise absent/matching/mismatched IDs,
shared Control categories, exact migration/reset backups, repeated explicit resets, additional
defaults, unversioned callers, malformed markers, preservation failures, real segmented values,
and an actual loaded `ModConfigSpec` populated from TOML. Test fixtures own their specs and
do not register test configs on the real mod container.

Validation: `build/config-reset-validation.log` passes the family unit suites, changed library
jar/TestInstance compilation, and all 13 native DragonAPI GameTests (1m 45s). The final shared
Control-category correction is verified by the full DragonAPI unit suite and jar build in
`build/config-reset-final.log`. Results total 81 unit tests: DragonAPI 37, RotaryCraft 33,
ReactorCraft 8, ChromatiCraft 3. Remote-client config reload/reconnect acceptance remains
tracked separately as N07.
