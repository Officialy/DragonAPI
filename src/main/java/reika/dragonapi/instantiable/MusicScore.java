/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;
import java.util.Map.Entry;


public class MusicScore {

    private final ScoreTrack[] music;
    private final HashSet<Integer> activeTracks = new HashSet<>();
    private final int channelCount;

    private int length;
    private int noteCount;

    private MusicKey lowest;
    private MusicKey highest;
    private int firstNoteTime = Integer.MAX_VALUE;

    public MusicScore(int channels) {
        channelCount = channels;
        music = new ScoreTrack[channels];
    }

    public static MusicScore load(CompoundTag tag) {
        MusicScore mus = new MusicScore(NBTCompat.getInt(tag, "numchan", 0));

        for (int i = 0; i < mus.channelCount; i++) {
            if (tag.contains("Ch_" + i)) {
                mus.music[i] = new ScoreTrack(i);
                CompoundTag nbt = NBTCompat.getCompound(tag, "Ch_" + i);
                for (Object o : nbt.keySet()) {
                    String s = (String) o;
                    int time = Integer.parseInt(s);
                    ListTag li = nbt.getListOrEmpty(s);
                    NoteData c = NoteData.load(time, li);
                    mus.music[i].put(time, c);
                }
                if (mus.music[i].isEmpty())
                    mus.music[i] = null;
                else
                    mus.activeTracks.add(i);
            }
        }

        mus.rebuildMetadata();

        return mus;
    }

    public void addNote(int time, int channel, MusicKey note, int voice, int vol, int len, boolean perc) {
        this.addNote(channel, time, new Note(note, voice, vol, len, perc));
    }

    private void addNote(int channel, int time, Note note) {
        Objects.requireNonNull(note.key, "Music key");
        if (time < 0 || note.length < 0) throw new IllegalArgumentException("Negative note time or length");
        if (music[channel] == null) {
            music[channel] = new ScoreTrack(channel);
        }
        NoteData c = music[channel].getNoteAt(time);
        if (c == null) {
            c = new NoteData(time);
            music[channel].put(time, c);
        }
        activeTracks.add(channel);
        if (!c.notes.containsKey(note.key)) noteCount++;
        c.add(note);
        music[channel].onAddNote(time, note);
        length = Math.max(length, time);
        firstNoteTime = Math.min(firstNoteTime, time);
        if (lowest == null || lowest.ordinal() > note.key.ordinal()) lowest = note.key;
        if (highest == null || highest.ordinal() < note.key.ordinal()) highest = note.key;
    }

    /** Derive caches from actual notes after destructive edits or loading. */
    private void rebuildMetadata() {
        activeTracks.clear();
        length = 0;
        noteCount = 0;
        firstNoteTime = Integer.MAX_VALUE;
        lowest = highest = null;
        for (int channel = 0; channel < channelCount; channel++) {
            ScoreTrack track = music[channel];
            if (track == null) continue;
            track.rebuildMetadata();
            if (track.isEmpty()) continue;
            activeTracks.add(channel);
            length = Math.max(length, track.lastNoteTime());
            firstNoteTime = Math.min(firstNoteTime, track.firstNoteTime());
            noteCount += track.noteCount();
            if (lowest == null || lowest.ordinal() > track.lowest.ordinal()) lowest = track.lowest;
            if (highest == null || highest.ordinal() < track.highest.ordinal()) highest = track.highest;
        }
    }

    public Collection<Note> getNotes(int time) {
        Collection<Note> li = new ArrayList<>();
        for (int i = 0; i < channelCount; i++) {
            NoteData n = music[i] != null ? music[i].getNoteAt(time) : null;
            if (n != null) {
                li.addAll(n.notes.values());
            }
        }
        return li;
    }

    public Collection<Note> getNotes(int channel, int time) {
        NoteData c = music[channel] != null ? music[channel].getNoteAt(time) : null;
        return c != null ? Collections.unmodifiableCollection(c.notes.values()) : null;
    }

