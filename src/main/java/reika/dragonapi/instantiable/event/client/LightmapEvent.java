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


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;


public class LightmapEvent extends Event {

	public static void fire() {
		MinecraftForge.EVENT_BUS.post(new LightmapEvent());
	}

}
