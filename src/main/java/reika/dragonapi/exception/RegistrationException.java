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

import reika.dragonapi.base.DragonAPIMod;

public class RegistrationException extends DragonAPIException {
    //todo fix mod being null
    public RegistrationException(DragonAPIMod mod, String msg) {
        this(mod, msg, null);
    }

    public RegistrationException(DragonAPIMod mod, String msg, Throwable e) {
        message.append(mod.getTechnicalName()).append(" has a registration error:\n");
        message.append(msg).append("\n");
        message.append("Contact ").append(mod.getModAuthorName()).append(" immediately!\n");
        message.append("Include the following information:");
        if (e != null)
            this.initCause(e);
        this.crash();
    }

}
