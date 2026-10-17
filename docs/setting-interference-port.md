# Setting interference warnings on 26.3

D05 restores the original client setting warnings without changing the player's
settings. The original contract and icon are from DragonAPI `de1bcf1f`,
`Auxiliary/Trackers/SettingInterferenceTracker.java` and `Resources/mutewarn.png`.
Chroma's original client registered the mute handler; the active modern Chroma
client now makes that registration during queued client setup.

## Detection and presentation

The original important sound categories remain MASTER, AMBIENT, BLOCKS and
PLAYERS. Any one below 0.1 triggers the mute interference; 0.1 itself is allowed.
Muting music alone does not trigger it. Detection reads the current options on
each HUD extraction, so changing a slider updates the reminder immediately.

`RegisterGuiLayersEvent` registers a physical-client layer immediately below
TITLE. Login popups draw after TITLE, so their text covers the reminder while the
popup is open. Once dismissed, reminders remain at the original `(2, 2)` corner,
16 pixels square with four pixels between icons. Handler order is deterministic;
duplicate IDs keep the first registration. Irrelevant or inactive handlers do
not draw. Hiding the HUD with F1 also hides the reminder explicitly: custom
NeoForge layers do not inherit a vanilla layer's visibility predicate.

The original unmodified 32x32 mute texture is extracted with full UVs through
`GuiGraphicsExtractor` and `RenderPipelines.GUI_TEXTURED`. Its SHA-256 is
`872b6dbb10d1ec20e7b32ec1d956eddbaa2a6ac97785ec5cd69f6851b8f5aac6`.
Icons use the supplied native GUI context and isolated pose pushes/pops, with no
fixed-function lighting, global texture binding or profiler render hook.

The extension interface's `drawIcon` parameter is now `GuiGraphicsExtractor`.
The old context-free `onRender`/profiler entry point is replaced by
`onRender(GuiGraphicsExtractor)`. All in-tree accepted consumers use this coherent
modern rendering contract; the excluded historical Chroma client stays intact.

## Login, lifecycle and persistence

The real `ClientLoginEvent` now invokes the tracker. The legacy LOGIN packet
posts the same event and no longer calls the tracker a second time. The tracker
deduplicates notifications for the local player's current session and clears that
identity on `ClientLogoutEvent`, without retaining a player or level reference.
The original policy names and public `WarningPersistence.isActive` API remain.
The configured policy is read at login, rather than frozen before config loading.

- EVERYLOAD evaluates on every new login.
- SETTINGCHANGE compares the registered interfering ID set with the last cache.
- VERSION compares the actual client-loaded mod versions with its own cache.
- ONCE warns only when the cache has not yet been created.

All policies evaluate a first/missing cache. VERSION also evaluates an old cache
without version metadata once during migration. This deliberately corrects its
dependence on integrated-server version state, which was unavailable on a remote
client and could retain information from another world. It performs no update
feed request; the port updater remains inactive.

As in the original, login descriptions cover all interfering settings, including
ones not relevant at that instant; the icons additionally require current
relevance. The header and mute description are generated translations. The header
now mentions the actual persistent top-left reminder.

The original `DragonAPI/setting_interference_cache.dat` identity and plain enabled
ID lines remain. UTF-8 comment lines add client version metadata and remain
compatible with old readers. Unknown/unregistered IDs and malformed metadata
comments do not crash login. Writes use a unique sibling file and atomic replace
where supported, with a complete-file replacement fallback. A read failure logs
the full cause, shows warnings and preserves the unreadable file; failed writes
are logged and cannot erase the prior cache by deleting it first.

## Acceptance

Pure Java regressions exercise all policy transitions, old cache input, UTF-8,
deterministic round trips, complete replacement, replacement failure cleanup and
immutable/validated snapshots. The opt-in native client checks real Chroma
registration, sound thresholds, original icon geometry/UVs, F1, handler filtering,
translated popups, session deduplication/logout and active/cleared HUD frames. It
restores options, HUD visibility and existing popup state before continuing other
sound/render checks. Normal Play does not run these fixtures.

Final run evidence is recorded in `PORT-AUDIT-REVIEW.md`,
`ReactorCraft/PORTING.md`, `ChromatiCraft/PORTING.md` and `PROJECT-BACKLOG.md`.
