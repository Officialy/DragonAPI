/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.entity;


/**
 * Ensure your entity writes isDead to NBT!
 */
public interface DestroyOnUnload {

    /**
     * Usually calls setDead
     */
    void destroy();

}
