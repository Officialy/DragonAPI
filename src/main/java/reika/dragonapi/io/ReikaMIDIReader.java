/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.io;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.MusicScore;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

import javax.sound.midi.*;
import java.io.*;
import java.util.*;

public final class ReikaMIDIReader {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final int INSTRU_CHANGE = 0xC0;
    public static final int TEMPO = 0x51;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"};
    public static final int MIDI_C5 = 60;

    // Original score units are 5 ms. Existing consumers apply their own speed/length scaling.
    private static final double MICROS_PER_SCORE_TICK = 5000;
    private static final int DEFAULT_TEMPO = 500000;

    private ReikaMIDIReader() {}

    public static Sequence getMIDIFromFile(Class<?> root, String path) {
        DragonAPI.LOGGER.info("Reading MIDI at {}", path);
        try (InputStream input = root.getResourceAsStream(path)) {
            if (input == null) {
                DragonAPI.LOGGER.error("File at {} not found. Aborting.", path);
                return null;
            }
            return MidiSystem.getSequence(input);
        } catch (IOException | InvalidMidiDataException error) {
            DragonAPI.LOGGER.error("MIDI file at {} unreadable or invalid", path, error);
            return null;
        }
    }

    public static Sequence getMIDIFromFile(File file) {
        try {
            return getMIDIFromFile(new FileInputStream(file));
        } catch (IOException | InvalidMidiDataException error) {
            DragonAPI.LOGGER.error("MIDI file at {} unreadable or invalid", file, error);
            return null;
        }
    }

    /** Takes ownership of and closes the supplied stream, including on parse failure. */
    public static Sequence getMIDIFromFile(InputStream input) throws IOException, InvalidMidiDataException {
        try (input) {
            return MidiSystem.getSequence(input);
        }
    }

    /**
     * Legacy track-indexed query on the original 60-MIDI-tick grid, with a 120-tick lead-in.
     * task 0 returns the original folded pitch, 1 the channel program, 2 the attack velocity.
     * A valid track with no matching attack returns -1 for pitch/velocity; invalid tracks return 0.
     */
    public static int readMIDI(Sequence sequence, int track, int time, int task) {
        if (!validTrack(sequence, track)) return 0;
        long tick = (long) time * 60 + 120;
        int[] programs = new int[16];
        Arrays.fill(programs, -1);
        int voice = -1, note = -1, velocity = -1;
        for (MidiEvent event : orderedEvents(sequence)) {
            if (event.getTick() > tick) break;
            if (event.getMessage() instanceof ShortMessage message) {
                if (message.getCommand() == INSTRU_CHANGE) programs[message.getChannel()] = message.getData1();
            }
        }
        Track selected = sequence.getTracks()[track];
        for (int i = 0; i < selected.size(); i++) {
            MidiEvent event = selected.get(i);
            if (event.getTick() > tick) break;
            if (event.getMessage() instanceof ShortMessage message) {
                voice = programs[message.getChannel()];
                if (event.getTick() == tick && isAttack(message)) {
                    int octave = message.getData1() / 12 - 1;
                    while (octave > 5) octave -= 5;
                    note = message.getData1() % 12;
                    if (octave == 5 && note != 0) octave = 0;
                    note += 12 * octave;
                    velocity = message.getData2();
                }
            }
        }
        return switch (task) { case 0 -> note; case 1 -> voice; case 2 -> velocity; default -> 0; };
    }

    public static int getTrackLength(Sequence sequence, int track) {
        return validTrack(sequence, track) ? checkedTick(sequence.getTracks()[track].ticks() / 60 + 1) : 0;
    }

    public static boolean hasNoteOn(Sequence sequence, int track, int time) {
        if (!validTrack(sequence, track)) return false;
        long tick = (long) time * 60 + 120;
        Track selected = sequence.getTracks()[track];
        for (int i = 0; i < selected.size(); i++) {
            MidiEvent event = selected.get(i);
            if (event.getTick() > tick) break;
            if (event.getTick() == tick && event.getMessage() instanceof ShortMessage message && isAttack(message)) return true;
        }
        return false;
    }

    private static boolean validTrack(Sequence sequence, int track) {
        return sequence != null && track >= 0 && track < sequence.getTracks().length;
    }

