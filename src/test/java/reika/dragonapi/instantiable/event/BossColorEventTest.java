package reika.dragonapi.instantiable.event;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.event.client.BossColorEvent;

import static org.junit.jupiter.api.Assertions.*;

class BossColorEventTest {
    @Test void defaultsAndMutableListenerFieldsProduceAnIndependentImmutableResult() {
        assertEquals(new BossColorEvent.Color(0.7F, 0.6F, 0.6F), BossColorEvent.fire(false));
        var captured = new AtomicReference<BossColorEvent>();
        Consumer<BossColorEvent> listener = event -> {
            assertTrue(event.isLightmap);
            assertEquals(0.7F, event.originalRed); assertEquals(0.6F, event.originalGreen); assertEquals(0.6F, event.originalBlue);
            event.red = 0.2F; event.green = 0.3F; event.blue = 0.4F; captured.set(event);
        };
        NeoForge.EVENT_BUS.addListener(BossColorEvent.class, listener);
        try {
            var result = BossColorEvent.fire(true);
            captured.get().green = 99;
            assertEquals(new BossColorEvent.Color(0.2F, 0.3F, 0.4F), result);
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
    }

    @Test void nestedFogAndLightmapDispatchesCannotOverwriteEachOthersChannels() {
        var inner = new AtomicReference<BossColorEvent.Color>();
        Consumer<BossColorEvent> listener = event -> {
            if (event.isLightmap) { event.red = 0.1F; event.green = 0.2F; event.blue = 0.3F; }
            else {
                event.red = 0.4F;
                inner.set(BossColorEvent.fire(true));
                event.green = 0.5F; event.blue = 0.6F;
            }
        };
        NeoForge.EVENT_BUS.addListener(BossColorEvent.class, listener);
        try {
            var outer = BossColorEvent.fire(false);
            assertEquals(new BossColorEvent.Color(0.4F, 0.5F, 0.6F), outer);
            assertEquals(new BossColorEvent.Color(0.1F, 0.2F, 0.3F), inner.get());
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
    }

    @Test void concurrentPureListenersKeepEveryReturnedTupleIndependent() throws Exception {
        Consumer<BossColorEvent> listener = event -> {
            float value = event.isLightmap ? 0.25F : 0.75F;
            event.red = value; Thread.yield(); event.green = value + 1; Thread.yield(); event.blue = value + 2;
        };
        NeoForge.EVENT_BUS.addListener(BossColorEvent.class, listener);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { for (int i = 0; i < 500; i++) assertEquals(new BossColorEvent.Color(0.25F, 1.25F, 2.25F), BossColorEvent.fire(true)); });
            var second = executor.submit(() -> { for (int i = 0; i < 500; i++) assertEquals(new BossColorEvent.Color(0.75F, 1.75F, 2.75F), BossColorEvent.fire(false)); });
            first.get(30, TimeUnit.SECONDS); second.get(30, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); NeoForge.EVENT_BUS.unregister(listener); }
    }

    @Test void resetNotificationAndUnboundedOriginalFloatContractRemain() {
        var resets = new AtomicInteger();
        Consumer<BossColorEvent.BossColorResetEvent> reset = event -> resets.incrementAndGet();
        Consumer<BossColorEvent> color = event -> { event.red = -1; event.green = 2; event.blue = 0; };
        NeoForge.EVENT_BUS.addListener(BossColorEvent.BossColorResetEvent.class, reset);
        NeoForge.EVENT_BUS.addListener(BossColorEvent.class, color);
        try {
            assertEquals(new BossColorEvent.Color(-1, 2, 0), BossColorEvent.fire(false));
            BossColorEvent.reset(); BossColorEvent.reset(); assertEquals(2, resets.get());
        } finally { NeoForge.EVENT_BUS.unregister(reset); NeoForge.EVENT_BUS.unregister(color); }
        assertEquals(BossColorEvent.DEFAULT, BossColorEvent.fire(true));
    }
}