    public void backspace(int channel) {
        if (music[channel] != null && !music[channel].isEmpty()) {
            music[channel].remove(music[channel].lastNoteTime());
            rebuildMetadata();
        }
    }

    public ScoreTrack getTrack(int channel) {
        return music[channel] != null ? music[channel] : null;
    }

    public int getLatestPos(int channel) {
        return music[channel] != null && !music[channel].isEmpty() ? music[channel].lastNoteTime() : 0;
    }

    public int getLatestPos() {
        return length;
    }

    public int countTracks() {
        return music.length;
    }

    public MusicScore scaleSpeed(float factor, boolean alignToZero) {
        MusicScore mus = new MusicScore(channelCount);

        if (!(factor > 0) || !Float.isFinite(factor)) throw new IllegalArgumentException("Speed must be finite and positive");
        int origin = alignToZero && noteCount > 0 ? firstNoteTime : 0;

        for (int i = 0; i < channelCount; i++) {
            if (music[i] != null) {
                for (Entry<Integer, NoteData> e : music[i].entrySet()) {
                    int time = e.getKey();
                    NoteData c = e.getValue();
                    if (c != null)
                        for (Note n : c.notes.values())
                            mus.addNote(i, (int) ((time - origin) / factor), n.scaleSpeed(factor));
                }
            }
        }

        return mus;
    }

    public MusicScore alignToZero() {
        MusicScore mus = new MusicScore(channelCount);
        for (int i = 0; i < channelCount; i++) {
            if (music[i] != null) {
                for (Entry<Integer, NoteData> e : music[i].entrySet()) {
                    int time = e.getKey();
                    NoteData c = e.getValue();
                    if (c != null)
                        for (Note n : c.notes.values())
                            mus.addNote(i, time - firstNoteTime, n);
                }
            }
        }
        return mus;
    }

    public void transpose(int semitones) {
        if (semitones == 0)
            return;
        for (int i = 0; i < channelCount; i++) {
            if (music[i] != null) {
                for (Entry<Integer, NoteData> e : music[i].entrySet()) {
                    int time = e.getKey();
                    NoteData c = e.getValue();
                    if (c != null) {
                        e.setValue(c.transpose(semitones));
                    }
                }
            }
        }
        rebuildMetadata();
    }

    public MusicKey getLowest() {
        return lowest;
    }

    public MusicKey getHighest() {
        return highest;
    }

    public void normalizeToRange(MusicKey min, MusicKey max) {
        if (noteCount == 0) return;
        int under = min.ordinal() - lowest.ordinal();
        int over = highest.ordinal() - max.ordinal();
        int ounder = ReikaMathLibrary.roundToNearestX(12, under);
        int oover = ReikaMathLibrary.roundToNearestX(12, over);
        if ((oover <= 0 && ounder <= 0) || (oover == ounder)) {
        } else if (oover > ounder) {
            this.transpose(-12 * Math.round((oover - ounder) / 24F));
        } else {
            this.transpose(12 * Math.round((ounder - oover) / 24F));
        }
    }

    public void saveAdditional(CompoundTag tag) {
        for (int i = 0; i < channelCount; i++) {
            CompoundTag nbt = new CompoundTag();
            if (music[i] != null) {
                for (Entry<Integer, NoteData> e : music[i].entrySet()) {
                    ListTag li = e.getValue().saveAdditional();
                    nbt.put(String.valueOf(e.getKey()), li);
                }
            }
            tag.put("Ch_" + i, nbt);
        }
        tag.putInt("numchan", channelCount);
        tag.putInt("len", length);
        tag.putInt("first", firstNoteTime);
        tag.putInt("count", noteCount);
        tag.putInt("lowest", lowest != null ? lowest.ordinal() : -1);
        tag.putInt("highest", highest != null ? highest.ordinal() : -1);
    }

