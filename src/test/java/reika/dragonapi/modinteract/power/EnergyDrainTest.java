package reika.dragonapi.modinteract.power;

import org.junit.jupiter.api.Test;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import static org.junit.jupiter.api.Assertions.*;

class EnergyDrainTest {
    @Test void nativeStoresRetainForcedDrainingAndNotifyOnlyActualChanges() {
        class Store extends SimpleEnergyHandler {
            int changes;
            Store() { super(100, 100, 0, 40); }
            @Override protected void onEnergyChanged(int previous) { changes++; }
        }
        var store = new Store();
        ReikaRFHelper.drainStorage(store, 15);
        assertEquals(25, store.getAmountAsLong()); assertEquals(1, store.changes);
        ReikaRFHelper.drainStorage(store, 100);
        assertEquals(0, store.getAmountAsLong()); assertEquals(2, store.changes);
        ReikaRFHelper.drainStorage(store, 0); ReikaRFHelper.drainStorage(store, -1); ReikaRFHelper.drainStorage(store, 5);
        assertEquals(0, store.getAmountAsLong()); assertEquals(2, store.changes);
    }
    @Test void unknownStoresUseTransactionsAndDoNotTouchUnrelatedEnergyFields() {
        var store = new Store(); long before = store.energy;
        ReikaRFHelper.drainStorage(store, 10);
        assertEquals(before - 3, store.energy); assertEquals(1, store.commits);
        assertEquals(123, store.energyFrequency);
        store.invalidResult = true;
        assertThrows(IllegalStateException.class, () -> ReikaRFHelper.drainStorage(store, 10));
        assertEquals(before - 3, store.energy, "invalid extraction must roll back");
        assertEquals(1, store.commits);
    }
    @Test void explicitAdaptersUseThePublicForcedDrainContract() {
        class Restricted extends Store {
            int force(int limit) { int n = (int)Math.min(energy, limit); energy -= n; commits++; return n; }
            @Override public int extract(int amount, TransactionContext context) { return 0; }
        }
        var store = new Restricted();
        ReikaRFHelper.registerEnergyDrainAdapter(Restricted.class, Restricted::force);
        long before = store.energy; ReikaRFHelper.drainStorage(store, 10);
        assertEquals(before - 10, store.energy); assertEquals(1, store.commits);
        assertThrows(IllegalStateException.class, () -> ReikaRFHelper.registerEnergyDrainAdapter(Restricted.class, Restricted::force));
        assertThrows(IllegalArgumentException.class, () -> ReikaRFHelper.registerEnergyDrainAdapter(String.class, (s,n) -> 0));
    }
    @Test void allCapabilityViewsShareOneBudgetAndAliasesAreVisitedOnce() {
        var first = new Store(); var second = new Store();
        var views = java.util.Arrays.asList(first, first, null, second, second, first, second);
        assertEquals(5, ReikaRFHelper.drainHandlers(views, 5));
        assertEquals((long)Integer.MAX_VALUE + 37, first.energy);
        assertEquals((long)Integer.MAX_VALUE + 38, second.energy);
        assertEquals(1, first.commits); assertEquals(1, second.commits);
        var nativeStore = new SimpleEnergyHandler(100,100,0,100);
        assertEquals(10, ReikaRFHelper.drainHandlers(java.util.List.of(nativeStore,nativeStore),10));
        assertEquals(90,nativeStore.getAmountAsLong());
    }
    private static class Store extends SnapshotJournal<Long> implements EnergyHandler {
        long energy = (long)Integer.MAX_VALUE + 40;
        int energyFrequency = 123, commits;
        boolean invalidResult;
        public long getAmountAsLong() { return energy; }
        public long getCapacityAsLong() { return Long.MAX_VALUE; }
        public int insert(int amount, TransactionContext context) { return 0; }
        public int extract(int amount, TransactionContext context) {
            int n = Math.min(amount, 3); updateSnapshots(context); energy -= n;
            return invalidResult ? amount + 1 : n;
        }
        protected Long createSnapshot() { return energy; }
        protected void revertToSnapshot(Long value) { energy = value; }
        protected void onRootCommit(Long previous) { commits++; }
    }
}
