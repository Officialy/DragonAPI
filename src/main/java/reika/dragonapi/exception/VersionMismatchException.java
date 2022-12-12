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
import net.minecraftforge.forgespi.language.IModInfo;

public class VersionMismatchException extends DragonAPIException {

    public VersionMismatchException(DragonAPIMod mod, IModInfo.ModVersion v, DragonAPI mod2, IModInfo.ModVersion v2, String req) {
        this(mod, v, mod2.getDisplayName(), v2, req);
    }

    public VersionMismatchException(DragonAPIMod mod, IModInfo.ModVersion v, String mod2, IModInfo.ModVersion v2, String req) {
        message.append(mod.getDisplayName() + " was not installed correctly:\n");
        message.append(mod.getDisplayName() + " " + v + " was installed with " + mod2 + " " + v2 + "\n");
        //if (v.majorVersion != v2.majorVersion) {  -TODO Fix this major version matching
        //    message.append("The major version numbers must match!\n");
        //} else {
            message.append("Version " + v + " of " + mod.getDisplayName() + " cannot run with " + mod2 + " " + v2 + "\n");
            message.append("Use " + req + " instead!\n");
        //}
        //message.append("This is not a "+mod.getDisplayName()+" bug. Do not post it to "+mod.getDocumentationSite().toString()+" unless you are really stuck.");
        this.crash();
    }

    public static final class APIMismatchException extends VersionMismatchException {

        public APIMismatchException(DragonAPIMod mod, IModInfo.ModVersion version, IModInfo.ModVersion api, String req) {
            super(mod, version, "DragonAPI", api, req);
        }

    }

}
