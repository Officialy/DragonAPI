/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.blockentity;

public interface SidePlacedTile {

    void placeOnSide(int s);

    boolean checkLocationValidity();

    void drop();

}
