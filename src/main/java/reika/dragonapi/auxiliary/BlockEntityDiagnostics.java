package reika.dragonapi.auxiliary;

import java.util.function.Consumer;

/** Optional instrumentation installed by a caller; no dependency on RotaryCraft or reflective hot paths. */
public final class BlockEntityDiagnostics {
    private static volatile Consumer<String> sink;
    private BlockEntityDiagnostics() {}
    public static void setSink(Consumer<String> consumer) { sink = consumer; }
    public static boolean enabled() { return sink != null; }
    public static void event(String name) {
        Consumer<String> current = sink;
        if (current != null) current.accept(name);
    }
}
