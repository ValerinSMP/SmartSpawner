package github.nighter.smartspawner.production;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Single bounded-frequency worker; never performs JDBC on a server/region thread. */
public final class ProductionObserver implements AutoCloseable {
    @FunctionalInterface
    public interface Connections { Connection open() throws SQLException; }

    public record Status(ProductionObservation.Snapshot snapshot, boolean failed) {}

    private final ScheduledExecutorService worker;
    private volatile Status status = new Status(null, false);
    private volatile boolean closed;

    public ProductionObserver(Connections connections, String table, long intervalSeconds, Logger logger) {
        worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SmartSpawner-production-observation");
            thread.setDaemon(true);
            return thread;
        });
        worker.scheduleWithFixedDelay(() -> {
            if (closed) return;
            try (Connection connection = connections.open()) {
                ProductionObservation.Snapshot snapshot = ProductionObservation.read(connection, table);
                if (!closed) status = new Status(snapshot, false);
            } catch (Exception failure) {
                if (!closed) {
                    // Keep last successful observation, but explicitly mark it stale.
                    if (!status.failed()) logger.warning("Production observation unavailable; stock is unchanged. "
                            + "Check storage connectivity and observation query compatibility.");
                    status = new Status(status.snapshot(), true);
                }
            }
        }, 0, Math.max(60, intervalSeconds), TimeUnit.SECONDS);
    }

    public Status status() { return status; }

    @Override
    public void close() {
        closed = true;
        worker.shutdownNow();
    }
}
