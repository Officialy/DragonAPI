/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

public interface PacketHandler {

    void handleData(ReikaPacketHelper.PacketObj packet, Level world, Player ep);

}
