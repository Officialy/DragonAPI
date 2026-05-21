package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
// BlockEntityWithoutLevelRenderer no longer needed
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import reika.dragonapi.interfaces.IBlockRenderer;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Predicate;


// Used CodeChickenLib as a reference on how to do this. Forge really should have this sort of thing in-built smh
// Thanks covers1624!
public class ReikaRenderDispatcher extends BlockRenderDispatcher {

    private static final Map<Block, IBlockRenderer> blockRenderers = new HashMap<>();
    private static final List<IBlockRenderer> renderers = new ArrayList<>();
    public final BlockRenderDispatcher parentDispatcher;

    public ReikaRenderDispatcher(BlockRenderDispatcher parentDispatcher, BlockColors p_173401_) {
        super(parentDispatcher.getBlockModelShaper(), 
              getPrivateField(parentDispatcher, "materials"),
              getPrivateField(parentDispatcher, "specialBlockModelRenderer"),
              p_173401_);
        this.parentDispatcher = parentDispatcher;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object obj, String fieldName) {
        try {
            Class<?> c = obj.getClass();
            Field f = null;
            while (f == null && c != null) {
                try {
                    f = c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e2) {
                    c = c.getSuperclass();
                }
            }
            if (f == null) {
                throw new NoSuchFieldException("Could not find field " + fieldName);
            }
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field " + fieldName, e);
        }
    }

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher parentDispatcher = mc.getBlockRenderer();

        mc.blockRenderer = new ReikaRenderDispatcher(parentDispatcher, mc.getBlockColors());
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
