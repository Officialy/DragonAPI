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

import net.minecraft.core.Direction;

public final class ScaledDirection {

    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;

    public final int distance;
    public final Direction direction;

    public ScaledDirection(Direction dir, int dist) {
        direction = dir;
        distance = dist;

        offsetX = dir.getStepX() * dist;
        offsetY = dir.getStepY() * dist;
        offsetZ = dir.getStepZ() * dist;
    }

}
