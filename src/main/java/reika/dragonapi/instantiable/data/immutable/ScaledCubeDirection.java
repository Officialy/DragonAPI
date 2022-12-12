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

import reika.dragonapi.libraries.ReikaDirectionHelper;


public final class ScaledCubeDirection {

    public final int offsetX;
    public final int offsetZ;

    public final int distance;
    public final ReikaDirectionHelper.CubeDirections direction;

    public ScaledCubeDirection(ReikaDirectionHelper.CubeDirections dir, int dist) {
        direction = dir;
        distance = dist;

        offsetX = dir.directionX * dist;
        offsetZ = dir.directionZ * dist;
    }

}
