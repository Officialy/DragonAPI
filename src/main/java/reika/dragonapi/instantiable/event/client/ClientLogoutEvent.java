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

<<<<<<< Updated upstream:Instantiable/Event/Client/ClientLogoutEvent.java
import net.minecraft.entity.player.EntityPlayer;
=======
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.eventbus.api.Event;
>>>>>>> Stashed changes:src/main/java/reika/dragonapi/instantiable/event/client/ClientLogoutEvent.java

public class ClientLogoutEvent extends Event {

    public final Player player;

    public ClientLogoutEvent(Player ep) {
        player = ep;
    }

}
