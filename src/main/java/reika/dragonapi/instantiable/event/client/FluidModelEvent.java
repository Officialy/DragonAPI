package reika.dragonapi.instantiable.event.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The fluid half of V33a {@code BlockIconEvent}: lets a mod swap the textures a fluid is drawn with at
 * one position.
 *
 * <p>DragonAPI 1.7.10 let listeners replace a block's icon per position; ChromatiCraft used it to draw
 * the Luminous Cliffs' clear shallows with their own water texture. A 26.2 fluid is drawn with one
 * {@link FluidModel} per fluid state, and NeoForge will not let a mod replace vanilla water's, so
 * {@code MixinFluidRenderer} offers the model here, per position, as the fluid is meshed.
 *
 * <p>Fired on chunk-meshing worker threads, once per fluid block per mesh build. Listeners must be
 * thread-safe and must not load chunks.
 */
public class FluidModelEvent extends Event {

	public final BlockAndTintGetter access;
	public final BlockPos pos;
	public final BlockState blockState;
	public final FluidState fluidState;
	public final FluidModel originalModel;

	public FluidModel model;

	public FluidModelEvent(BlockAndTintGetter access, BlockPos pos, BlockState blockState, FluidState fluidState, FluidModel model) {
		this.access = access;
		this.pos = pos;
		this.blockState = blockState;
		this.fluidState = fluidState;
		this.originalModel = this.model = model;
	}

	public static FluidModel fire(BlockAndTintGetter access, BlockPos pos, BlockState blockState, FluidState fluidState, FluidModel model) {
		return NeoForge.EVENT_BUS.post(new FluidModelEvent(access, pos, blockState, fluidState, model)).model;
	}
}
