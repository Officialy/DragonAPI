package reika.dragonapi.auxiliary.trackers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.worldgen.PopulationScope;

import static org.junit.jupiter.api.Assertions.*;

class BoundedPopulationStatusTest {
    private static BoundedPopulation population() {
        return BoundedPopulation.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
    }

    @Test void snapshotsRemainStableWhileSessionsPauseResumeAndCancel() {
        var population = population();
        long id = population.begin("manual", "snapshot", PopulationScope.rectangle(-1, -1, 1, 1), false);
        population.registerQueue("test", origins -> 7);
        var before = population.statusSnapshot();
        assertEquals(new BoundedPopulation.PopulationStatus(id, "manual",
                BoundedPopulation.SessionState.FINISHING_STRUCTURES, 9, 7), before.getFirst());
        population.pause(id);
        assertEquals(BoundedPopulation.SessionState.PAUSED, population.statusSnapshot().getFirst().state());
        assertEquals(BoundedPopulation.SessionState.FINISHING_STRUCTURES, before.getFirst().state());
        assertThrows(UnsupportedOperationException.class, before::clear);
        population.resume(id);
        assertEquals(BoundedPopulation.SessionState.FINISHING_STRUCTURES, population.statusSnapshot().getFirst().state());
        population.cancel(id);
        assertEquals(BoundedPopulation.SessionState.CANCELLED, population.statusSnapshot().getFirst().state());
        assertEquals("#1 manual cancelled: 9 origin chunks, 7 pending jobs", population.status().getFirst());
    }

    @Test void feedingSnapshotDoesNotChangeAdmissionAndRoundTripKeepsPausedRecovery() {
        var population = population();
        long id = population.begin("chunky", "selection", PopulationScope.rectangle(0, 0, 0, 0), true);
        assertEquals(BoundedPopulation.SessionState.TERRAIN_RUNNING, population.statusSnapshot().getFirst().state());
        var saved = BoundedPopulation.CODEC.encodeStart(NbtOps.INSTANCE, population).getOrThrow();
        var restored = BoundedPopulation.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(BoundedPopulation.SessionState.PAUSED, restored.statusSnapshot().getFirst().state());
        assertEquals(id, restored.begin("chunky", "selection", PopulationScope.rectangle(0, 0, 0, 0), true));
        assertEquals(BoundedPopulation.SessionState.TERRAIN_RUNNING, restored.statusSnapshot().getFirst().state());
    }
}
