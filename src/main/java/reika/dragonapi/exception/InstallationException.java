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


import reika.dragonapi.ModList;
import reika.dragonapi.base.DragonAPIMod;

public class InstallationException extends DragonAPIException {

    public InstallationException(DragonAPIMod mod, String msg) {
        message.append(mod.getDisplayName()).append(" was not installed correctly:\n");
        message.append(msg).append("\n");
        //message.append("Try consulting "+mod.getDocumentationSite().toString()+"for information.\n");
        message.append("This is not a ").append(mod.getDisplayName()).append(" bug. Do not post it to forums")/*.append(mod.todo getDocumentationSite().toString())*/.append(" unless you are really stuck.");
        this.crash();
    }

    public InstallationException(String modname, String msg) {
        message.append(modname + " was not installed correctly:\n");
        message.append(msg + "\n");
        message.append("Try consulting the mod website for information.\n");
        message.append("This is not a bug. Do not post it unless you are really stuck.");
        this.crash();
    }

    public InstallationException(ModList mod, String msg) {
        message.append(mod.name() + " was not installed correctly:\n");
        message.append(msg + "\n");
        message.append("This is not a mod bug. Do not post it to the forums/websites unless you are really stuck.");
        this.crash();
    }

    public InstallationException(DragonAPIMod mod, Exception e) {
        super(e);
        message.append(mod.getDisplayName() + " was not installed correctly:\n");
    }

}
