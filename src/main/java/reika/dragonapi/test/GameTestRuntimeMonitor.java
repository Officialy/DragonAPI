package reika.dragonapi.test;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.server.level.ServerPlayer;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.mixin.MultipleTestTrackerAccessor;

/**
 * Wall-clock diagnostics and fail-safe for the headless GameTest server.
 *
 * <p>A GameTestServer intentionally fast-forwards instead of targeting vanilla's 20 TPS, so a
 * conventional TPS monitor reports misleading data. This monitor reports effective executed
 * ticks/second, wall-clock ms/tick, heap use, suite progress, and time since the server thread last
 * completed a tick. Its daemon thread remains responsive when the server thread is blocked.</p>
 */
public final class GameTestRuntimeMonitor {

    private static final int HEARTBEAT_SECONDS = positiveProperty("dragonapi.gametest.heartbeatSeconds", 10);
    private static final int STACK_DUMP_SECONDS = positiveProperty("dragonapi.gametest.stackDumpSeconds", 30);
    private static final int HANG_TIMEOUT_SECONDS = nonNegativeProperty("dragonapi.gametest.hangTimeoutSeconds", 180);

    private static volatile GameTestServer server;
    private static volatile boolean running;
    private static volatile long lastCompletedTickNanos;
    private static volatile int completedTicks;
    private static volatile String phase = "initializing";
    private static volatile String progress = "0/0";
    private static volatile String activeTests = "none";
    private static volatile int syntheticPlayers;

    private GameTestRuntimeMonitor() {
    }

    public static synchronized void start(GameTestServer testServer, int totalTests, int batches) {
        if (running)
            return;
        server = testServer;
        running = true;
        lastCompletedTickNanos = System.nanoTime();
        completedTicks = testServer.getTickCount();
        progress = "0/" + totalTests;
        phase = "starting " + batches + " batch" + (batches == 1 ? "" : "es");

        Thread monitor = new Thread(GameTestRuntimeMonitor::monitorLoop, "DragonAPI-GameTest-Watchdog");
        monitor.setDaemon(true);
        monitor.start();
        DragonAPI.LOGGER.info(
                "GameTest watchdog active: heartbeat={}s, no-completed-tick timeout={}s, heap ceiling={} MiB",
                HEARTBEAT_SECONDS, HANG_TIMEOUT_SECONDS == 0 ? "disabled" : HANG_TIMEOUT_SECONDS,
                Runtime.getRuntime().maxMemory() / 1048576L);
    }

    public static void tickStarted(GameTestServer testServer, MultipleTestTracker tracker) {
        server = testServer;
        completedTicks = testServer.getTickCount();
        captureProgress(tracker);
    }

    public static void tickCompleted(GameTestServer testServer, MultipleTestTracker tracker) {
        server = testServer;
        completedTicks = testServer.getTickCount();
        lastCompletedTickNanos = System.nanoTime();
        syntheticPlayers = countSyntheticPlayers(testServer);
        captureProgress(tracker);
    }

    public static void preparingBatch(int batchIndex, int batchCount, int testCount, int playersRemoved) {
        phase = batchIndex >= batchCount
                ? "finalizing suite"
                : "preparing batch " + (batchIndex + 1) + "/" + batchCount + " (" + testCount + " tests)";
        syntheticPlayers = 0;
        if (playersRemoved > 0)
            DragonAPI.LOGGER.info("Released {} synthetic GameTest player(s) before {}", playersRemoved, phase);
    }

    public static void runningBatch(int batchIndex, int batchCount) {
        phase = batchIndex >= batchCount
                ? "finalizing suite"
                : "running batch " + (batchIndex + 1) + "/" + batchCount;
    }

    public static synchronized void stop() {
        running = false;
        server = null;
    }

    public static boolean isSyntheticPlayer(ServerPlayer player) {
        return player.getGameProfile().name().startsWith("test-mock-player");
    }

