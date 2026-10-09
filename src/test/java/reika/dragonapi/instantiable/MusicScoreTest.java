package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey.*;

class MusicScoreTest {
    @Test void channelChordsRoundTripThroughNativeNbt() {
        var score = score(); var tag = new CompoundTag(); score.saveAdditional(tag);
        assertTrue(tag.getCompoundOrEmpty("Ch_2").contains("24"));
        assertFalse(tag.contains("24"));
        tag.putInt("count", 999); // caches are derived from persisted notes
        var loaded = MusicScore.load(tag);
        assertEquals(3, loaded.noteCount());
        assertEquals(score.getActiveTracks(), loaded.getActiveTracks());
        assertEquals(C5, loaded.getTrack(2).getLowest());
        assertEquals(G5, loaded.getTrack(2).getHighest());
        assertEquals(2, loaded.getNotes(2,24).size());
        assertTrue(loaded.getNotes(9,48).iterator().next().percussion);
        var note = loaded.getNotes(2,24).iterator().next();
        assertEquals(32,note.voice); assertEquals(80,note.volume); assertEquals(96,note.length);
    }

    @Test void copiesDoNotShareChordMutationsAndKeepMetadata() {
        var score = score(); var copy = score.copy();
        copy.addNote(24,2,E5,18,64,12,false);
        assertEquals(2,score.getNotes(2,24).size());
        assertEquals(3,copy.getNotes(2,24).size());
        assertEquals(3,score.noteCount()); assertEquals(4,copy.noteCount());
        assertEquals(score.getActiveTracks(),copy.getActiveTracks());
        assertEquals(score.getLowest(),copy.getLowest());
    }

    @Test void editsRecomputeCountsRangeAndLatestTime() {
        var score = score();
        score.backspace(2); assertEquals(1,score.noteCount());
        assertEquals(48,score.getLatestPos()); assertEquals(C4,score.getLowest());
        score.clearChannel(9); score.clearChannel(9);
        assertEquals(0,score.noteCount()); assertTrue(score.getActiveTracks().isEmpty());
        assertEquals(0,score.getLatestPos()); assertNull(score.getHighest());
        score.normalizeToRange(C4,C6); // empty scores used to throw
    }

    @Test void transposeAndSpeedScalingUpdateConsumerMetadata() {
        var score = score(); score.transpose(12);
        assertEquals(C5,score.getLowest()); assertEquals(G6,score.getTrack(2).getHighest());
        var scaled = score.scaleSpeed(11,true);
        assertEquals(0,scaled.getTrack(2).firstNoteTime());
        assertEquals(8,scaled.getTrack(2).getNoteAt(0).length());
        assertEquals(2,scaled.getTrack(9).firstNoteTime());
        assertEquals(0,score.alignToZero().getTrack(2).firstNoteTime());
        assertThrows(IllegalArgumentException.class, () -> score.scaleSpeed(0,true));
        assertThrows(IllegalArgumentException.class, () -> score.scaleSpeed(Float.NaN,true));
    }

    @Test void gridAlignmentMergesChordsAndLengthIncludesEarlierLongNotes() {
        var score = new MusicScore(1);
        score.addNote(0,0,C5,0,64,200,false);
        score.addNote(10,0,E5,0,64,20,false);
        assertEquals(200,score.getTrack(0).getLengthInTicks());
        assertEquals(1,score.getTrack(0).getActiveNotesAt(100).size());
        var grid = score.getTrack(0).alignToGrid(12);
        assertEquals(2,grid.getNoteAt(0).notes().size());
        assertEquals(2,grid.noteCount()); assertEquals(E5,grid.getHighest());
        assertThrows(IllegalArgumentException.class, () -> score.getTrack(0).alignToGrid(0));
    }

    @Test void replacingOnePitchKeepsCountsAndChordDurationAccurate() {
        var score = new MusicScore(1);
        score.addNote(0,0,C5,0,64,200,false);
        score.addNote(0,0,C5,32,64,10,false);
        assertEquals(1,score.noteCount()); assertEquals(10,score.getTrack(0).getNoteAt(0).length());
        assertEquals(32,score.getNotes(0,0).iterator().next().voice);
    }

    private static MusicScore score() {
        var score=new MusicScore(16);
        score.addNote(24,2,C5,32,80,96,false);
        score.addNote(24,2,G5,18,64,24,false);
        score.addNote(48,9,C4,0,90,12,true);
        return score;
    }
}
