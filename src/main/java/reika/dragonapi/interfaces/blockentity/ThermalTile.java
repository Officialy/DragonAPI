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

public interface ThermalTile {

    /** Some machine families implement thermal state only for particular variants. */
    default boolean hasTemperature() {
        return true;
    }

    int getTemperature();

    void setTemperature(int temp);

}
