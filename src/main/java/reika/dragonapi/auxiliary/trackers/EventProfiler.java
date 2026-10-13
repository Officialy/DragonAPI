package reika.dragonapi.auxiliary.trackers;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.DragonAPI;

/** Exact-event, initiating-thread listener profiling of the real NeoForge game bus. */
public final class EventProfiler {
    private EventProfiler() {}
    private static final Map<String, String> SHORTCUTS = new HashMap<>();
    private static final java.util.Set<String> AMBIGUOUS = new java.util.HashSet<>();
    private static volatile Session active;
    private static volatile Session last;
    private static final ThreadLocal<Dispatch> DISPATCH = new ThreadLocal<>();
    private static final ThreadLocal<ArrayDeque<ManualSpan>> MANUAL = ThreadLocal.withInitial(ArrayDeque::new);
    static {
        // String names keep client event classes out of common/dedicated-server verification.
        shortcut("net.neoforged.neoforge.event.entity.EntityJoinLevelEvent");
        shortcut("net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre");
        shortcut("net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent$Pre");
        shortcut("net.neoforged.neoforge.event.entity.player.ItemTooltipEvent");
        shortcut("net.neoforged.neoforge.event.tick.EntityTickEvent$Pre");
        shortcut("net.neoforged.neoforge.client.event.RenderGuiLayerEvent$Post");
        shortcut("net.neoforged.neoforge.client.event.RenderLevelStageEvent$AfterLevel");
        for (String type : List.of("SetBlockEvent", "BlockTickEvent", "EntityAboutToRayTraceEvent", "ItemUpdateEvent"))
            shortcut("reika.dragonapi.instantiable.event." + type);
    }
    private static void shortcut(String name) {
        String tail = name.substring(name.lastIndexOf('.') + 1);
        alias(tail.replace('$', '.'), name);
        alias(tail.substring(tail.lastIndexOf('$') + 1), name);
    }
    private static void alias(String key, String name) {
        if (AMBIGUOUS.contains(key)) return;
        String previous = SHORTCUTS.putIfAbsent(key, name);
        if (previous != null && !previous.equals(name)) { SHORTCUTS.remove(key); AMBIGUOUS.add(key); }
    }
    public static synchronized void addShortcut(Class<? extends Event> type) { shortcut(type.getName()); }

