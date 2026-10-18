package reika.dragonapi.interfaces;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only additive terrain geometry. Both methods execute on client main during section capture. */
public interface IBlockRenderer {

    /**
     * Called to render a given BlockState at a given location.
     * @param state The {@link BlockState} to render
     * @param pos The {@link BlockPos} to render at
     * @param level The {@link BlockAndTintGetter}. Essentially a level
     * @param stack The {@link PoseStack}, can be used to rotate the model
     * @param buffer The {@link VertexConsumer} to add vertices to
     * The pose is already translated from this block into section-local coordinates. Emit complete quads
     * with position, ARGB color, atlas UV0 and packed light; these are the native 26.3 BLOCK attributes.
     * World/resource lookups belong here. No renderer or live world is retained by the meshing callback.
     */
    void renderBlock(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack stack, VertexConsumer buffer);

    boolean shouldRender(BlockState blockState, BlockAndTintGetter world, BlockPos pos, ChunkSectionLayer layer);

}
