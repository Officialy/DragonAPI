/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;

 import reika.dragonapi.ModList;
 import reika.dragonapi.interfaces.registry.CropHandler;
 import reika.dragonapi.interfaces.registry.ModEntry;

public interface CustomCropHandler extends CropHandler {

    /**
     * Use a {@link ModList} entry if it already exists. Else, just create your own ModEntry object.
     */
    ModEntry getMod();

    /**
     * Used for displays like the GPR.
     */
    int getColor();

    /**
     * Be careful not to conflict with anything!
     */
    String getEnumEntryName();

    /**
     * Whether the crop is a BlockEntity and that data affects harvesting.
     */
    boolean isBlockEntity();

}
