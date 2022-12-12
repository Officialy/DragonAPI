/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary;

import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.ArrayList;
import java.util.Collection;


public enum CoreModDetection {

    OPTIFINE("optifine.OptiFineTweaker", "rendering and textures", ""),
    VIVE("com.mtbs3d.minecrift.api.IRoomscaleAdapter", "rendering and interface", ""),
    COLOREDLIGHTS("coloredlightscore.src.asm.ColoredLightsCoreLoadingPlugin", "rendering", "");

    public static final CoreModDetection[] list = values();

    static {
        for (int i = 0; i < values().length; i++) {
            CoreModDetection c = values()[i];
            if (c.isInstalled()) {
                ReikaJavaLibrary.pConsole("DRAGONAPI: " + c + " detected. Loading compatibility features.");
                ReikaJavaLibrary.pConsole("\t\tNote that some parts of the game, especially " + c.warning + ", may error out.");
                ReikaJavaLibrary.pConsole("\t\t" + c.message);
            } else {
                ReikaJavaLibrary.pConsole("DRAGONAPI: " + c + " not detected.");
            }
        }
    }

    private final Class refClass;
    private final boolean isLoaded;
    private final String warning;
    private final String message;

    CoreModDetection(String s, String w, String m) {
        this(ReikaJavaLibrary.getClassNoException(s), w, m);
    }

    CoreModDetection(Class c, String w, String m) {
        this(c, c != null, w, m);
    }

    CoreModDetection(boolean flag, String w, String m) {
        this(null, flag, w, m);
    }

    CoreModDetection(Class c, boolean flag, String w, String m) {
        refClass = c;
        isLoaded = flag;
        warning = w;
        message = m;
    }

    public static String getStatus() {
        Collection<CoreModDetection> li = new ArrayList<>();
        for (CoreModDetection cm : list) {
            if (cm.isInstalled())
                li.add(cm);
        }
        return li.isEmpty() ? "None" : li.toString();
    }

    public boolean isInstalled() {
        return isLoaded;
    }
}
