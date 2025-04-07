package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import reika.dragonapi.interfaces.IBlockRenderer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


// Used CodeChickenLib as a reference on how to do this. Forge really should have this sort of thing in-built smh
// Thanks covers1624!
public class ReikaRenderDispatcher extends BlockRenderDispatcher {

    private static final Map<Block, IBlockRenderer> blockRenderers = new HashMap<>();
    private static final List<IBlockRenderer> renderers = new ArrayList<>();
    public final BlockRenderDispatcher parentDispatcher;

    public ReikaRenderDispatcher(BlockRenderDispatcher parentDispatcher, BlockEntityWithoutLevelRenderer renderer, BlockColors p_173401_) {
        super(parentDispatcher.getBlockModelShaper(), renderer, p_173401_);
        this.parentDispatcher = parentDispatcher;
    }

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher parentDispatcher = mc.getBlockRenderer();

        mc.blockRenderer = new ReikaRenderDispatcher(parentDispatcher, new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()), mc.getBlockColors());
    }

    public static synchronized void registerBlockRenderer(Block block, IBlockRenderer renderer) {
        blockRenderers.computeIfAbsent(block, key -> renderer);
    }

    public static synchronized void registerRenderer(IBlockRenderer renderer) {
        renderers.add(renderer);
    }

    @Override
    public void renderBatched(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack stack, VertexConsumer builder, boolean checkSides, RandomSource rand, ModelData modelData, RenderType renderType) {
        try {
            IBlockRenderer renderer = findFor(state.getBlock(), iBlockRenderer -> iBlockRenderer.shouldRender(state, level, pos, renderType));
            if (renderer != null) {
                renderer.renderBlock(state, pos, level, stack, builder);
            }
        } catch (Throwable t) {
            CrashReport crashreport = CrashReport.forThrowable(t, "Tessellating DragonAPI block in world" + "block is:" + state);
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tessellated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            throw new ReportedException(crashreport);
        }
        try {
            parentDispatcher.renderBatched(state, pos, level, stack, builder, checkSides, rand, modelData, renderType);
        } catch (Throwable t) {
            throw t;
        }
    }

    @Nullable
    static IBlockRenderer findFor(Block block, Predicate<IBlockRenderer> predicate) {
        IBlockRenderer found = blockRenderers.get(block);
//        DragonAPI.LOGGER.info(found);
        if (found != null && predicate.test(found)) {
            return found;
        }

        return renderers.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

}