    public static void startProfiling(Class<? extends Event> type) {
        ProfileStartStatus status = startProfiling(type, NeoForge.EVENT_BUS);
        if (status != ProfileStartStatus.SUCCESS) DragonAPI.LOGGER.error("Cannot start event profiling: {}", status);
    }
    public static ProfileStartStatus startProfiling(String type) { return startProfiling(type, NeoForge.EVENT_BUS); }
    public static synchronized ProfileStartStatus startProfiling(String name, IEventBus bus) {
        if (active != null) return ProfileStartStatus.ALREADYRUNNING;
        if (AMBIGUOUS.contains(name)) return ProfileStartStatus.AMBIGUOUS;
        try {
            Class<?> type = Class.forName(SHORTCUTS.getOrDefault(name, name), false, EventProfiler.class.getClassLoader());
            if (!Event.class.isAssignableFrom(type)) return ProfileStartStatus.NOTANEVENT;
            return startProfiling(type.asSubclass(Event.class), bus);
        } catch (ClassNotFoundException error) { return ProfileStartStatus.NOSUCHCLASS; }
        catch (LinkageError error) { return ProfileStartStatus.UNAVAILABLE; }
    }
    /** Also supports explicitly constructed instrumented buses for integrations and native fixtures. */
    public static synchronized ProfileStartStatus startProfiling(Class<? extends Event> type, IEventBus bus) {
        Objects.requireNonNull(type);
        if (active != null) return ProfileStartStatus.ALREADYRUNNING;
        if (Modifier.isAbstract(type.getModifiers())) return ProfileStartStatus.NOTPOSTABLE;
        if (!(bus instanceof ProfilingEventBus instrumented)) return ProfileStartStatus.UNAVAILABLE;
        Session next = new Session(type, instrumented, Thread.currentThread());
        try { instrumented.prepare(next); }
        catch (IllegalArgumentException error) { return ProfileStartStatus.NOTPOSTABLE; }
        catch (ReflectiveOperationException | RuntimeException error) {
            DragonAPI.LOGGER.error("Event-bus profiling instrumentation is unavailable", error);
            return ProfileStartStatus.UNAVAILABLE;
        }
        last = next;
        active = next;
        return ProfileStartStatus.SUCCESS;
    }
    public static synchronized void finishProfiling() {
        Session session = active;
        if (session == null) return;
        active = null;
        session.bus.restore();
    }
    static Session active(ProfilingEventBus bus) { Session s = active; return s != null && s.bus == bus ? s : null; }
    static boolean isActive() { return active != null; }
    static Dispatch begin(ProfilingEventBus bus, Event event) {
        Dispatch previous = DISPATCH.get();
        Session selected = active(bus);
        if (selected != null && (event.getClass() != selected.type || Thread.currentThread() != selected.thread)) selected = null;
        Dispatch frame = new Dispatch(previous, selected, event);
        DISPATCH.set(frame);
        return frame;
    }
    static void end(Dispatch frame, boolean returned, boolean started) {
        if (frame.session != null && (frame.enteredListener || returned && started)) frame.session.fired();
        if (frame.previous == null) DISPATCH.remove(); else DISPATCH.set(frame.previous);
    }
    static Dispatch dispatch(Event event) {
        Dispatch frame = DISPATCH.get();
        return frame != null && frame.session != null && frame.event == event ? frame : null;
    }
    static final class Dispatch {
        final Dispatch previous; final Session session; final Event event; boolean enteredListener;
        Dispatch(Dispatch previous, Session session, Event event) { this.previous = previous; this.session = session; this.event = event; }
    }
    static final class Session {
        final Class<? extends Event> type; final ProfilingEventBus bus; final Thread thread;
        private final IdentityHashMap<Object, Measurement> measurements = new IdentityHashMap<>();
        private long fires;
        Session(Class<? extends Event> type, ProfilingEventBus bus, Thread thread) { this.type = type; this.bus = bus; this.thread = thread; }
        synchronized void fired() { fires++; }
        synchronized void record(Object key, String identifier, Class<?> identifyingClass, long nanos, boolean failed) {
            Measurement entry = measurements.computeIfAbsent(key, ignored -> new Measurement(identifier, identifyingClass));
            entry.time += nanos; entry.calls++; if (failed) entry.failures++;
        }
        synchronized ProfileSnapshot snapshot() {
            ArrayList<EventProfile> entries = new ArrayList<>();
            for (Measurement m : measurements.values()) entries.add(new EventProfile(m.identifier, m.type, m.time, m.calls, m.failures));
            entries.sort(null);
            return new ProfileSnapshot(type.getName(), "game/event bus, thread '" + thread.getName() + "'", fires,
                    entries.stream().mapToLong(EventProfile::getTotalTime).sum(), List.copyOf(entries), active == this);
        }
    }
    private static final class Measurement {
        final String identifier; final Class<?> type; long time, calls, failures;
        Measurement(String identifier, Class<?> type) { this.identifier = identifier; this.type = type; }
    }
    public record ProfileSnapshot(String eventType, String scope, long eventFires, long totalHandlerTime,
            List<EventProfile> handlers, boolean running) {}
    public static ProfileSnapshot snapshot() {
        Session s = last;
        return s == null ? new ProfileSnapshot("[none]", "[none]", 0, 0, List.of(), false) : s.snapshot();
    }
    public static ArrayList<EventProfile> getProfilingData() { return new ArrayList<>(snapshot().handlers()); }
    public static long getTotalProfilingTime() { return snapshot().totalHandlerTime(); }
    public static String getProfiledEventType() { return snapshot().eventType(); }
    public static int getEventFireCount() { return (int)Math.min(Integer.MAX_VALUE, snapshot().eventFires()); }

    /** Legacy explicit hooks remain usable; manual spans do not invent bus-post counts. */
    public static void firePre(Event event, Object listener) {
        Session s = active;
        if (s != null && event.getClass() == s.type && Thread.currentThread() == s.thread)
            MANUAL.get().push(new ManualSpan(s, event, listener, System.nanoTime()));
    }
    public static void firePost(Event event, Object listener) {
        ArrayDeque<ManualSpan> spans = MANUAL.get();
        if (spans.isEmpty()) { MANUAL.remove(); return; }
        ManualSpan span = spans.peek();
        if (span.event != event || span.listener != listener) throw new IllegalStateException("Unbalanced event profiling hooks");
        spans.pop(); if (spans.isEmpty()) MANUAL.remove();
        span.session.record(listener, describe(listener), listener.getClass(), System.nanoTime() - span.start, false);
    }
    private record ManualSpan(Session session, Event event, Object listener, long start) {}
    static String describe(Object listener) {
        try { return listener.toString(); }
        catch (RuntimeException error) { return listener.getClass().getName(); }
    }
    public enum ProfileStartStatus { SUCCESS, ALREADYRUNNING, NOSUCHCLASS, NOTANEVENT, AMBIGUOUS, NOTPOSTABLE, UNAVAILABLE }
    /** Immutable report row; totals/averages count real body calls, including bodies that throw. */
    public static final class EventProfile implements Comparable<EventProfile> {
        public final String identifier; public final Class<?> identifyingClass;
        private final long totalTime, calls, failures;
        private EventProfile(String id, Class<?> type, long totalTime, long calls, long failures) {
            identifier = id; identifyingClass = type; this.totalTime = totalTime; this.calls = calls; this.failures = failures;
        }
        public long getTotalTime() { return totalTime; }
        public long getAverageTime() { return calls == 0 ? 0 : totalTime / calls; }
        public long getFireCount() { return calls; }
        public long getFailureCount() { return failures; }
        @Override public int compareTo(EventProfile other) {
            int time = Long.compare(other.totalTime, totalTime);
            return time != 0 ? time : identifier.compareTo(other.identifier);
        }
    }
}