    public static void debugMIDI(Sequence sequence) {
        if (sequence == null) {
            DragonAPI.LOGGER.error("Debugged MIDI is null!");
            return;
        }
        int trackNumber = 0;
        for (Track track : sequence.getTracks()) {
            DragonAPI.LOGGER.info("Track {}: size = {}", ++trackNumber, track.size());
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof ShortMessage message) {
                    int pitch = message.getData1();
                    DragonAPI.LOGGER.info("@{} channel {} command {}: {}{} key={} value={}", event.getTick(),
                            message.getChannel(), message.getCommand(), NOTE_NAMES[pitch % 12], pitch / 12 - 1,
                            pitch, message.getData2());
                } else DragonAPI.LOGGER.info("@{} {}", event.getTick(), event.getMessage().getClass().getSimpleName());
            }
        }
    }

    /** Original API: largest track event count, including its end-of-track event; not duration. */
    public static int getMidiLength(Sequence sequence) {
        if (sequence == null) return 0;
        int length = 0;
        for (Track track : sequence.getTracks()) length = Math.max(length, track.size());
        return length;
    }

    /** Original 5 ms score units; these are not native 50 ms game ticks. */
    public static int MIDITickToMCTick(Sequence sequence, long tick, float bpm) {
        return checkedTick(tick * microsPerTick(sequence, bpm) / MICROS_PER_SCORE_TICK);
    }

    public static long MCTickToMIDITick(Sequence sequence, int tick, float bpm) {
        if (tick < 0) throw new IllegalArgumentException("Negative score time");
        double micros = microsPerTick(sequence, bpm);
        return sequence.getDivisionType() == Sequence.PPQ
                ? (long) (tick * MICROS_PER_SCORE_TICK * sequence.getResolution() * (double) bpm / 60000000D)
                : (long) (tick * MICROS_PER_SCORE_TICK / micros);
    }

    private static double microsPerTick(Sequence sequence, float bpm) {
        if (!(bpm > 0) || !Float.isFinite(bpm)) throw new IllegalArgumentException("Tempo must be finite and positive");
        return sequence.getDivisionType() == Sequence.PPQ ? 60000000D / (sequence.getResolution() * (double) bpm)
                : 1000000D / (framesPerSecond(sequence) * sequence.getResolution());
    }

    private static double framesPerSecond(Sequence sequence) {
        return sequence.getDivisionType() == Sequence.SMPTE_30DROP ? 30000D / 1001 : sequence.getDivisionType();
    }

    /**
     * Original array shape [score time][64][instrument, pitch, velocity]. MIDI channels occupy
     * slots 0..15; the other slots remain reserved. A channel/time cell can retain only the last
     * attack of a chord. Use fillToScore for polyphony. Unsupported GM voices remain silent.
     */
    public static int[][][] readMIDIFileToArray(Sequence sequence) {
        if (sequence == null) return new int[0][64][3];
        List<MidiEvent> events = orderedEvents(sequence);
        Timeline timeline = new Timeline(sequence, events);
        int[][][] result = new int[Math.addExact(timeline.time(sequence.getTickLength()), 1)][64][3];
        int[] programs = new int[16];
        for (MidiEvent event : events) {
            if (!(event.getMessage() instanceof ShortMessage message)) continue;
            int channel = message.getChannel();
            if (message.getCommand() == INSTRU_CHANGE) programs[channel] = message.getData1();
            else if (isAttack(message)) {
                int instrument = getNoteblockFromGM(programs[channel]);
                if (instrument == 0) continue;
                int pitch = message.getData1() - 24 + (instrument == 2 ? 12 : instrument == 3 ? -12 : 0);
                int[] cell = result[timeline.time(event.getTick())][channel];
                cell[0] = instrument;
                cell[1] = pitch;
                cell[2] = message.getData2();
            }
        }
        return result;
    }

    public static MusicScore readMIDIFileToScore(Sequence sequence, boolean readPercussion) {
        MusicScore result = new MusicScore(16);
        if (sequence == null) return result;
        List<MidiEvent> events = orderedEvents(sequence);
        Timeline timeline = new Timeline(sequence, events);
        int[] programs = new int[16];
        // FIFO pairing retains repeated, overlapping attacks of the same pitch.
        Map<Integer, ArrayDeque<Attack>> active = new HashMap<>();
        for (MidiEvent event : events) {
            if (!(event.getMessage() instanceof ShortMessage message)) continue;
            int channel = message.getChannel();
            if (channel == 9 && !readPercussion) continue;
            int identity = channel * 128 + message.getData1();
            if (message.getCommand() == INSTRU_CHANGE) programs[channel] = message.getData1();
            else if (isAttack(message)) {
                active.computeIfAbsent(identity, ignored -> new ArrayDeque<>()).addLast(
                        new Attack(event.getTick(), message.getData1(), programs[channel], message.getData2()));
            } else if (message.getCommand() == NOTE_OFF || message.getCommand() == NOTE_ON) {
                ArrayDeque<Attack> notes = active.get(identity);
                if (notes != null && !notes.isEmpty()) addNote(result, timeline, channel, notes.removeFirst(), event.getTick());
            }
        }
        // Unterminated notes sound through the end of the sequence rather than disappearing.
        active.forEach((identity, notes) -> notes.forEach(note -> addNote(result, timeline, identity / 128, note, sequence.getTickLength())));
        return result;
    }

    private static void addNote(MusicScore result, Timeline timeline, int channel, Attack attack, long end) {
        MusicKey key = MusicKey.getKeyFromMIDI(attack.pitch());
        if (key == null) {
            DragonAPI.LOGGER.warn("MIDI pitch {} is outside MusicKey's supported range", attack.pitch());
            return;
        }
        int start = timeline.time(attack.tick());
        result.addNote(start, channel, key, attack.voice(), attack.velocity(), timeline.time(end) - start, channel == 9);
    }

    private static boolean isAttack(ShortMessage message) {
        return message.getCommand() == NOTE_ON && message.getData2() > 0;
    }

    private static List<MidiEvent> orderedEvents(Sequence sequence) {
        List<MidiEvent> result = new ArrayList<>();
        for (Track track : sequence.getTracks()) for (int i = 0; i < track.size(); i++) result.add(track.get(i));
        // Stable sort: equal-tick events retain track order and order inside each track.
        result.sort(Comparator.comparingLong(MidiEvent::getTick));
        return result;
    }

    private static int checkedTick(double tick) {
        if (!Double.isFinite(tick) || tick < 0 || tick > Integer.MAX_VALUE) throw new IllegalArgumentException("MIDI time exceeds score range: " + tick);
        return (int) tick;
    }

    private record Attack(long tick, int pitch, int voice, int velocity) {}
    private record TempoPoint(double micros, int microsPerQuarter) {}

    private static final class Timeline {
        private final Sequence sequence;
        private final TreeMap<Long, TempoPoint> tempos = new TreeMap<>();

        private Timeline(Sequence sequence, List<MidiEvent> events) {
            this.sequence = sequence;
            tempos.put(0L, new TempoPoint(0, DEFAULT_TEMPO));
            if (sequence.getDivisionType() != Sequence.PPQ) return;
            for (MidiEvent event : events) {
                if (!(event.getMessage() instanceof MetaMessage message) || message.getType() != TEMPO) continue;
                byte[] bytes = message.getData();
                if (bytes.length != 3) continue;
                int value = (bytes[0] & 255) << 16 | (bytes[1] & 255) << 8 | bytes[2] & 255;
                if (value == 0) continue;
                var previous = tempos.floorEntry(event.getTick());
                double micros = previous.getValue().micros() + (event.getTick() - previous.getKey())
                        * (double) previous.getValue().microsPerQuarter() / sequence.getResolution();
                tempos.put(event.getTick(), new TempoPoint(micros, value));
            }
        }

        private int time(long tick) {
            if (sequence.getDivisionType() != Sequence.PPQ)
                return checkedTick(tick * 1000000D / (framesPerSecond(sequence) * sequence.getResolution()) / MICROS_PER_SCORE_TICK);
            var tempo = tempos.floorEntry(tick);
            return checkedTick((tempo.getValue().micros() + (tick - tempo.getKey())
                    * (double) tempo.getValue().microsPerQuarter() / sequence.getResolution()) / MICROS_PER_SCORE_TICK);
        }
    }

    public static int getNoteblockFromGM(int voice) {
        return switch (voice) { case 0 -> 1; case 32 -> 2; case 18 -> 3; default -> 0; };
    }
}
