/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.exception;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.DragonAPIMod;

public class MissingDependencyException extends DragonAPIException {

    public MissingDependencyException(DragonAPIMod mod, String mod2) {
        message.append(DragonAPI.NAME + " was not installed correctly:\n");
        message.append(DragonAPI.NAME + " was installed without its dependency " + mod2 + "!\n");
        message.append("This is not a " + DragonAPI.NAME + " bug. Do not post it to officialy.");
        this.crash();
    }

}
