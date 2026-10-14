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

public class MissingASMException extends DragonAPIException {

    public MissingASMException(DragonAPIMod mod) {
        message.append(DragonAPI.NAME + " is missing its ASM transformers.\n");
        message.append("This should never happen, and is likely caused by a jar manifest failure.\n");
        message.append("If you got this by editing the mod jar, you may have to redownload the mod. Consult the developer for further questions.");
        this.crash();
    }

}
