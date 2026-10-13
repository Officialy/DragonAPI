package reika.dragonapi.auxiliary.trackers;

import java.util.function.Consumer;
import net.neoforged.bus.BusBuilderImpl;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;

/** Native bus subclass: native registration, dispatch, filtering and exception handling stay authoritative. */
public final class ProfilingEventBus extends EventBus {
    private NativeEventBusAccess access;
    private int registrationDepth;
    public ProfilingEventBus(BusBuilderImpl builder) { super(builder); }
    public static ProfilingEventBus create(BusBuilder builder) {
        if (!(builder instanceof BusBuilderImpl nativeBuilder)) throw new IllegalArgumentException("Expected NeoForge bus builder");
        return new ProfilingEventBus(nativeBuilder);
    }
    synchronized void prepare(EventProfiler.Session session) throws ReflectiveOperationException {
        if (access == null) access = new NativeEventBusAccess(this);
        access.validate(session.type);
        try { access.instrument(session.type); }
        catch (ReflectiveOperationException | RuntimeException error) { access.restore(); throw error; }
    }
    synchronized void restore() {
        try { if (access != null) access.restore(); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot restore native event listeners", error); }
    }
    private synchronized void registration(Runnable operation) {
        registrationDepth++;
        try { operation.run(); }
        finally {
            if (--registrationDepth == 0) {
                var session = EventProfiler.active(this);
                if (session != null) {
                    try { access.instrument(session.type); }
                    catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot instrument new native listeners", error); }
                }
            }
        }
    }
    @Override public void register(Object target) { registration(() -> super.register(target)); }
    @Override public void unregister(Object target) { registration(() -> super.unregister(target)); }
    @Override public <T extends Event> void addListener(Consumer<T> c) { registration(() -> super.addListener(c)); }
    @Override public <T extends Event> void addListener(EventPriority p, Consumer<T> c) { registration(() -> super.addListener(p,c)); }
    @Override public <T extends Event> void addListener(EventPriority p, boolean r, Consumer<T> c) { registration(() -> super.addListener(p,r,c)); }
    @Override public <T extends Event> void addListener(EventPriority p, Class<T> t, Consumer<T> c) { registration(() -> super.addListener(p,t,c)); }
    @Override public <T extends Event> void addListener(boolean r, Consumer<T> c) { registration(() -> super.addListener(r,c)); }
    @Override public <T extends Event> void addListener(boolean r, Class<T> t, Consumer<T> c) { registration(() -> super.addListener(r,t,c)); }
    @Override public <T extends Event> void addListener(Class<T> t, Consumer<T> c) { registration(() -> super.addListener(t,c)); }
    @Override public <T extends Event> void addListener(EventPriority p, boolean r, Class<T> t, Consumer<T> c) { registration(() -> super.addListener(p,r,t,c)); }

    @Override public <T extends Event> T post(T event) {
        if (!EventProfiler.isActive()) return super.post(event);
        var frame = EventProfiler.begin(this,event);
        boolean returned = false;
        boolean started = access != null && access.started();
        try { T result = super.post(event); returned = true; return result; }
        finally { EventProfiler.end(frame,returned,started); }
    }
    @Override public <T extends Event> T post(EventPriority priority,T event) {
        if (!EventProfiler.isActive()) return super.post(priority,event);
        var frame = EventProfiler.begin(this,event);
        boolean returned = false;
        boolean started = access != null && access.started();
        try { T result = super.post(priority,event); returned = true; return result; }
        finally { EventProfiler.end(frame,returned,started); }
    }
}
