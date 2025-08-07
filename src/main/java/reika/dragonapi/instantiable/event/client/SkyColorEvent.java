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


import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.Event;


public class SkyColorEvent extends Event {

	public final int originalColor;
	public int color;

	public SkyColorEvent(int c) {
		originalColor = c;
		color = originalColor;
	}

	public static int fire(int c) {
		SkyColorEvent evt = new SkyColorEvent(c);
		NeoForge.EVENT_BUS.post(evt);
		return evt.color;
	}

}

