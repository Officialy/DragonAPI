package reika.dragonapi.instantiable.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Asked when a dropped item collides with a leaf block (anything in {@code #minecraft:leaves}). Cancel it
 * and the item falls straight through that block.
 *
 * <p>1.7.10 DragonAPI hooked {@code Entity.moveEntity}'s collision-box gathering and let mods drop boxes
 * per entity; ChromatiCraft used it so Fertility Seeds shaken loose from Glow Roots fall through the
 * canopy. 26.2 asks each block state for its collision shape with a {@code CollisionContext} naming the
 * moving entity, so {@code MixinBlockStateBase} fires this there, only for item entities and leaves.
 */
public class ItemLeafCollisionEvent extends Event implements ICancellableEvent {

	public final BlockGetter access;
	public final BlockPos pos;
	public final BlockState state;
	public final ItemEntity item;

	public ItemLeafCollisionEvent(BlockGetter access, BlockPos pos, BlockState state, ItemEntity item) {
		this.access = access;
		this.pos = pos;
		this.state = state;
		this.item = item;
	}

	/** @return whether the item should pass through the block */
	public static boolean fire(BlockGetter access, BlockPos pos, BlockState state, ItemEntity item) {
		return NeoForge.EVENT_BUS.post(new ItemLeafCollisionEvent(access, pos, state, item)).isCanceled();
	}
}
