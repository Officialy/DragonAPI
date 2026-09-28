package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

/**
 * Lets listeners replace the block an item's {@code BLOCK_TRANSFORMER} component is about to place
 * (tilling, stripping, flattening and the like).
 *
 * <p>26.2 exposed these tool actions through NeoForge's {@code BlockToolModificationEvent}. 26.3
 * moved tilling, stripping and flattening onto data-driven block transformers and removed the
 * matching {@code ItemAbilities} (such as {@code HOE_TILL}), so that event no longer fires for them.
 * {@code MixinBlockTransformer} posts this instead, once the transformer has chosen a state and
 * before the block is placed. Only posted with a real {@link Level}.
 */
public class BlockTransformResultEvent extends WorldPositionEvent {

	public final UseOnContext context;
	public final BlockState originalState;
	private BlockState result;

	public BlockTransformResultEvent(Level world, BlockPos pos, UseOnContext context, BlockState originalState, BlockState result) {
		super(world, pos);
		this.context = context;
		this.originalState = originalState;
		this.result = result;
	}

	/** The state the transformer will place. */
	public BlockState getResult() {
		return result;
	}

	public void setResult(BlockState result) {
		this.result = result;
	}

	/** @return the state to place: the transformer's choice unless a listener replaced it */
	public static BlockState fire(UseOnContext context, BlockState result) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		return NeoForge.EVENT_BUS.post(new BlockTransformResultEvent(world, pos, context, world.getBlockState(pos), result)).getResult();
	}
}