    private static void captureProgress(MultipleTestTracker tracker) {
        if (tracker == null) {
            activeTests = "not started";
            return;
        }
        progress = tracker.getDoneCount() + "/" + tracker.getTotalCount()
                + (tracker.getFailedRequiredCount() > 0 ? ", failed=" + tracker.getFailedRequiredCount() : "");
        Collection<GameTestInfo> tests = ((MultipleTestTrackerAccessor)tracker).dragonapi$getTests();
        String active = tests.stream()
                .filter(test -> test.hasStarted() && !test.isDone())
                .limit(4)
                .map(test -> test.id() + "@" + test.getTick() + "/" + test.getTimeoutTicks())
                .collect(Collectors.joining(", "));
        long extra = tests.stream().filter(test -> test.hasStarted() && !test.isDone()).count() - 4;
        activeTests = active.isEmpty() ? "none" : active + (extra > 0 ? " (and " + extra + " more)" : "");
    }

    private static int countSyntheticPlayers(GameTestServer testServer) {
        return (int)testServer.getPlayerList().getPlayers().stream().filter(GameTestRuntimeMonitor::isSyntheticPlayer).count();
    }

    private static void monitorLoop() {
        long lastSampleNanos = System.nanoTime();
        long lastStackDumpNanos = 0;
        int lastSampleTick = completedTicks;

        while (running) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(HEARTBEAT_SECONDS));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running)
                return;

            GameTestServer currentServer = server;
            if (currentServer == null)
                continue;
            long now = System.nanoTime();
            int tick = completedTicks;
            int tickDelta = tick - lastSampleTick;
            double elapsedSeconds = (now - lastSampleNanos) / 1.0E9;
            double effectiveTps = tickDelta / elapsedSeconds;
            String mspt = tickDelta > 0
                    ? String.format(Locale.ROOT, "%.2f", elapsedSeconds * 1000D / tickDelta)
                    : "stalled";
            double stallSeconds = (now - lastCompletedTickNanos) / 1.0E9;
            Runtime runtime = Runtime.getRuntime();
            long usedMiB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
            long committedMiB = runtime.totalMemory() / 1048576L;
            long maxMiB = runtime.maxMemory() / 1048576L;

            DragonAPI.LOGGER.info(
                    "GameTest heartbeat: phase={}, progress={}, effectiveTPS={}, wallMSPT={}, "
                            + "lastCompletedTick={}s, heap={}/{}/{} MiB (used/committed/max), mockPlayers={}, active=[{}]",
                    phase, progress, String.format(Locale.ROOT, "%.1f", effectiveTps), mspt,
                    String.format(Locale.ROOT, "%.1f", stallSeconds), usedMiB, committedMiB, maxMiB,
                    syntheticPlayers, activeTests);

            if (stallSeconds >= STACK_DUMP_SECONDS
                    && now - lastStackDumpNanos >= TimeUnit.SECONDS.toNanos(STACK_DUMP_SECONDS)) {
                lastStackDumpNanos = now;
                Thread serverThread = currentServer.getRunningThread();
                String trace = java.util.Arrays.stream(serverThread.getStackTrace())
                        .map(element -> "\n    at " + element)
                        .collect(Collectors.joining());
                DragonAPI.LOGGER.warn("GameTest server thread has not completed a tick for {}s; current stack:{}",
                        String.format(Locale.ROOT, "%.1f", stallSeconds), trace);
            }

            if (HANG_TIMEOUT_SECONDS > 0 && stallSeconds >= HANG_TIMEOUT_SECONDS) {
                DragonAPI.LOGGER.error(
                        "GameTest server exceeded the {}s no-progress timeout during {}. Requesting shutdown; "
                                + "the watchdog will terminate this GameTest JVM if the server thread remains wedged.",
                        HANG_TIMEOUT_SECONDS, phase);
                currentServer.halt(false);
                for (int i = 0; i < 10 && running; i++) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (running) {
                    DragonAPI.LOGGER.error("GameTest server did not shut down after the watchdog grace period; terminating test JVM.");
                    Runtime.getRuntime().halt(3);
                }
                return;
            }

            lastSampleNanos = now;
            lastSampleTick = tick;
        }
    }

    private static int positiveProperty(String name, int fallback) {
        return Math.max(1, Integer.getInteger(name, fallback));
    }

    private static int nonNegativeProperty(String name, int fallback) {
        return Math.max(0, Integer.getInteger(name, fallback));
    }
}
