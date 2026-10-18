package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import reika.dragonapi.client.CapturedBlockGeometry;
import reika.dragonapi.interfaces.IBlockRenderer;
import static org.junit.jupiter.api.Assertions.*;

class ReikaRenderDispatcherTest {
    private static IBlockRenderer renderer() {
        return new IBlockRenderer() {
            @Override public boolean shouldRender(BlockState s, BlockAndTintGetter l, BlockPos p, ChunkSectionLayer layer) { return true; }
            @Override public void renderBlock(BlockState s, BlockPos p, BlockAndTintGetter l, PoseStack pose, VertexConsumer out) {
                for (int i=0;i<4;i++) out.addVertex(i,0,0).setColor(-1).setUv(0,0).setLight(0);
            }
        };
    }

    @Test void directFirstWinsThenOrderedPredicateFallbackAndOwnedRemoval() {
        var direct=renderer(); var replacement=renderer(); var first=renderer(); var second=renderer();
        try {
            ReikaRenderDispatcher.registerBlockRenderer(Blocks.BARRIER,direct);
            ReikaRenderDispatcher.registerBlockRenderer(Blocks.BARRIER,replacement);
            ReikaRenderDispatcher.registerRenderer(first); ReikaRenderDispatcher.registerRenderer(second);
            assertSame(direct,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->true));
            assertSame(first,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->r!=direct));
            assertSame(second,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->r==second));
            assertNull(ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->false));
            assertFalse(ReikaRenderDispatcher.unregisterBlockRenderer(Blocks.BARRIER,replacement));
            assertTrue(ReikaRenderDispatcher.unregisterBlockRenderer(Blocks.BARRIER,direct));
            assertSame(first,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->true));
            ReikaRenderDispatcher.registerRenderer(first);
            assertTrue(ReikaRenderDispatcher.unregisterRenderer(first));
            assertSame(second,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->true));
            assertTrue(ReikaRenderDispatcher.unregisterRenderer(first));
            assertFalse(ReikaRenderDispatcher.unregisterRenderer(first));
        } finally {
            ReikaRenderDispatcher.unregisterBlockRenderer(Blocks.BARRIER,direct);
            ReikaRenderDispatcher.unregisterRenderer(first); ReikaRenderDispatcher.unregisterRenderer(first);
            ReikaRenderDispatcher.unregisterRenderer(second);
        }
    }

    @Test void reentrantRegistrationChangesOnlySubsequentSelectionSnapshots() {
        var first=renderer(); var old=renderer(); var next=renderer();
        try {
            ReikaRenderDispatcher.registerRenderer(first); ReikaRenderDispatcher.registerRenderer(old);
            assertSame(old,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->{
                if (r==first) { ReikaRenderDispatcher.unregisterRenderer(old); ReikaRenderDispatcher.registerRenderer(next); }
                return r!=first;
            }));
            assertSame(next,ReikaRenderDispatcher.findFor(Blocks.BARRIER,r->r!=first));
        } finally { for (var r:List.of(first,old,next)) ReikaRenderDispatcher.unregisterRenderer(r); }
    }

    @Test void blockCaptureKeepsNativeFirstWriteAttributesAndImmutableCompleteQuads() {
        var capture=new CapturedBlockGeometry();
        for (int i=0;i<4;i++) {
            capture.addVertex(i,2,3).setColor(1,2,3,4).setColor(-1).setUv(.25F,.75F).setUv(0,0)
                    .setLight(0x00D000A0).setLight(0).setNormal(0,1,0).setOverlay(0).setUv3(0,0).setLineWidth(1);
        }
        var vertices=capture.finish();
        assertEquals(4,vertices.size()); assertEquals(0x04010203,vertices.getFirst().color());
        assertEquals(.25F,vertices.getFirst().u()); assertEquals(0x00D000A0,vertices.getFirst().light());
        assertEquals(3,vertices.getLast().x()); assertThrows(UnsupportedOperationException.class,vertices::clear);
        assertThrows(IllegalStateException.class,()->capture.addVertex(1,2,3));
        assertThrows(IllegalStateException.class,capture::finish);
    }

    @Test void invalidTerrainDataFailsAtCaptureAndEmptyGeometryRemainsEmpty() {
        assertTrue(new CapturedBlockGeometry().finish().isEmpty());
        var missing=new CapturedBlockGeometry(); missing.addVertex(0,0,0).setColor(-1).setUv(0,0);
        assertThrows(IllegalStateException.class,missing::finish);
        var partial=new CapturedBlockGeometry(); partial.addVertex(0,0,0).setColor(-1).setUv(0,0).setLight(0);
        assertThrows(IllegalStateException.class,partial::finish);
        assertThrows(IllegalStateException.class,()->new CapturedBlockGeometry().setUv(0,0));
        assertThrows(NullPointerException.class,()->ReikaRenderDispatcher.registerRenderer(null));
    }
}
