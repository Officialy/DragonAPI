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

import net.minecraftforge.fml.ModList;

public class ModHandlerException extends DragonAPIException {

    public ModHandlerException(ModList mod) {
        message.append("You cannot call a mod handler before its parent mod initializes!\n");
        message.append("Target mod: " + mod);
        this.crash();
    }

}
