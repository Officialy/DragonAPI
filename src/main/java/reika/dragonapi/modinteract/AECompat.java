package reika.dragonapi.modinteract;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import appeng.api.AECapabilities;
import reika.dragonapi.instantiable.modinteract.BasicAEInterface;
import reika.dragonapi.interfaces.blockentity.MEGridHost;
import reika.dragonapi.modinteract.deepinteract.MESystemReader;

/**
 * The AE-typed half of {@link AEHooks}. Only loaded when Applied Energistics 2 is present.
 */
final class AECompat {

	private AECompat() {

	}

	static void init(IEventBus modEventBus) {
		modEventBus.addListener(AECompat::registerCapabilities);
		//1.7.10 DragonAPIInit.load: MESystemReader.registerEffectHandler()
		NeoForge.EVENT_BUS.addListener(AECompat::onServerTick);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		for (Supplier<? extends BlockEntityType<?>> type : AEHooks.getGridHosts()) {
			event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type.get(), (be, ctx) -> {
				if (be instanceof MEGridHost host && host.getAEInterface() instanceof BasicAEInterface ae)
					return ae;
				return null;
			});
		}
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		MESystemReader.tickEffects(event.getServer());
	}

}
