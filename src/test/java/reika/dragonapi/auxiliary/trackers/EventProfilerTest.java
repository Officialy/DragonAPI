package reika.dragonapi.auxiliary.trackers;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.neoforged.bus.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EventProfilerTest {
    public static class Sample extends Event implements ICancellableEvent {}
    public static class Child extends Sample {}
    public static abstract class AbstractSample extends Event {}
    public static class Empty extends Event {}
    public static class Annotated {
        int calls;
        @SubscribeEvent(receiveCanceled = true) public void receive(Sample event) { calls++; }
    }
    private ProfilingEventBus bus() { return ProfilingEventBus.create(BusBuilder.builder()); }
    private void start(Class<? extends Event> type, IEventBus bus) {
        assertEquals(EventProfiler.ProfileStartStatus.SUCCESS, EventProfiler.startProfiling(type,bus));
    }
    @AfterEach void stop() { EventProfiler.finishProfiling(); }

    @Test void nativePriorityCancellationAndAnnotatedBodiesArePreserved() {
        var bus=bus(); var order=new ArrayList<String>(); var subscriber=new Annotated();
        bus.addListener(EventPriority.HIGH, Sample.class, e->{ order.add("high"); e.setCanceled(true); });
        bus.addListener(Sample.class,e->order.add("skipped"));
        bus.addListener(EventPriority.LOW,true,Sample.class,e->order.add("low"));
        bus.register(subscriber); start(Sample.class,bus); bus.post(new Sample());
        assertEquals(List.of("high","low"),order); assertEquals(1,subscriber.calls);
        var data=EventProfiler.snapshot(); assertEquals(1,data.eventFires()); assertEquals(3,data.handlers().size());
        assertEquals(3,data.handlers().stream().mapToLong(EventProfiler.EventProfile::getFireCount).sum());
        assertTrue(data.totalHandlerTime()>0); assertTrue(data.handlers().stream().anyMatch(p->p.identifier.contains("receive")));
        EventProfiler.finishProfiling(); bus.post(new Sample()); assertEquals(2,subscriber.calls);
    }
    @Test void nestedPostsReuseTheSameEventWithoutOverwritingTimers() {
        var bus=bus(); AtomicInteger calls=new AtomicInteger();
        bus.addListener(Sample.class,e->{ if(calls.incrementAndGet()==1) bus.post(e); });
        start(Sample.class,bus); bus.post(new Sample());
        assertEquals(2,calls.get()); assertEquals(2,EventProfiler.snapshot().eventFires());
        assertEquals(2,EventProfiler.snapshot().handlers().getFirst().getFireCount());
        assertTrue(EventProfiler.snapshot().handlers().getFirst().getAverageTime()>0);
    }
    @Test void dynamicRegistrationUnregistrationAndStoppedSnapshotsRetainNativeIdentity() {
        var bus=bus(); AtomicInteger calls=new AtomicInteger(); Consumer<Sample> first=e->calls.incrementAndGet();
        bus.addListener(Sample.class,first); start(Sample.class,bus); bus.post(new Sample());
        Consumer<Sample> second=e->calls.addAndGet(10); bus.addListener(Sample.class,second);
        bus.unregister(first); bus.post(new Sample()); assertEquals(11,calls.get());
        EventProfiler.finishProfiling(); var stopped=EventProfiler.snapshot();
        bus.unregister(second); bus.post(new Sample()); assertEquals(11,calls.get());
        assertEquals(2,stopped.eventFires()); assertEquals(2,stopped.handlers().size()); assertFalse(stopped.running());
        assertThrows(UnsupportedOperationException.class,()->stopped.handlers().clear());
        start(Sample.class,bus); bus.post(new Sample()); assertEquals(1,EventProfiler.snapshot().eventFires());
        assertTrue(EventProfiler.snapshot().handlers().isEmpty()); assertEquals(2,stopped.eventFires());
    }
    @Test void actualExceptionHandlerGetsNativeBusAndFailureIsMeasured() {
        var failure=new IllegalStateException("fixture"); var handled=new AtomicInteger();
        var bus=ProfilingEventBus.create(BusBuilder.builder().setExceptionHandler((b,e,l,i,t)->{
            assertInstanceOf(ProfilingEventBus.class,b); assertSame(failure,t); handled.incrementAndGet();
        }));
        bus.addListener(Sample.class,e->{throw failure;}); start(Sample.class,bus);
        assertSame(failure,assertThrows(IllegalStateException.class,()->bus.post(new Sample())));
        assertEquals(1,handled.get()); assertEquals(1,EventProfiler.snapshot().eventFires());
        assertEquals(1,EventProfiler.snapshot().handlers().getFirst().getFailureCount());
    }
    @Test void exactClassAndInitiatingThreadScopeDoNotPreventOtherDispatch() throws Exception {
        var bus=bus(); var calls=new AtomicInteger(); bus.addListener(Sample.class,e->calls.incrementAndGet());
        start(Sample.class,bus); bus.post(new Child());
        var thread=new Thread(()->bus.post(new Sample())); thread.start(); thread.join(); bus.post(new Sample());
        assertEquals(3,calls.get()); assertEquals(1,EventProfiler.snapshot().eventFires());
        assertEquals(1,EventProfiler.snapshot().handlers().getFirst().getFireCount());
        EventProfiler.finishProfiling(); start(Child.class,bus); bus.post(new Child());
        assertEquals(1,EventProfiler.snapshot().handlers().getFirst().getFireCount());
    }
    @Test void emptyShutdownAndPerPhasePostsFollowNativeBusRules() {
        var bus=ProfilingEventBus.create(BusBuilder.builder().startShutdown().allowPerPhasePost());
        start(Empty.class,bus); bus.post(new Empty()); assertEquals(0,EventProfiler.snapshot().eventFires());
        bus.start(); bus.post(new Empty()); bus.post(EventPriority.LOW,new Empty());
        assertEquals(2,EventProfiler.snapshot().eventFires()); assertTrue(EventProfiler.snapshot().handlers().isEmpty());
        EventProfiler.finishProfiling(); var ordinary=bus(); start(Empty.class,ordinary);
        assertThrows(IllegalStateException.class,()->ordinary.post(EventPriority.LOW,new Empty()));
        assertEquals(0,EventProfiler.snapshot().eventFires());
    }
    @Test void invalidStartKeepsPreviousReportAndAmbiguousAliasesDoNotChooseArbitrarily() {
        var bus=bus(); start(Empty.class,bus); bus.post(new Empty()); EventProfiler.finishProfiling();
        assertEquals(EventProfiler.ProfileStartStatus.AMBIGUOUS,EventProfiler.startProfiling("Pre",bus));
        assertEquals(EventProfiler.ProfileStartStatus.NOTANEVENT,EventProfiler.startProfiling("java.lang.String",bus));
        assertEquals(EventProfiler.ProfileStartStatus.NOSUCHCLASS,EventProfiler.startProfiling("missing.Event",bus));
        assertEquals(EventProfiler.ProfileStartStatus.NOTPOSTABLE,EventProfiler.startProfiling(AbstractSample.class,bus));
        assertEquals(EventProfiler.ProfileStartStatus.UNAVAILABLE,EventProfiler.startProfiling(Empty.class,BusBuilder.builder().build()));
        assertEquals(1,EventProfiler.snapshot().eventFires());
    }
}
