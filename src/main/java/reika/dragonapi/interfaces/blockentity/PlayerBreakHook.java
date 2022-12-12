/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.blockentity;

import net.minecraft.world.entity.player.Player;

public interface PlayerBreakHook {

    /**
     * Return false to cancel the block break.
     */
    boolean breakByPlayer(Player ep);

    boolean isBreakable(Player ep);

}
