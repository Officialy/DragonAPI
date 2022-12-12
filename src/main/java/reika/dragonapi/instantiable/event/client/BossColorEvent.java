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


public class BossColorEvent extends Event {

	private static float cachedRed;
	private static float cachedGreen;
	private static float cachedBlue;
	public final float originalRed = 0.7F;
	public final float originalGreen = 0.6F;
	public final float originalBlue = 0.6F;
	public final boolean isLightmap;
	public float red = originalRed;
	public float green = originalGreen;
	public float blue = originalBlue;

	public BossColorEvent(boolean light) {
		isLightmap = light;
	}

	public static float fireAndReturnRed_Light() {
		fire(true);
		return cachedRed;
	}

	public static float fireAndReturnRed() {
		fire(false);
		return cachedRed;
	}

	public static float returnGreen() {
		return cachedGreen;
	}

	public static float returnBlue() {
		return cachedBlue;
	}

	private static void fire(boolean b) {
		BossColorEvent evt = new BossColorEvent(b);
		MinecraftForge.EVENT_BUS.post(evt);
		cachedRed = evt.red;
		cachedGreen = evt.green;
		cachedBlue = evt.blue;
	}

	public static void reset() {
		BossColorResetEvent evt = new BossColorResetEvent();
		MinecraftForge.EVENT_BUS.post(evt);
	}

	public static class BossColorResetEvent extends Event {

	}

}