    @Override
    public String toString() {
        return Arrays.toString(music);
    }

    public int noteCount() {
        return noteCount;
    }

    public Set<Integer> getActiveTracks() {
        return Collections.unmodifiableSet(activeTracks);
    }

    public void clearChannel(int channel) {
        music[channel] = null;
        rebuildMetadata();
    }


    public void renderPianoRoll() {

    }

    public MusicScore copy() {
        MusicScore mus = new MusicScore(channelCount);
        for (int i = 0; i < channelCount; i++) {
            if (music[i] != null) {
                mus.music[i] = music[i].copy();
            }
        }
        mus.rebuildMetadata();
        return mus;
    }

    public static class ScoreTrack {

        public final int channel;
        private final TreeMap<Integer, NoteData> notes = new TreeMap<>();
        private MusicKey lowest;
        private MusicKey highest;
        private int firstNoteTime = Integer.MAX_VALUE;

        private ScoreTrack(int ch) {
            channel = ch;
        }

        public ScoreTrack copy() {
            ScoreTrack ret = new ScoreTrack(channel);
            for (Entry<Integer, NoteData> entry : notes.entrySet()) ret.put(entry.getKey(), entry.getValue().setTick(entry.getKey()));
            return ret;
        }

        public ScoreTrack alignToGrid(int step) {
            if (step <= 0) throw new IllegalArgumentException("Grid step must be positive");
            ScoreTrack ret = new ScoreTrack(channel);
            for (Entry<Integer, NoteData> e : notes.entrySet()) {
                int tick = ReikaMathLibrary.roundDownToX(step, e.getKey());
                NoteData merged = ret.notes.computeIfAbsent(tick, NoteData::new);
                for (Note note : e.getValue().notes()) merged.add(note);
            }
            ret.rebuildMetadata();
            return ret;
        }

        public NoteData getNoteAt(int time) {
            return notes.get(time);
        }

        public int firstNoteTime() {
            return notes.isEmpty() ? -1 : notes.firstKey();
        }

        public int lastNoteTime() {
            return notes.isEmpty() ? -1 : notes.lastKey();
        }

        public MusicKey getLowest() {
            return lowest;
        }

        public MusicKey getHighest() {
            return highest;
        }

        public int getLengthInTicks() {
            int end = 0;
            for (NoteData data : notes.values()) end = Math.max(end, Math.addExact(data.tick, data.length()));
            return end;
        }

        public boolean isEmpty() {
            return notes.isEmpty();
        }

        public int noteCount() {
            return notes.values().stream().mapToInt(data -> data.notes.size()).sum();
        }

        private Collection<Entry<Integer, NoteData>> entrySet() {
            return notes.entrySet();
        }

        public Collection<Entry<Integer, NoteData>> entryView() {
            return Collections.unmodifiableMap(notes).entrySet();
        }

        private Collection<Integer> keySet() {
            return notes.keySet();
        }

        private void put(int time, NoteData data) {
            notes.put(time, data);

            for (Note n : data.notes())
                this.onAddNote(time, n);
        }

        private void rebuildMetadata() {
            firstNoteTime = Integer.MAX_VALUE;
            lowest = highest = null;
            for (NoteData data : notes.values()) for (Note note : data.notes()) onAddNote(data.tick, note);
        }

        private void onAddNote(int time, Note n) {
            firstNoteTime = Math.min(firstNoteTime, time);
            if (lowest == null || lowest.ordinal() > n.key.ordinal())
                lowest = n.key;
            if (highest == null || highest.ordinal() < n.key.ordinal())
                highest = n.key;
        }

        private NoteData remove(int time) {
            return notes.remove(time);
        }

        public Collection<NoteData> getNotes() {
            return Collections.unmodifiableCollection(notes.values());
        }

        @Override
        public String toString() {
            return notes.toString();
        }

