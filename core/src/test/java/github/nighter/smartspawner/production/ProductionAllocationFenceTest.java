package github.nighter.smartspawner.production;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ProductionAllocationFenceTest {
    @Test void newAllocationWaitsForOldApplyAndStaleBatchCannotAcquire() throws Exception {
        var fence = new ProductionAllocationFence();
        var epoch = new AtomicInteger(1);
        var started = new CountDownLatch(1);
        var worker = Executors.newSingleThreadExecutor();
        try {
            var oldLease = fence.acquire(() -> epoch.get() == 1);
            assertNotNull(oldLease);
            var publication = worker.submit(() -> {
                started.countDown();
                fence.publish(() -> epoch.set(2));
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            try { assertThrows(TimeoutException.class, () -> publication.get(50, TimeUnit.MILLISECONDS)); }
            finally { oldLease.close(); }
            publication.get(2, TimeUnit.SECONDS);
            assertNull(fence.acquire(() -> epoch.get() == 1));
            try (var current = fence.acquire(() -> epoch.get() == 2)) { assertNotNull(current); }
        } finally { worker.shutdownNow(); }
    }
    @Test void regionNeverWaitsForPublisherAndInvalidPredicateReleasesLock() throws Exception {
        var fence = new ProductionAllocationFence();
        assertNull(fence.acquire(() -> false));
        var entered = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        var worker = Executors.newSingleThreadExecutor();
        try {
            var publication = worker.submit(() -> fence.publish(() -> {
                entered.countDown();
                try { finish.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertNull(fence.acquire(() -> true));
            finish.countDown();
            publication.get(2, TimeUnit.SECONDS);
            try (var current = fence.acquire(() -> true)) { assertNotNull(current); }
        } finally { finish.countDown(); worker.shutdownNow(); }
    }
}
