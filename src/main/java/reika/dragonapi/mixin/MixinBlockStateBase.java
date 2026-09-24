package reika.dragonapi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import reika.dragonapi.instantiable.event.ItemLeafCollisionEvent;

/**
 * Fires {@link ItemLeafCollisionEvent} when a dropped item asks a leaf block for its collision shape.
 * This is a hot method, so the event is only built after the cheap entity-type and tag tests pass.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase {

	@Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("HEAD"), cancellable = true)
	private void dragonapi$itemLeafCollision(BlockGetter level, BlockPos pos, CollisionContext context,
			CallbackInfoReturnable<VoxelShape> callback) {
		if (!(context instanceof EntityCollisionContext entityContext)
				|| !(entityContext.getEntity() instanceof ItemEntity item))
			return;
		BlockState state = (BlockState)(Object)this;
		if (state.is(BlockTags.LEAVES) && ItemLeafCollisionEvent.fire(level, pos, state, item))
			callback.setReturnValue(Shapes.empty());
	}
}
