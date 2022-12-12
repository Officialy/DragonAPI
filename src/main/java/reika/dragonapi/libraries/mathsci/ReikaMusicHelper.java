/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.mathsci;

import reika.dragonapi.io.ReikaMIDIReader;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.*;


public class ReikaMusicHelper {

	public static Note getNote(int note) {
		return Note.notes[(note + 6) % Note.notes.length];
	}

	public static String getNoteName(int note) {
		return getNote(note).name;
	}

	public static boolean isNoteSharpOrFlat(int note) {
		return !getNote(note).isPure();
	}

	public static boolean isNoteSharp(int note) {
		return getNoteName(note).endsWith("#");
	}

	public static boolean isNoteFlat(int note) {
		return getNoteName(note).endsWith("b");
	}

	public enum Note {
		C("C", 0),
		CSHARP("C#"),
		D("D", 1),
		EFLAT("Eb"),
		E("E", 2),
		F("F", 3),
		FSHARP("F#"),
		G("G", 4),
		GSHARP("G#"),
		A("A", 5),
		BFLAT("Bb"),
		B("B", 6);

		private static final HashSet<Note> pureNotes = new HashSet<>();
		private static final Note[] notes = values();

		static {
			for (Note n : notes) {
				if (n.isPure()) {
					pureNotes.add(n);
				}
			}
		}

		public final String name;
		/**
		 * Semitone offset. Zero for 'pure' notes
		 */
		public final int semitone;
		public final int keyIndex;

		Note(String s) {
			this(s, -1);
		}

		Note(String s, int idx) {
			name = s;
			char c = s.charAt(s.length() - 1);
			switch (c) {
				case '#' -> semitone = 1;
				case 'b' -> semitone = -1;
				default -> semitone = 0;
			}
			keyIndex = idx;
		}

		public static Note getNoteByName(String s) {
			return Note.valueOf(s.replace("#", "SHARP").replace("b", "FLAT"));
		}

		public boolean isPure() {
			return semitone == 0;
		}

		public Note getFlat() {
			return this == C ? B : notes[this.ordinal() - 1];
		}

