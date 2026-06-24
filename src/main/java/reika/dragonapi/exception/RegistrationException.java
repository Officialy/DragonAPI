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
        this(mod.getTechnicalName(), mod.getModAuthorName(), msg, e);
    }

    // String-based overloads for mods that do not extend DragonAPIMod (e.g. the NeoForge ReactorCraft main class).
    public RegistrationException(String modName, String msg) {
        this(modName, "Reika", msg, null);
    }

    public RegistrationException(String modName, String authorName, String msg, Throwable e) {
        message.append(modName).append(" has a registration error:\n");
        message.append(msg).append("\n");
        message.append("Contact ").append(authorName).append(" immediately!\n");
        message.append("Include the following information:");
        if (e != null)
            this.initCause(e);
        this.crash();
    }

}
