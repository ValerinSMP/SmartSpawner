package github.nighter.smartspawner.production;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;

/** Nonblocking region-side lease; worker publication waits for old applies to finish. No I/O under it. */
public final class ProductionAllocationFence {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public Lease acquire(BooleanSupplier valid) {
        if (!lock.readLock().tryLock()) return null;
        boolean accepted = false;
        try {
            if (!valid.getAsBoolean()) return null;
            accepted = true;
            return new Lease(lock.readLock()::unlock);
        } finally {
            if (!accepted) lock.readLock().unlock();
        }
    }
    public void publish(Runnable publication) {
        lock.writeLock().lock();
        try { publication.run(); }
        finally { lock.writeLock().unlock(); }
    }
    public static final class Lease implements AutoCloseable {
        private final Runnable release;
        private Lease(Runnable release) { this.release = release; }
        @Override public void close() { release.run(); }
    }
}