		public Note getSharp() {
			return this == B ? C : notes[this.ordinal() + 1];
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public enum MusicKey {
		C1(33),
		Cs1(35),
		D1(37),
		Eb1(39),
		E1(41),
		F1(44),
		Fs1(46),
		G1(49),
		Ab1(52),
		A1(55),
		Bb1(58),
		B1(62),
		C2(65),
		Cs2(69),
		D2(73),
		Eb2(78),
		E2(82),
		F2(87),
		Fs2(92),
		G2(98),
		Ab2(104),
		A2(110),
		Bb2(117),
		B2(123),
		C3(131),
		Cs3(139),
		D3(147),
		Eb3(156),
		E3(165),
		F3(175),
		Fs3(185),
		G3(196),
		Ab3(208),
		A3(220),
		Bb3(233),
		B3(247),
		C4(262),
		Cs4(277),
		D4(294),
		Eb4(311),
		E4(330),
		F4(349),
		Fs4(370),
		G4(392),
		Ab4(415),
		A4(440),
		Bb4(466),
		B4(494),
		C5(523),
		Cs5(554),
		D5(587),
		Eb5(622),
		E5(659),
		F5(698),
		Fs5(740),
		G5(784),
		Ab5(830),
		A5(880),
		Bb5(932),
		B5(988),
		C6(1046),
		Cs6(1109),
		D6(1175),
		Eb6(1245),
		E6(1319),
		F6(1397),
		Fs6(1480),
		G6(1568),
		Ab6(1661),
		A6(1760),
		Bb6(1865),
		B6(1976),
		C7(2093),
		Cs7(2218),
		D7(2349),
		Eb7(2489),
		E7(2637),
		F7(2794),
		Fs7(2960),
		G7(3136),
		Ab7(3322),
		A7(3520),
		Bb7(3729),
		B7(3951),
		C8(4186),
		;

		private static final MusicKey[] list = values();
		public final int pitch;
		public final int octaveNumber;

		MusicKey(int f) {
			pitch = f;

			octaveNumber = Integer.parseInt(this.name().substring(this.name().length() - 1));
		}

		public static double getRatio(MusicKey k1, MusicKey k2) {
			return (double) k2.pitch / k1.pitch;
		}

		public static Collection<MusicKey> getAllOf(Note n) {
			ArrayList<MusicKey> li = new ArrayList<>();
			for (int i = n.ordinal(); i < list.length; i += Note.notes.length) {
				li.add(list[i]);
			}
			return li;
		}

		public static MusicKey getKeyFromMIDI(int key) {
			int index = key - ReikaMIDIReader.MIDI_C5 + C5.ordinal();
			return index >= 0 && index < list.length ? list[index] : null;
		}

		public static MusicKey getByIndex(int key) {
			return key >= 0 && key < list.length ? list[key] : null;
		}

		public MusicKey getMinorThird() {
			return this.getInterval(3);
		}

		public MusicKey getMajorThird() {
			return this.getInterval(4);
		}

		public MusicKey getFourth() {
			return this.getInterval(5);
		}

		public MusicKey getFifth() {
			return this.getInterval(7);
		}

		public MusicKey getOctave() {
			return this.getInterval(12);
		}

		public MusicKey getInterval(int n) {
			int o = this.ordinal() + n;
			return o >= 0 && o < list.length ? list[o] : this;
		}

		public double getRatio(MusicKey k) {
			return getRatio(k, this);
		}

		@Override
		public String toString() {
			return this.displayName() + " @ " + pitch + " Hz";
		}

		public Note getNote() {
			return Note.notes[this.ordinal() % Note.notes.length];
		}

		public String displayName() {
			return this.name().replaceAll("s", "#");
		}
	}

	public enum KeySignature {
		C(Note.C),
		F(Note.F, Note.B),
		BFLAT(Note.B, Note.B, Note.E),
		EFLAT(Note.E, Note.B, Note.E, Note.A),
		AFLAT(Note.A, Note.B, Note.E, Note.A, Note.D),
		DFLAT(Note.D, Note.B, Note.E, Note.A, Note.D, Note.G),
		GFLAT(Note.G, Note.B, Note.E, Note.A, Note.D, Note.G, Note.C),
		CFLAT(Note.C, Note.B, Note.E, Note.A, Note.D, Note.G, Note.C, Note.F),
		G(Note.G, Note.F),
		D(Note.D, Note.F, Note.C),
		A(Note.A, Note.F, Note.C, Note.G),
		E(Note.E, Note.F, Note.C, Note.G, Note.D),
		B(Note.B, Note.F, Note.C, Note.G, Note.D, Note.A),
		FSHARP(Note.F, Note.F, Note.C, Note.G, Note.D, Note.A, Note.E),
		CSHARP(Note.C, Note.F, Note.C, Note.G, Note.D, Note.A, Note.E, Note.B);

		public static final KeySignature[] keys = values();
		private static final EnumMap<Note, KeySignature> keyMap = new EnumMap<>(Note.class);
		private static final EnumMap<Note, KeySignature> minorKeyMap = new EnumMap<>(Note.class);

		static {
			for (int i = 0; i < keys.length; i++) {
				KeySignature ks = KeySignature.keys[i];
				keyMap.put(ks.tonic, ks);
				minorKeyMap.put(ks.minor.get(0), ks);
			}
		}

		public final Note tonic;
		private final HashSet<Note> sharps = new HashSet();
		private final HashSet<Note> flats = new HashSet();
		private final HashSet<Note> notes = new HashSet();
		private final ArrayList<Note> scale = new ArrayList<>();
		private final ArrayList<Note> minor = new ArrayList<>();

		KeySignature(Note ton, Note... key) {
			for (int i = 0; i < key.length; i++) {
				Note n = key[i];
				if (this.isSharp()) {
					sharps.add(n);
				} else if (this.isFlat()) {
					flats.add(n);
				}
			}

			Note ctr = ton;

			if (sharps.contains(ton))
				ton = ton.getSharp();
			else if (flats.contains(ton))
				ton = ton.getFlat();
			tonic = ton;

			ArrayList<Note> li = new ArrayList<>(Note.pureNotes);
			ReikaJavaLibrary.cycleList(li, li.size() - li.indexOf(ctr));
			for (int i = 0; i < li.size(); i++) {
				Note n = li.get(i);
				if (sharps.contains(n)) {
					li.set(i, n.getSharp());
				} else if (flats.contains(n)) {
					li.set(i, n.getFlat());
				}
			}
			notes.addAll(li);
			scale.addAll(li);
			minor.addAll(li);
			ReikaJavaLibrary.cycleList(minor, 2);
		}

		public static KeySignature getByTonic(MusicKey key) {
			return getByTonic(key.getNote());
		}

		public static KeySignature getByTonic(Note key) {
			return keyMap.get(key);
		}

		public static KeySignature getByMinorTonic(MusicKey key) {
			return getByMinorTonic(key.getNote());
		}

		public static KeySignature getByMinorTonic(Note key) {
			return minorKeyMap.get(key);
		}

		public static KeySignature findSignature(ArrayList<MusicKey> li) {
			HashSet<Note> notes = new HashSet<>();
			for (MusicKey key : li) {
				notes.add(key.getNote());
			}
			for (KeySignature ks : keys) {
				boolean flag = true;
				for (Note n : notes) {
					if (!ks.isNoteValid(n)) {
						flag = false;
						break;
					}
				}
				if (flag)
					return ks;
			}
			return null;
		}

		public boolean isFlat() {
			return this.ordinal() != 0 && this.ordinal() < 8; //G
		}

		public boolean isSharp() {
			return this.ordinal() >= 8; //G
		}

		public Set<Note> getFlats() {
			return Collections.unmodifiableSet(flats);
		}

		public Set<Note> getSharps() {
			return Collections.unmodifiableSet(sharps);
		}

		public List<Note> getScale() {
			return Collections.unmodifiableList(scale);
		}

		public List<Note> getRelativeMinor() {
			return Collections.unmodifiableList(minor);
		}

		@Override
		public String toString() {
			return "Scale '" + this.name() + "' (" + tonic.name + "): M+" + scale + " & m-" + minor;
		}

		public boolean isNoteValid(Note n) {
			return notes.contains(n);
		}
	}

	public enum ChordType {
		OCTAVE(0, 12),
		POWER(0, 7, 12),
		MAJOR(0, 4, 7, 12),
		MINOR(0, 3, 7, 12),
		AUGMENTED(0, 4, 8, 12),
		DIMINISHED(0, 3, 6, 12);

		private final ArrayList<Integer> notes;

		ChordType(int... n) {
			notes = ReikaJavaLibrary.makeIntListFromArray(n);
		}

		public ArrayList<MusicKey> getChord(MusicKey tonic) {
			ArrayList<MusicKey> li = new ArrayList<>();
			for (int note : notes) {
				li.add(tonic.getInterval(note));
			}
			return li;
		}

		@Override
		public String toString() {
			return this.name() + " Chord: " + notes;
		}
	}
}
