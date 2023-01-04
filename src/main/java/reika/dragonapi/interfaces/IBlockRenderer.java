package reika.dragonapi.interfaces;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface IBlockRenderer {

    /**
     * Called to render a given BlockState at a given location.
     * @param state The {@link BlockState} to render
     * @param pos The {@link BlockPos} to render at
     * @param level The {@link BlockAndTintGetter}. Essentially a level
     * @param stack The {@link PoseStack}, can be used to rotate the model
     * @param buffer The {@link VertexConsumer} to add vertices to
     */
    void renderBlock(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack stack, VertexConsumer buffer);

    boolean shouldRender(BlockState blockState, BlockAndTintGetter world, BlockPos pos, @Nullable RenderType renderType);

}