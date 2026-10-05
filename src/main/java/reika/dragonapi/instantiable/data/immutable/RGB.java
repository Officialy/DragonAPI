/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

public record RGB(int red, int green, int blue, int alpha) {

    public RGB(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public RGB(int red, int green, int blue, int alpha) {
        this.red = Math.min(255, red);
        this.green = Math.min(255, green);
        this.blue = Math.min(255, blue);
        this.alpha = Math.min(255, alpha);
    }

    /**
     * ARGB
     */
    public RGB(int color) {
        this(ReikaColorAPI.getRed(color), ReikaColorAPI.getGreen(color), ReikaColorAPI.getBlue(color), ReikaColorAPI.getAlpha(color));
    }

    public int getInt() {
        return ReikaColorAPI.RGBtoHex(red, green, blue, alpha);
    }

    public double getDistance(RGB rgb) {
        return ReikaMathLibrary.py3d(red - rgb.red, green - rgb.green, blue - rgb.blue);
    }

    public RGB permute(int dr, int dg, int db, int da) {
        return new RGB(red + dr, green + dg, blue + db, alpha + da);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RGB c) {
            return c.red == red && c.green == green && c.blue == blue && c.alpha == alpha;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getInt();
    }

}