        public Collection<Note> getActiveNotesAt(int tick) {
            Collection<Note> ret = new ArrayList<>();
            Integer prev = notes.floorKey(tick);
            while (prev != null) {
                NoteData nd = notes.get(prev);
                for (Note n : nd.notes.values()) {
                    long end = (long) nd.tick + n.length;
                    if (end > tick)
                        ret.add(n);
                }

                prev = notes.lowerKey(prev);
            }
            return ret;
        }

    }

    public static class NoteData {

        public final int tick;
        private final EnumMap<MusicKey, Note> notes = new EnumMap<>(MusicKey.class);
        private int longestNote = 0;

        private NoteData(int t) {
            tick = t;
        }

        private static NoteData load(int t, ListTag li) {
            NoteData dat = new NoteData(t);
            for (int index = 0; index < li.size(); index++) {
                CompoundTag val = li.getCompoundOrEmpty(index);
                Note n = Note.load(val);
                if (n.key != null) dat.add(n);
            }
            return dat;
        }

        public NoteData setTick(int newtick) {
            NoteData ret = new NoteData(newtick);
            ret.notes.putAll(notes);
            ret.longestNote = longestNote;
            return ret;
        }

        public int length() {
            return longestNote;
        }

        public NoteData transpose(int semitones) {
            NoteData ret = new NoteData(tick);
            for (Note n : notes.values()) {
                ret.add(n.transpose(semitones));
            }
            return ret;
        }

        private ListTag saveAdditional() {
            ListTag li = new ListTag();
            for (Note n : notes.values()) {
                CompoundTag val = new CompoundTag();
                n.saveAdditional(val);
                li.add(val);
            }
            return li;
        }

        private void add(Note note) {
            notes.put(note.key, note);
            longestNote = notes.values().stream().mapToInt(n -> n.length).max().orElse(0);
        }

        public Collection<Note> notes() {
            return Collections.unmodifiableCollection(notes.values());
        }

        public Set<MusicKey> keys() {
            return Collections.unmodifiableSet(notes.keySet());
        }

        @Override
        public String toString() {
            return notes.values() + " @ " + tick;
        }

    }

    public static class Note {

        public final MusicKey key;

        /**
         * With General MIDI Spec, is 0-127. See MIDI Spec for details
         */
        public final int voice;

        /**
         * With MIDI Spec, is 0-127
         */
        public final int volume;

        /**
         * In MC ticks
         */
        public final int length;

        public final boolean percussion;

        private Note(MusicKey note, int instru, int vol, int len, boolean perc) {
            key = note;
            voice = instru;
            volume = vol;
            length = len;
            percussion = perc;
        }

        public static Note load(CompoundTag nbt) {
            int key = NBTCompat.getInt(nbt, "key", 0);
            int voice = NBTCompat.getInt(nbt, "voice", 0);
            int volume = NBTCompat.getInt(nbt, "volume", 0);
            int length = NBTCompat.getInt(nbt, "length", 0);
            boolean perc = NBTCompat.getBoolean(nbt, "percussion", false);
            return new Note(MusicKey.getByIndex(key), voice, volume, length, perc);
        }

        public Note scaleSpeed(float speed) {
            return new Note(key, voice, volume, (int) (length / speed), percussion);
        }

        public Note transpose(int semitones) {
            return new Note(key.getInterval(semitones), voice, volume, length, percussion);
        }

        public void saveAdditional(CompoundTag nbt) {
            nbt.putInt("key", key.ordinal());
            nbt.putInt("volume", volume);
            nbt.putInt("voice", voice);
            nbt.putInt("length", length);
            nbt.putBoolean("percussion", percussion);
        }

        @Override
        public String toString() {
            return key.name() + " / instr=" + voice + " / vol=" + volume + " / len=" + length;
        }

        @Override
        public int hashCode() {
            return key.ordinal() << 16 | voice << 8 | volume;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Note n) {
                return n.key == key && n.voice == voice && n.volume == volume;
            }
            return false;
        }

    }

}
