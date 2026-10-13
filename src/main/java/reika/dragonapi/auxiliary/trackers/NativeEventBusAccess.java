package reika.dragonapi.auxiliary.trackers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import net.neoforged.bus.ConsumerEventHandler;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.GeneratedEventListener;
import net.neoforged.bus.ListenerList;
import net.neoforged.bus.SubscribeEventListener;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventListener;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventClassChecker;
import net.neoforged.bus.api.SubscribeEvent;

/** Isolated bus-8.0.5 adapter; listener reflection is limited to enable/disable and registration. */
final class NativeEventBusAccess {
    private final EventBus bus;
    private final Field targets, checker, shutdown, parent, priorities, writeLock, predicate, annotation;
    private final Method listenerList, rebuild;
    private final IdentityHashMap<EventListener, TimedListener> wrapped = new IdentityHashMap<>();
    private final List<ListenerList> touched = new ArrayList<>();
    NativeEventBusAccess(EventBus bus) throws ReflectiveOperationException {
        this.bus = bus;
        targets = field(EventBus.class,"listeners"); checker = field(EventBus.class,"classChecker"); shutdown = field(EventBus.class,"shutdown");
        parent = field(ListenerList.class,"parent"); priorities = field(ListenerList.class,"priorities"); writeLock = field(ListenerList.class,"writeLock");
        predicate = field(ConsumerEventHandler.WithPredicate.class,"predicate"); annotation = field(SubscribeEventListener.class,"subInfo");
        listenerList = EventBus.class.getDeclaredMethod("getListenerList",Class.class); listenerList.setAccessible(true);
        rebuild = ListenerList.class.getDeclaredMethod("forceRebuild"); rebuild.setAccessible(true);
    }
    private static Field field(Class<?> owner,String name) throws ReflectiveOperationException {
        Field value = owner.getDeclaredField(name); value.setAccessible(true); return value;
    }
    void validate(Class<? extends Event> type) throws ReflectiveOperationException {
        ((IEventClassChecker)checker.get(bus)).check(type);
    }
    boolean started() {
        try { return !shutdown.getBoolean(bus); }
        catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    void instrument(Class<? extends Event> type) throws ReflectiveOperationException {
        for (ListenerList list = (ListenerList)listenerList.invoke(bus,type); list != null; list = (ListenerList)parent.get(list)) {
            Semaphore lock = (Semaphore)writeLock.get(list);
            boolean changed = false;
            // Track the list before mutation so a partial installation can always be restored.
            if (!touched.contains(list)) touched.add(list);
            lock.acquireUninterruptibly();
            try {
                for (List<EventListener> priority : priorityLists(list)) for (int i = 0; i < priority.size(); i++) {
                    EventListener original = priority.get(i);
                    if (original instanceof TimedListener) continue;
                    TimedListener timed = wrapped.get(original);
                    if (timed == null) { timed = new TimedListener(original,predicate,annotation); wrapped.put(original,timed); }
                    priority.set(i,timed); changed = true;
                }
            } finally { lock.release(); if (changed) rebuild.invoke(list); }
        }
        for (List<EventListener> values : targetMap().values()) synchronized (values) {
            for (int i = 0; i < values.size(); i++) {
                TimedListener replacement = wrapped.get(values.get(i));
                if (replacement != null) values.set(i,replacement);
            }
        }
    }
    void restore() throws ReflectiveOperationException {
        for (ListenerList list : touched) {
            Semaphore lock = (Semaphore)writeLock.get(list);
            lock.acquireUninterruptibly();
            try {
                for (List<EventListener> priority : priorityLists(list)) for (int i = 0; i < priority.size(); i++)
                    if (priority.get(i) instanceof TimedListener timed) priority.set(i,timed.original);
            } finally { lock.release(); }
            rebuild.invoke(list);
        }
        for (List<EventListener> values : targetMap().values()) synchronized (values) {
            for (int i = 0; i < values.size(); i++)
                if (values.get(i) instanceof TimedListener timed) values.set(i,timed.original);
        }
        touched.clear(); wrapped.clear();
    }
    @SuppressWarnings("unchecked") private List<List<EventListener>> priorityLists(ListenerList list) throws IllegalAccessException {
        return (List<List<EventListener>>)priorities.get(list);
    }
    @SuppressWarnings("unchecked") private Map<Object,List<EventListener>> targetMap() throws IllegalAccessException {
        return (Map<Object,List<EventListener>>)targets.get(bus);
    }
    /** GeneratedEventListener is the native non-sealed extension point permitted by EventListener. */
    private static final class TimedListener extends GeneratedEventListener {
        final EventListener original, withoutCheck;
        final Predicate<Event> cancellationFilter;
        private volatile String identifier;
        @SuppressWarnings("unchecked") TimedListener(EventListener original,Field predicate,Field annotation) throws IllegalAccessException {
            this.original = original;
            if (original instanceof ConsumerEventHandler.WithPredicate consumer) {
                withoutCheck = consumer.getWithoutCheck(); cancellationFilter = (Predicate<Event>)predicate.get(consumer);
            } else if (original instanceof SubscribeEventListener subscriber) {
                withoutCheck = subscriber.getWithoutCheck();
                boolean receiveCanceled = ((SubscribeEvent)annotation.get(subscriber)).receiveCanceled();
                cancellationFilter = receiveCanceled ? null : event -> !((ICancellableEvent)event).isCanceled();
            } else { withoutCheck = original; cancellationFilter = null; }
        }
        @Override public void invoke(Event event) {
            var frame = EventProfiler.dispatch(event);
            if (frame != null) frame.enteredListener = true;
            // Match native unwrapping for non-cancellable events; skipped bodies are never timed.
            if (event instanceof ICancellableEvent && cancellationFilter != null && !cancellationFilter.test(event)) return;
            if (frame == null) { withoutCheck.invoke(event); return; }
            long start = System.nanoTime(); boolean failed = true;
            try { withoutCheck.invoke(event); failed = false; }
            finally {
                long elapsed = System.nanoTime()-start;
                // Listener descriptions can execute user code; never evaluate them under the list lock.
                String description = identifier;
                if (description == null) identifier = description = EventProfiler.describe(original);
                frame.session.record(original,description,original.getClass(),elapsed,failed);
            }
        }
        @Override public String toString() { return original.toString(); }
    }
}
