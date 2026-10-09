# MIDI and music consumers — D25

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta. Source references: DragonAPI
`de1bcf1f` (`IO/ReikaMIDIReader.java`, `Instantiable/IO/MIDIInterface.java`,
`Instantiable/MusicScore.java`, `Instantiable/GUI/PianoKeyboard.java`) and RotaryCraft
`b2288638f7f4703bb4555f72a2b86f72407cc2ae` (`Items/ItemDisk.java`,
`GUIs/Machine/GuiMusic.java`, `TileEntities/Decorative/TileEntityMusicBox.java`).
The generated 26.3 Minecraft source jar supplies the current ListTag, mouse-input,
AbstractButton and GUI extraction contracts.

## Timing and compatibility

The original reader divides milliseconds by **5**, not 50. Its public conversion names
say MC ticks, but scores use 5 ms units. RotaryCraft imports with `scaleSpeed(11, true)`
and its Note adapter further divides duration by eight before selecting the nearest
original 48/24/12/6/3-tick note length. These authored conversion factors are retained.
MusicKey's existing MIDI-60-to-C5 convention is retained. Do not reinterpret this batch
as replacing either mod's music timing or tuning with a new convention.

PPQ import integrates microseconds-per-quarter tempo messages from all tracks on one
chronological timeline, rounding only after cumulative elapsed time. Default tempo is
500000 microseconds per quarter (120 BPM). Zero or malformed tempo messages are ignored;
other metadata lengths are safe. SMPTE frame-based files use their division/resolution,
including 30000/1001 drop-frame timing, independently of tempo messages. Equal-tick
ordering is stable by track index and original order inside each track.

`MIDIInterface.getLength()` retains its original **largest track event count**, including
end-of-track, rather than silently becoming duration. Restored `getNoteAtTrackAndTime`,
reader `readMIDI`, `getTrackLength` and `hasNoteOn` retain zero-based **track indices** and
the original legacy 60-MIDI-tick grid / 120-tick lead-in. Pitch queries retain their
original octave folding; absent attacks return -1 on valid tracks, invalid tracks return
zero. These compatibility queries are independent of the modern tempo-aware score path.

## Notes and representations

Score and array imports merge events chronologically, use actual MIDI channel IDs (0..15),
and retain programs separately per channel. Attacks capture their program and velocity;
later program changes cannot retimbre an existing note. Velocity-zero NOTE_ON closes a
note. Overlapping attacks of the same pitch pair FIFO with releases, rather than replacing
and losing the previous attack. Unmatched releases are ignored; unfinished attacks extend
to sequence end. Channel nine remains optional percussion. MusicKey does not represent
all 128 MIDI pitches; unsupported pitches are logged and skipped, as the original score
contract requires. File/resource readers close owned streams on success and failure.

The legacy array retains `[time][64][3]`, with channels in slots 0..15 and the other slots
reserved. Each occupied cell contains instrument/pitch/velocity. GM mapping remains
0 -> piano/1, 32 -> bass/2 (+12 semitones), 18 -> guitar/3 (-12), other programs silent.
Only attacks populate cells: previous attacks are not copied into later rests. Final-time
attacks fit in the allocation. This dense representation retains the last attack of a
channel/time chord; use MusicScore for polyphony and sparse/long sequences.

MusicScore retains one note per pitch per channel/start-time cell. Different-pitch chords
and same-pitch attacks at distinct converted times survive; same-pitch attacks quantized
to the exact same cell retain the latest inserted note, matching the authored map model.
This batch does not claim sustain/controller/pitch-bend synthesis or a new piano-roll UI.
`renderPianoRoll()` was already an empty, uncalled method in de1bcf1f; it is not a port stub.

## Score and actual consumers

NBT writes each time-indexed chord into its `Ch_n` compound and reads compounds directly
from ListTag. Counts, active tracks, range and latest start time are derived from notes
when loading, editing, transposing or copying. Copies own their chord maps; replacing a
pitch updates chord duration/count accurately. Grid alignment merges different-pitch
chords; track end time includes earlier long notes. Empty normalization is safe and speed
and grid divisors are validated. Latest position still means last **start**, preserving
its original API; track length includes note duration.

RotaryCraft disk extraction again reads original `ch0`..`ch15` note records, advances through
rests, and retains pitch, duration, voice, percussion and the authored disk volume value
128. The existing typed CUSTOM_DATA transport is retained. MIDI dispatch restores the
original clear-before-import packet, so loading a second file replaces previous music.
The music GUI again has all six authored instrument icons, including oak planks, and
routes each piano key's stored 0..48 note ID. Keyboard clicks report handling, prioritize
black keys, reject non-left/disabled/invisible input and retain the original shape-specific
highlight rectangles/fade through native GUI extraction.

ChromatiCraft's CrystalMusic/MIDIDisc consumers remain outside its accepted port slice.
Their original calls/behavior are preserved; this batch does not admit or claim completion
of those subsystems. Neither normal Play configuration nor its world directory is used
for automated checks.

## Validation

Seventeen new unit regressions exercise chronological channel programs, tempo boundaries,
fractional integration, SMPTE modes, overlapping/zero-velocity notes, percussion, unfinished
notes, metadata, legacy queries, stream closure, invalid inputs, native NBT round-tripping,
independent copies, score edits/scaling/grid alignment and original RotaryCraft disk data.
All 109 affected-module units pass: DragonAPI 67, RotaryCraft 34, ReactorCraft 8.
Log: `build/midi-validation.log` (1m 4s).

The opt-in MusicClientChecks uses the real client, registered music menu, original texture,
GuiMusic initialization/callback and native GuiGraphicsExtractor/GuiRenderState. It checks
all 49 original note IDs, black-key precedence, click rejection and highlighted geometry.
The callback records the routed ID before packet sending; this fixture does not claim
music packet delivery, audible playback, a remote server or broad music-box acceptance.
The full separate client audit passes in `build/midi-client-validation.log` (3m 24s),
including MUSIC_AUDIT_CLIENT_PASS and the existing GPU shader, blue water, model, resource,
input and request/reply checks. Packaged MIDI/score/keyboard/consumer classes are present;
normal Play VM/program argument hashes match the D24 checkpoint exactly.
