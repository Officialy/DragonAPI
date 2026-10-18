package reika.dragonapi.instantiable.rendering;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TessellatorVertexListTest {
    @Test void explicitSinkPreservesKindsPrecisionAndOrder() {
        var list = new TessellatorVertexList();
        list.addVertex(1, 2, 3);
        list.addVertexWithUV(4, 5, 6, 0.1234567890123, 0.9);
        list.addVertexWithUVColor(7, 8, 9, 0.3, 0.4, 0x123456);
        var emitted = new ArrayList<TessellatorVertexList.VertexData>();
        list.render(emitted::add);
        assertEquals(list.snapshot(), emitted);
        assertFalse(emitted.get(0).hasUV()); assertFalse(emitted.get(0).hasColor());
        assertEquals(0.1234567890123, emitted.get(1).u());
        assertTrue(emitted.get(1).hasUV()); assertFalse(emitted.get(1).hasColor());
        assertEquals(0x123456, emitted.get(2).color()); assertTrue(emitted.get(2).hasColor());
    }

    @Test void immutableSnapshotSurvivesMutationStackAndReentrantEmission() {
        var list = new TessellatorVertexList(); list.vertex(1, 2, 3); list.vertex(4, 5, 6);
        var snapshot = list.snapshot();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        var emitted = new ArrayList<TessellatorVertexList.VertexData>();
        list.render(vertex -> { emitted.add(vertex); list.clear(); });
        assertEquals(snapshot, emitted);
        list.vertex(8, 9, 10); list.push(); list.vertex(20, 30, 40); list.pop();
        assertEquals(8, list.snapshot().getFirst().x());
        list.offset(10, 20, 30); list.clear();
        assertEquals(1, snapshot.getFirst().x()); assertEquals(2, snapshot.size());
        assertThrows(IllegalStateException.class, list::pop);
    }

    @Test void transformsKeepUvColorAndOriginalWindingRules() {
        var list = new TessellatorVertexList(10, 20, 30);
        list.addVertexWithUVColor(11, 22, 33, .25, .75, 0xABCDEF); list.vertex(14, 25, 36);
        list.rotateYtoZ();
        assertEquals(14, list.snapshot().getFirst().x());
        assertEquals(26, list.snapshot().getFirst().y());
        assertEquals(35, list.snapshot().getFirst().z());
        var colored = list.snapshot().getLast();
        assertEquals(0xABCDEF, colored.color()); assertEquals(.25, colored.u());
        assertEquals(23, colored.y()); assertEquals(32, colored.z());
        list.invertX();
        assertEquals(-10, list.snapshot().getFirst().x());
        assertTrue(list.snapshot().getFirst().hasColor());
        list.scale(2, 3, 4); list.clamp(-21, 0, 0, 0, 70, 130);
        assertEquals(-20, list.snapshot().getFirst().x());
        assertEquals(69, list.snapshot().getFirst().y());
        assertEquals(128, list.snapshot().getFirst().z());
        list.center();
        assertEquals(0, list.snapshot().stream().mapToDouble(TessellatorVertexList.VertexData::x).sum(), 1e-10);
        assertEquals(0, list.snapshot().stream().mapToDouble(TessellatorVertexList.VertexData::y).sum(), 1e-10);
        assertEquals(0, list.snapshot().stream().mapToDouble(TessellatorVertexList.VertexData::z).sum(), 1e-10);
    }

    @Test void singleVertexCopyAndExplicitDestinationRemainIndependent() {
        var vertex = new TessellatorVertexList.TessellatorVertex(1, 2, 3, .1, .2, 0xABCDEF);
        var copy = vertex.copy(); vertex.offset(10, 20, 30);
        var emitted = new ArrayList<TessellatorVertexList.VertexData>(); copy.addToTessellator(emitted::add);
        assertEquals(1, emitted.getFirst().x()); assertTrue(emitted.getFirst().hasColor());
        assertEquals(0xABCDEF, emitted.getFirst().color());
        assertThrows(NullPointerException.class, () -> copy.addToTessellator(null));
    }
}
