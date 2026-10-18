# Popup warnings on Minecraft 26.3

`PopupWriter` is a common warning service. It no longer extends a client screen,
and the mutable public `list` and `ungrabbed` fields are gone. Client presentation
is owned by `PopupClient` and a fresh `PopupScreen` for each opening. Common
server callers can register and send warnings without loading `Screen`.

The original contract is in DragonAPI commit `de1bcf1f`,
`Auxiliary/PopupWriter.java`: queued client warnings are displayed in order;
holding Ctrl releases the cursor; only the left mouse button dismisses the
displayed close target; server warnings are sent once per player; warning
equality and hashing use text, independently of width. The current port preserves
these semantics and the `POPUP` wire ID/text/width payload. Width clamping remains
96–300 GUI pixels, as in the preceding accepted port. No save format or warning
identity has changed.

Use `PopupWriter.instance()` with these operations:

- `addMessage(String)` / `addMessage(Warning)` retain side-aware registration.
  Logical server calls, including integrated-server calls, register server
  warnings. Client calls append the original Ctrl instruction once.
- `addServerMessage(Warning)` explicitly registers a server warning, including
  callers without a server-thread logical-side context.
- `pendingMessages()` returns an immutable snapshot of formatted client warnings.
- `enqueue(Warning)` queues an already formatted warning, including restoring
  a snapshot without appending the instruction a second time.
- `currentMessage()` returns the current occurrence's identity and warning.
  `dismiss(Message)` succeeds only for that exact occurrence. Identical text,
  equal warnings, or even reusing the same Warning object do not share occurrence
  identity. A stale click cannot dismiss a successor.
- `dismissFirst()` supports deliberate programmatic dismissal; `clearMessages()`
  clears the client queue. `PopupClient.clearSession()` also closes an owned
  popup screen and is called by the existing native logout hook.

The synchronized service has no geometry or cursor flags. Its server delivery
history is scoped by MinecraftServer and player UUID, and discarded when that
server stops. Registered server warnings remain available to future servers,
matching the original process-lifetime registration contract. Native player
login delivers them automatically. Text-based duplicate suppression is preserved.

The named client HUD layer `dragonapi:popup_warnings` is above TITLE. An open
popup screen extracts the warning itself, so it is drawn once. The screen owns
its layout, invalidates it after a dismissal/removal, and accepts input only after
extraction for the still-current occurrence. Hidden HUD, non-left buttons, missing
Ctrl, outside clicks and stale layouts cannot dismiss anything. GUI coordinates
are used directly; SDL's `InputConstants.MOUSE_BUTTON_LEFT` is authoritative on
26.3. Opening while another screen is active leaves that screen intact. Ctrl
release, loss of held Ctrl on a later tick, Escape, final dismissal and logout
all use native screen ownership to restore cursor handling. Other key presses
do not toggle a shared cursor flag or reopen the popup after Escape.

The development test item, recipe/logout cleanup and setting-warning fixture
use the new operations. Client packet, settings, asset and inactive updater
callers retain `addMessage`. The updater remains inactive per owner instruction.

`PopupWriterTest` covers immutable snapshots, repeated warning occurrences,
stale identity after clear/replacement, ordered dismissal and legacy text/width
identity. `PopupClientChecks` uses a real integrated-server packet, actual native
screen ownership, SDL physical key state and GUI render-state extraction to check
the presentation/input contract. Its synthetic key bytes, screen, queue and HUD
state are restored before other audit fixtures. It also observes native logout
cleanup. Normal Play does not run this fixture.
