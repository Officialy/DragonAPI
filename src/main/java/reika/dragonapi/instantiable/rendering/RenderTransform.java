/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.rendering;


public record RenderTransform(double offsetX, double offsetY, double offsetZ, double rotationX, double rotationY,
                              double rotationZ) {

    public RenderTransform(double ox, double oy, double oz) {
        this(ox, oy, oz, 0, 0, 0);
    }

}
