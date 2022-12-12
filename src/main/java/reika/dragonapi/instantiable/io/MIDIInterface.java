/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.io;

import reika.dragonapi.instantiable.MusicScore;
import reika.dragonapi.io.ReikaMIDIReader;

import javax.sound.midi.Sequence;
import java.io.File;

public final class MIDIInterface {

    private final Sequence midi;

    public MIDIInterface(File f) {
        midi = ReikaMIDIReader.getMIDIFromFile(f);
    }

    public MIDIInterface(Class root, String path) {
        midi = ReikaMIDIReader.getMIDIFromFile(root, path);
    }
	/*
	/** Returns the note at the given track and time. Args: Track, Time *//*
	public int getNoteAtTrackAndTime(int track, int time) {/*
		int a = ReikaMIDIReader.readMIDI(midi, track, time, 0);
		if (a != 0 && a != -1)
		;//ReikaJavaLibrary.pConsole(time+" @ "+a);
		return a;*//*
		//return ReikaMIDIReader.getMidiNoteAtChannelAndTime(midi, time, track);
		return 0;
	}*/

    public void debug() {
        ReikaMIDIReader.debugMIDI(midi);
    }

    public int getLength() {
        return ReikaMIDIReader.getMidiLength(midi);
    }

    public int[][][] fillToArray() {
        return ReikaMIDIReader.readMIDIFileToArray(midi);
    }

    public MusicScore fillToScore(boolean readPercussion) {
        return ReikaMIDIReader.readMIDIFileToScore(midi, readPercussion);
    }

}
