/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.rendering.connected;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

import reika.dragonapi.DragonAPI;

/**
 * Registers DragonAPI's connected-texture blockstate-model codecs, making
 * {@code {"type": "dragonapi:connected_overlay", ...}} and
 * {@code {"type": "dragonapi:connected_axis", ...}} usable in any mod's blockstate JSON variants.
 */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class ConnectedModelRegistration {

	private ConnectedModelRegistration() {}

	@SubscribeEvent
	public static void register(RegisterBlockStateModels event) {
		event.registerModel(Identifier.fromNamespaceAndPath(DragonAPI.MODID, "connected_overlay"), OverlayConnectedModel.Unbaked.CODEC);
		event.registerModel(Identifier.fromNamespaceAndPath(DragonAPI.MODID, "connected_axis"), AxisConnectedModel.Unbaked.CODEC);
	}
}
