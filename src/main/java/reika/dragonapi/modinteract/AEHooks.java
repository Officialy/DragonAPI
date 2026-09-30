package reika.dragonapi.modinteract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

import reika.dragonapi.ModList;

/**
 * AE-free entry point for the Applied Energistics 2 integration. Every method here is safe to call without AE2
 * installed; anything that touches AE classes is delegated to {@link AECompat}, which is only ever loaded once
 * {@link ModList#APPENG} is confirmed present.
 *
 * <p>This replaces what 1.7.10 did with {@code @Strippable}/{@code @ModDependent}: machines implement the AE-free
 * {@link reika.dragonapi.interfaces.blockentity.MEGridHost} and register their block entity type here, and the AE
 * grid node host capability is attached to those types only when AE is loaded.
 */
public final class AEHooks {

	private static final List<Supplier<? extends BlockEntityType<?>>> gridHosts = new ArrayList<>();

	private AEHooks() {

	}

	/** Called from a mod's constructor for each block entity type implementing {@code MEGridHost}. */
	public static void registerGridHost(Supplier<? extends BlockEntityType<?>> type) {
		gridHosts.add(type);
	}

	public static List<Supplier<? extends BlockEntityType<?>>> getGridHosts() {
		return Collections.unmodifiableList(gridHosts);
	}

	/** Called once from DragonAPI's constructor. */
	public static void init(IEventBus modEventBus) {
		if (ModList.APPENG.isLoaded()) {
			AECompat.init(modEventBus);
		}
	}

}
