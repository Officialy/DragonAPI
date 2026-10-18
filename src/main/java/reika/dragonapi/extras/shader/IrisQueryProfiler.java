package reika.dragonapi.extras.shader;

import java.util.LinkedHashMap;
import java.util.Map;

/** Opt-in query measurements. Normal play performs no clock reads, collections or frame caching. */
public final class IrisQueryProfiler {
    private static final boolean ENABLED = Boolean.getBoolean("dragonapi.profileIris");
    private static volatile Session session;
    private static long frame;
    private static String stage = "FRAME";
    private static boolean probe;

    private IrisQueryProfiler() {}

    public static long start() { return ENABLED && session != null ? System.nanoTime() : 0; }

    public static void record(long start, String source, boolean shadow, boolean value) {
        if (start == 0) return;
        long elapsed = System.nanoTime() - start;
        Session current = session;
        if (current == null || current.owner != Thread.currentThread()) return;
        String key = source + "/" + (shadow ? "shadow" : "enabled") + "/" + stage + "/" + (probe ? "probe" : "consumer");
        current.rows.computeIfAbsent(key, ignored -> new Stats()).add(elapsed, value);
        current.frames.merge(frame, elapsed, Long::sum);
        if (!probe) current.consumerFrames.merge(frame, elapsed, Long::sum);
    }

    public static void begin() {
        if (!ENABLED) throw new IllegalStateException("Enable dragonapi.profileIris before collecting query measurements");
        if (session != null) throw new IllegalStateException("Iris query measurement already active");
        session = new Session(Thread.currentThread());
    }

    public static void frame(long id) { frame = id; stage = "FRAME"; }
    public static String stage(String name) { String previous = stage; stage = name; return previous; }
    public static void probe(boolean value) { probe = value; }

    public static Map<String, Object> finish() {
        Session current = session;
        if (current == null || current.owner != Thread.currentThread()) throw new IllegalStateException("No owned Iris measurement");
        session = null; probe = false;
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> rows = new LinkedHashMap<>();
        current.rows.forEach((key, value) -> rows.put(key, Map.of("calls", value.calls, "true", value.truths,
                "totalNanos", value.total, "maxNanos", value.maximum)));
        result.put("queries", rows);
        result.put("framesWithQueries", current.frames.size());
        result.put("maxQueryNanosPerFrame", current.frames.values().stream().mapToLong(Long::longValue).max().orElse(0));
        result.put("totalQueryNanos", current.frames.values().stream().mapToLong(Long::longValue).sum());
        result.put("consumerFrames", current.consumerFrames.size());
        result.put("maxConsumerQueryNanosPerFrame", current.consumerFrames.values().stream().mapToLong(Long::longValue).max().orElse(0));
        result.put("totalConsumerQueryNanos", current.consumerFrames.values().stream().mapToLong(Long::longValue).sum());
        long[] costs = current.consumerFrames.values().stream().mapToLong(Long::longValue).sorted().toArray();
        if (costs.length != 0) {
            result.put("medianConsumerQueryNanosPerFrame", costs[(costs.length - 1) / 2]);
            result.put("p95ConsumerQueryNanosPerFrame", costs[(int)Math.ceil(costs.length * 0.95) - 1]);
            result.put("p99ConsumerQueryNanosPerFrame", costs[(int)Math.ceil(costs.length * 0.99) - 1]);
        }
        return result;
    }

    private static final class Session {
        final Thread owner;
        final Map<String, Stats> rows = new LinkedHashMap<>();
        final Map<Long, Long> frames = new LinkedHashMap<>();
        final Map<Long, Long> consumerFrames = new LinkedHashMap<>();
        Session(Thread owner) { this.owner = owner; }
    }

    private static final class Stats {
        long calls, truths, total, maximum;
        void add(long nanos, boolean value) { calls++; if (value) truths++; total += nanos; maximum = Math.max(maximum, nanos); }
    }
}
