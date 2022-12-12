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


import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;


public class WeatherSkyStrengthEvent extends Event {

	public final Level world;
	public final float originalStrength;
	public final float partialTickTime;

	public float returnValue;

	public WeatherSkyStrengthEvent(Level world, float f, float ptick) {
		this.world = world;
		originalStrength = returnValue = f;
		partialTickTime = ptick;
	}

	public static float fire_Rain(Level world, float ptick) {
		WeatherSkyStrengthEvent evt = new WeatherSkyStrengthEvent(world, world.getRainLevel(ptick), ptick);
		MinecraftForge.EVENT_BUS.post(evt);
		return evt.returnValue;
	}

	public static float fire_Thunder(Level world, float ptick) {
		WeatherSkyStrengthEvent evt = new WeatherSkyStrengthEvent(world, world.getThunderLevel(ptick), ptick);
		MinecraftForge.EVENT_BUS.post(evt);
		return evt.returnValue;
	}

}
