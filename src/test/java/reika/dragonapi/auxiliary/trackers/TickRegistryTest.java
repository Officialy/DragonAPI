package reika.dragonapi.auxiliary.trackers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import reika.dragonapi.auxiliary.trackers.TickRegistry.Phase;
import reika.dragonapi.auxiliary.trackers.TickRegistry.TickType;

import static org.junit.jupiter.api.Assertions.*;

class TickRegistryTest {
    private static class Handler implements TickRegistry.TickHandler {
        final EnumSet<TickType> types;
        final EnumSet<Phase> phases;
        final List<List<Object>> calls = new ArrayList<>();
        Handler(EnumSet<TickType> types, EnumSet<Phase> phases) { this.types = types; this.phases = phases; }
        public void tick(TickType type, Object... data) { calls.add(List.of(type, List.of(data))); }
        public EnumSet<TickType> getType() { return types; }
        public boolean canFire(Phase phase) { return phases.contains(phase); }
        public String getLabel() { return "test handler"; }
    }

    @Test void renderDispatchRestoresFloatAndPhasePositionsAndFiltersBothPhases() {
        var registry = TickRegistry.instance;
        var start = new Handler(EnumSet.of(TickType.RENDER), EnumSet.of(Phase.START));
        var end = new Handler(EnumSet.of(TickType.RENDER), EnumSet.of(Phase.END));
        var both = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class));
        for (var handler : List.of(start, end, both)) registry.registerTickHandler(handler);
        try {
            registry.fireRender(0.25F, Phase.START); registry.fireRender(0.75F, Phase.END);
            assertEquals(List.of(List.of(TickType.RENDER, List.of(0.25F, Phase.START))), start.calls);
            assertEquals(List.of(List.of(TickType.RENDER, List.of(0.75F, Phase.END))), end.calls);
            assertEquals(2, both.calls.size());
        } finally { for (var handler : List.of(start, end, both)) registry.unregisterTickHandler(handler); }
    }

    @Test void clientDispatchAndArgumentArraysRemainIndependentAcrossHandlers() {
        var registry = TickRegistry.instance;
        var mutator = new Handler(EnumSet.of(TickType.CLIENT), EnumSet.allOf(Phase.class)) {
            @Override public void tick(TickType type, Object... data) { data[0] = null; }
        };
        var observer = new Handler(EnumSet.of(TickType.CLIENT), EnumSet.allOf(Phase.class));
        registry.registerTickHandler(mutator); registry.registerTickHandler(observer);
        try {
            registry.fireClient(Phase.START); registry.fireClient(Phase.END);
            assertEquals(List.of(List.of(TickType.CLIENT, List.of(Phase.START)), List.of(TickType.CLIENT, List.of(Phase.END))), observer.calls);
        } finally { registry.unregisterTickHandler(mutator); registry.unregisterTickHandler(observer); }
    }

    @Test void reentrantRegistrationUsesStableSnapshotsAndUnregistrationKeepsIdentityAndMultiplicity() {
        var registry = TickRegistry.instance;
        var added = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class));
        var registrar = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class)) {
            @Override public void tick(TickType type, Object... data) { registry.registerTickHandler(added); registry.unregisterTickHandler(this); }
        };
        registry.registerTickHandler(registrar);
        try {
            registry.fireRender(0.1F, Phase.START);
            assertTrue(added.calls.isEmpty());
            registry.fireRender(0.2F, Phase.END);
            assertEquals(1, added.calls.size());
            registry.registerTickHandler(added);
            registry.fireRender(0.3F, Phase.START);
            assertEquals(3, added.calls.size());
            registry.unregisterTickHandler(added);
            registry.fireRender(0.4F, Phase.END);
            assertEquals(3, added.calls.size());
        } finally { registry.unregisterTickHandler(registrar); registry.unregisterTickHandler(added); }
        var first = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class)) {
            @Override public boolean equals(Object other) { return other instanceof Handler; }
            @Override public int hashCode() { return 1; }
        };
        var second = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class));
        registry.registerTickHandler(first); registry.registerTickHandler(second);
        try {
            registry.unregisterTickHandler(first); registry.fireRender(0.5F, Phase.START);
            assertTrue(first.calls.isEmpty()); assertEquals(1, second.calls.size());
        } finally { registry.unregisterTickHandler(first); registry.unregisterTickHandler(second); }
    }

    @Test void handlerFailuresPropagateAndNullInputsFailBeforeDispatch() {
        var registry = TickRegistry.instance;
        RuntimeException failure = new IllegalStateException("expected tick failure");
        var handler = new Handler(EnumSet.of(TickType.RENDER), EnumSet.allOf(Phase.class)) {
            @Override public void tick(TickType type, Object... data) { throw failure; }
        };
        registry.registerTickHandler(handler);
        try { assertSame(failure, assertThrows(IllegalStateException.class, () -> registry.fireRender(0, Phase.START))); }
        finally { registry.unregisterTickHandler(handler); }
        assertThrows(NullPointerException.class, () -> registry.registerTickHandler(null));
        assertThrows(NullPointerException.class, () -> registry.fireRender(0, null));
    }
}
