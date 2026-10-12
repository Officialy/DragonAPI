package reika.dragonapi.instantiable.worldgen;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PopulationScopeTest {
    @Test void rectangularAdmissionStopsAtItsEdges() {
        var scope = PopulationScope.rectangle(-4, -3, 2, 8);
        assertEquals(84, scope.size());
        assertTrue(scope.contains(new ChunkPos(-4, -3)));
        assertTrue(scope.contains(new ChunkPos(2, 8)));
        assertFalse(scope.contains(new ChunkPos(-5, -3)));
        assertFalse(scope.contains(new ChunkPos(2, 9)));
        var copy = PopulationScope.load(scope.save());
        assertEquals(scope.size(), copy.size());
        assertFalse(copy.contains(new ChunkPos(3, 0)));
    }

    @Test void observedRootsSurviveNegativeRegionsAndDoNotAuthorizeTheirHalo() {
        var scope = PopulationScope.observedChunks();
        var points = java.util.List.of(new ChunkPos(-33, -32), new ChunkPos(-1, -1),
                new ChunkPos(0, 0), new ChunkPos(31, 31), new ChunkPos(32, 32));
        points.forEach(scope::observe);
        points.forEach(scope::observe);
        assertEquals(5, scope.size());
        var copy = PopulationScope.load(scope.save());
        assertEquals(5, copy.size());
        points.forEach(p -> assertTrue(copy.contains(p), "Missing " + p));
        assertFalse(copy.contains(new ChunkPos(-32, -32)));
        assertFalse(copy.contains(new ChunkPos(1, 0)));
        assertFalse(copy.contains(new ChunkPos(31, 32)));
    }
}
