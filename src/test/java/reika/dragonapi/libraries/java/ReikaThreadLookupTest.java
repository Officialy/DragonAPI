package reika.dragonapi.libraries.java;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaThreadLookupTest {
    private static class RunningThread implements AutoCloseable {
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<ThreadGroup> root = new AtomicReference<>();
        final Thread thread;

        RunningThread(String name, ThreadGroup group, boolean virtual) throws InterruptedException {
            var ready = new CountDownLatch(1);
            Runnable task = () -> {
                root.set(ReikaJavaLibrary.getTopLevelThreadGroup());
                ready.countDown();
                try {
                    release.await();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
            };
            thread = virtual ? Thread.ofVirtual().name(name).unstarted(task)
                    : Thread.ofPlatform().group(group).daemon(true).name(name).unstarted(task);
            thread.start();
            if (!ready.await(5, TimeUnit.SECONDS)) {
                close();
                fail("Diagnostic thread did not become ready");
            }
        }

        @Override public void close() throws InterruptedException {
            release.countDown();
            thread.join(5_000);
            assertFalse(thread.isAlive(), "Test thread must stop");
        }
    }

    @Test void lookupFindsNestedLivePlatformThreadsAndDoesNotRetainTerminatedThreads() throws Exception {
        String name = "dragonapi-lookup-" + UUID.randomUUID();
        var group = new ThreadGroup(new ThreadGroup(name + "-parent"), name + "-child");
        ThreadGroup expectedRoot = ReikaJavaLibrary.getTopLevelThreadGroup();
        assertNull(expectedRoot.getParent());
        try (var running = new RunningThread(name, group, false)) {
            assertSame(expectedRoot, running.root.get());
            assertSame(running.thread, ReikaJavaLibrary.getThreadByName(name));
        }
        assertNull(ReikaJavaLibrary.getThreadByName(name));
    }

    @Test void duplicateNamesReturnALiveMatchAndAbsentOrNullNamesReturnNull() throws Exception {
        String name = "dragonapi-duplicates-" + UUID.randomUUID();
        try (var first = new RunningThread(name, new ThreadGroup(name + "-first"), false);
             var second = new RunningThread(name, new ThreadGroup(name + "-second"), false)) {
            Thread match = ReikaJavaLibrary.getThreadByName(name);
            assertNotNull(match);
            assertTrue(Set.of(first.thread, second.thread).contains(match));
            assertTrue(match.isAlive());
            assertNull(ReikaJavaLibrary.getThreadByName(name + "-missing"));
            assertNull(ReikaJavaLibrary.getThreadByName(null));
        }
    }

    @Test void virtualThreadsShareTheRootButAreOutsidePlatformLookup() throws Exception {
        String name = "dragonapi-virtual-" + UUID.randomUUID();
        try (var running = new RunningThread(name, null, true)) {
            assertTrue(running.thread.isVirtual());
            assertSame(ReikaJavaLibrary.getTopLevelThreadGroup(), running.root.get());
            assertNull(ReikaJavaLibrary.getThreadByName(name));
        }
    }
}
