/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.client;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.eventbus.api.Event;


public class ClientLoginEvent extends Event {

    public final Player player;

    public final boolean newLogin;

    public ClientLoginEvent(Player ep, boolean log) {
        player = ep;
        newLogin = log;
    }

}
