package reika.dragonapi.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.sound.midi.*;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.MusicScore;
import reika.dragonapi.instantiable.io.MIDIInterface;
import static org.junit.jupiter.api.Assertions.*;

class MIDIReaderTest {
    @Test void channelProgramsAreChronologicalAcrossTracks() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track notes = sequence.createTrack(), programs = sequence.createTrack();
        event(programs, 0, ShortMessage.PROGRAM_CHANGE, 2, 32, 0);
        event(programs, 240, ShortMessage.PROGRAM_CHANGE, 2, 18, 0);
        event(programs, 0, ShortMessage.PROGRAM_CHANGE, 3, 81, 0);
        event(notes, 120, ShortMessage.NOTE_ON, 2, 60, 75);
        event(notes, 480, ShortMessage.NOTE_OFF, 2, 60, 0);
        event(notes, 480, ShortMessage.NOTE_ON, 2, 64, 100);
        event(notes, 960, ShortMessage.NOTE_OFF, 2, 64, 0);
        event(notes, 120, ShortMessage.NOTE_ON, 3, 67, 90);
        event(notes, 480, ShortMessage.NOTE_OFF, 3, 67, 0);
        MusicScore score = new MIDIInterface(sequence).fillToScore(true);
        assertEquals(32, note(score, 2, 25).voice);
        assertEquals(75, note(score, 2, 25).volume);
        assertEquals(75, note(score, 2, 25).length);
        assertEquals(18, note(score, 2, 100).voice);
        assertEquals(81, note(score, 3, 25).voice);
        assertEquals(3, score.noteCount());
    }

    @Test void tempoChangesPreserveDurationsAcrossTrackBoundaries() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track notes = sequence.createTrack(), tempo = sequence.createTrack();
        event(notes, 0, ShortMessage.NOTE_ON, 0, 60, 64);
        event(notes, 960, ShortMessage.NOTE_OFF, 0, 60, 0);
        tempo(tempo, 480, 1000000);
        assertEquals(300, note(new MIDIInterface(sequence).fillToScore(false), 0, 0).length);
    }

    @Test void fractionalTempoSegmentsAreRoundedOnlyAfterIntegration() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        event(track, 0, ShortMessage.NOTE_ON, 0, 60, 64);
        for (int tick = 1; tick <= 48; tick++) tempo(track, tick, 500001);
        event(track, 48, ShortMessage.NOTE_OFF, 0, 60, 0);
        assertEquals(10, note(new MIDIInterface(sequence).fillToScore(false), 0, 0).length);
    }

    @Test void zeroVelocityEndsNotesAndOverlappingAttacksPairInOrder() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        event(track, 0, ShortMessage.NOTE_OFF, 0, 60, 0); // unmatched release
        event(track, 0, ShortMessage.NOTE_ON, 0, 60, 50);
        event(track, 240, ShortMessage.NOTE_ON, 0, 60, 80);
        event(track, 480, ShortMessage.NOTE_ON, 0, 60, 0);
        event(track, 960, ShortMessage.NOTE_OFF, 0, 60, 0);
        MusicScore score = new MIDIInterface(sequence).fillToScore(true);
        assertEquals(2, score.noteCount());
        assertEquals(100, note(score, 0, 0).length);
        assertEquals(150, note(score, 0, 50).length);
        assertEquals(80, note(score, 0, 50).volume);
    }

    @Test void unfinishedNotesAndPercussionRetainTheirContract() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        event(track, 0, ShortMessage.NOTE_ON, 9, 60, 64);
        event(track, 0, ShortMessage.NOTE_ON, 1, 64, 64);
        event(track, 480, ShortMessage.NOTE_OFF, 9, 60, 0);
        MusicScore score = new MIDIInterface(sequence).fillToScore(true);
        assertTrue(note(score, 9, 0).percussion);
        assertFalse(note(score, 1, 0).percussion);
        assertEquals(100, note(score, 1, 0).length);
        assertNull(new MIDIInterface(sequence).fillToScore(false).getTrack(9));
    }

    @Test void smpteUsesFrameTimeRegardlessOfTempo() throws Exception {
        for (float division : new float[]{Sequence.SMPTE_24, Sequence.SMPTE_25, Sequence.SMPTE_30, Sequence.SMPTE_30DROP}) {
            Sequence sequence = new Sequence(division, 80);
            Track track = sequence.createTrack();
            tempo(track, 0, 1000000);
            event(track, 0, ShortMessage.NOTE_ON, 0, 60, 64);
            event(track, 2400, ShortMessage.NOTE_OFF, 0, 60, 0);
            double fps = division == Sequence.SMPTE_30DROP ? 30000D / 1001 : division;
            int expected = (int)(2400 * 200D / (fps * 80));
            assertEquals(expected, note(new MIDIInterface(sequence).fillToScore(true), 0, 0).length);
            assertEquals(expected, ReikaMIDIReader.MIDITickToMCTick(sequence, 2400, 120));
        }
    }

    @Test void arrayUsesChannelsNoGhostAttacksAndHandlesAllMetadataLengths() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        meta(track, 0, 3, new byte[]{1});
        meta(track, 0, ReikaMIDIReader.TEMPO, new byte[]{1});
        tempo(track, 0, 0);
        event(track, 0, ShortMessage.PROGRAM_CHANGE, 15, 32, 0);
        event(track, 0, ShortMessage.NOTE_ON, 15, 60, 100);
        event(track, 240, ShortMessage.NOTE_ON, 1, 64, 90);
        event(track, 480, ShortMessage.NOTE_ON, 15, 60, 0);
        int[][][] array = new MIDIInterface(sequence).fillToArray();
        assertEquals(101, array.length);
        assertEquals(64, array[0].length);
        assertArrayEquals(new int[]{2,48,100}, array[0][15]);
        assertArrayEquals(new int[]{1,40,90}, array[50][1]);
        assertArrayEquals(new int[3], array[50][15]);
        assertArrayEquals(new int[3], array[100][15]);
    }

    @Test void originalLegacyQueriesAndEventCountRemainAvailable() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 480);
        Track track = sequence.createTrack();
        event(track, 0, ShortMessage.PROGRAM_CHANGE, 0, 32, 0);
        event(track, 120, ShortMessage.NOTE_ON, 0, 60, 90);
        event(track, 180, ShortMessage.NOTE_ON, 0, 60, 0);
        MIDIInterface midi = new MIDIInterface(sequence);
        assertEquals(48, midi.getNoteAtTrackAndTime(0,0));
        assertEquals(32, ReikaMIDIReader.readMIDI(sequence,0,0,1));
        assertEquals(90, ReikaMIDIReader.readMIDI(sequence,0,0,2));
        assertTrue(ReikaMIDIReader.hasNoteOn(sequence,0,0));
        assertFalse(ReikaMIDIReader.hasNoteOn(sequence,0,1));
        assertEquals(-1, midi.getNoteAtTrackAndTime(0,1));
        assertEquals(4, ReikaMIDIReader.getTrackLength(sequence,0));
        assertEquals(track.size(), midi.getLength());
        assertEquals(0, midi.getNoteAtTrackAndTime(-1,0));
    }

    @Test void streamOwnershipAndSerializedMidiWorkOnSuccessAndFailure() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ,480);
        Track track = sequence.createTrack();
        event(track,0,ShortMessage.NOTE_ON,0,60,64);
        event(track,480,ShortMessage.NOTE_OFF,0,60,0);
        var bytes = new ByteArrayOutputStream();
        MidiSystem.write(sequence,1,bytes);
        var stream = new OwnedStream(bytes.toByteArray());
        assertEquals(1, new MIDIInterface(ReikaMIDIReader.getMIDIFromFile(stream)).fillToScore(true).noteCount());
        assertTrue(stream.closed);
        var bad = new OwnedStream(new byte[]{1,2,3});
        assertThrows(java.io.IOException.class, () -> ReikaMIDIReader.getMIDIFromFile(bad));
        assertTrue(bad.closed);
    }

    @Test void emptyOutOfRangeAndConversionInputsAreHandled() throws Exception {
        assertEquals(0, new MIDIInterface((Sequence)null).fillToScore(true).noteCount());
        assertEquals(0, new MIDIInterface((Sequence)null).fillToArray().length);
        Sequence sequence = new Sequence(Sequence.PPQ,480);
        assertEquals(100, ReikaMIDIReader.MIDITickToMCTick(sequence,480,120));
        assertEquals(480, ReikaMIDIReader.MCTickToMIDITick(sequence,100,120));
        assertThrows(IllegalArgumentException.class, () -> ReikaMIDIReader.MIDITickToMCTick(sequence,480,0));
        assertThrows(IllegalArgumentException.class, () -> ReikaMIDIReader.MIDITickToMCTick(sequence,Long.MAX_VALUE,120));
        Track track = sequence.createTrack();
        event(track,0,ShortMessage.NOTE_ON,0,127,64);
        event(track,480,ShortMessage.NOTE_OFF,0,127,0);
        assertEquals(0, new MIDIInterface(sequence).fillToScore(true).noteCount());
    }

    private static MusicScore.Note note(MusicScore score, int channel, int time) {
        return score.getNotes(channel,time).iterator().next();
    }
    private static void event(Track track,long tick,int command,int channel,int a,int b) throws Exception {
        track.add(new MidiEvent(new ShortMessage(command,channel,a,b),tick));
    }
    private static void tempo(Track track,long tick,int value) throws Exception {
        meta(track,tick,ReikaMIDIReader.TEMPO,new byte[]{(byte)(value>>16),(byte)(value>>8),(byte)value});
    }
    private static void meta(Track track,long tick,int type,byte[] bytes) throws Exception {
        var message=new MetaMessage(); message.setMessage(type,bytes,bytes.length);
        track.add(new MidiEvent(message,tick));
    }
    private static final class OwnedStream extends ByteArrayInputStream {
        boolean closed;
        OwnedStream(byte[] bytes) { super(bytes); }
        @Override public void close() { closed=true; }
    }
}
