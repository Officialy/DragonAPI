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

import java.io.File;


public class InvalidBuildException extends DragonAPIException {

    public InvalidBuildException(DragonAPIMod mod, File jar) {
        this(mod, jar, null);
    }

    public InvalidBuildException(DragonAPIMod mod, File jar, String detail) {
        message.append(mod.getDisplayName() + " is an invalid JarFile:\n");
        message.append(jar.getPath() + " is likely not a valid compiled copy of the mod.\n");
        if (detail != null) {
            message.append(detail + "\n");
        }
        message.append("If you are attempting to make a custom build of the code, consult " + mod.getModAuthorName() + ".\n");
        message.append("Note that not all mods permit this, or of distributed versions of custom code, for security reasons.\n");
        message.append("If you got this by editing the mod jar, you may have to redownload the mod. Consult the developer for further questions.");
        this.crash();
    }

}
