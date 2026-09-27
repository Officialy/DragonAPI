package reika.dragonapi.mixin;

import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The render targets of a picture-in-picture renderer. 26.3 moved the render pass into
 * {@code PictureInPictureRenderer#prepare}, so a renderer that must flush part of its geometry
 * before the rest (to blend against it) needs these to open its own pass on the same targets.
 */
@Mixin(PictureInPictureRenderer.class)
public interface PictureInPictureRendererAccessor {

    @Accessor("textureView")
    @Nullable GpuTextureView dragonapi$getTextureView();

    @Accessor("depthTextureView")
    @Nullable GpuTextureView dragonapi$getDepthTextureView();
}
