/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.objects;

import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

public enum ArrowCharacters {

    RIGHT(8250, 0, 44),
    UPRIGHT(8989, 45, 89),
    UP(6356, 90, 134),
    UPLEFT(8988, 135, 179),
    LEFT(8249, 180, 224),
    DOWNLEFT(8990, 225, 269),
    DOWN(9013, 270, 314),
    DOWNRIGHT(8991, 315, 359);

    private static final ArrowCharacters[] arrowList = ArrowCharacters.values();

    private final char[] ch;
    private final int min;
    private final int max;

    ArrowCharacters(int c, int lo, int hi) {
        ch = Character.toChars(c);
        min = lo;
        max = hi;
    }

    public static ArrowCharacters getFromAngle(double ang) {
        ang = ang % 360;
        for (ArrowCharacters arrowCharacters : arrowList) {
            if (ReikaMathLibrary.isValueInsideBoundsIncl(arrowCharacters.getMinAngle(), arrowCharacters.getMaxAngle(), ang)) {
                return arrowCharacters;
            }
        }
        return null;
    }

    public int getMinAngle() {
        return min;
    }

    public int getMaxAngle() {
        return max;
    }

    public String getStringValue() {
        return new String(ch);
    }

}
