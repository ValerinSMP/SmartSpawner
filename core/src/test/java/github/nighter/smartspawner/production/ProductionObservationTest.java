package github.nighter.smartspawner.production;

import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class ProductionObservationTest {
    @Test void durableStockSurvivesConnectionRestart(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("stock.db");
        try (var connection = DriverManager.getConnection(url)) {
            connection.createStatement().execute("CREATE TABLE stock (entity_type TEXT, itemspawner_type TEXT, stack_size INTEGER)");
            connection.createStatement().execute("INSERT INTO stock VALUES ('BLAZE',NULL,73)");
        }
        try (var reopened = DriverManager.getConnection(url)) {
            assertEquals(73, ProductionObservation.read(reopened, "stock").units());
        }
    }

    @Test void connectionAcquisitionIsAsyncAndShutdownInterruptsObserver() throws Exception {
        var started = new java.util.concurrent.CountDownLatch(1);
        var stopped = new java.util.concurrent.CountDownLatch(1);
        var thread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var caller = Thread.currentThread();
        try (var observer = new ProductionObserver(() -> {
            thread.set(Thread.currentThread());
            started.countDown();
            try {
                new java.util.concurrent.CountDownLatch(1).await();
                throw new SQLException("unexpected wakeup");
            } catch (InterruptedException expected) {
                Thread.currentThread().interrupt();
                throw new SQLException("closed", expected);
            } finally {
                stopped.countDown();
            }
        }, "stock", 1, java.util.logging.Logger.getAnonymousLogger())) {
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertNotSame(caller, thread.get());
            assertNull(observer.status().snapshot());
            observer.close();
            assertTrue(stopped.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertNull(observer.status().snapshot());
        }
    }

    @Test void countsUnitsAcrossWorldsAndIgnoresInteractionAndVariant() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute("CREATE TABLE stock (entity_type TEXT, itemspawner_type TEXT, "
                    + "stack_size INTEGER, world TEXT, config_name TEXT, last_interacted_player TEXT, exp INTEGER)");
            connection.createStatement().execute("INSERT INTO stock VALUES "
                    + "('BLAZE',NULL,50,'loaded','aliasA','visitor',42),"
                    + "('BLAZE',NULL,100,'unloaded','aliasB','other',99),"
                    + "('ITEM','DIAMOND',7,'unloaded',NULL,NULL,17)");
            var snapshot = ProductionObservation.read(connection, "stock");
            assertEquals(3, snapshot.blocks());
            assertEquals(157, snapshot.units());
            assertEquals(150, snapshot.types().get("MOB:BLAZE").units());
            assertEquals(7, snapshot.types().get("ITEM:DIAMOND").units());
            assertThrows(UnsupportedOperationException.class, () -> snapshot.types().clear());
            var rows = connection.createStatement().executeQuery("SELECT SUM(exp) FROM stock");
            assertTrue(rows.next());
            assertEquals(158, rows.getLong(1)); // observation did not modify XP
        }
    }

    @Test void invalidLegacyStockIsVisibleAndNeverRepairedOrDeleted() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute("CREATE TABLE stock (entity_type TEXT, itemspawner_type TEXT, stack_size INTEGER)");
            connection.createStatement().execute("INSERT INTO stock VALUES ('PIG',NULL,0),('PIG',NULL,-5),('PIG',NULL,NULL),('PIG',NULL,2000000000),('PIG',NULL,2000000000)");
            var snapshot = ProductionObservation.read(connection, "stock");
            assertEquals(5, snapshot.blocks());
            assertEquals(4000000000L, snapshot.units());
            assertEquals(3, snapshot.invalidBlocks());
            assertEquals(3, snapshot.types().get("MOB:PIG").invalidBlocks());
            assertEquals(snapshot.units(), ProductionObservation.read(connection, "stock").units());
        }
    }

    @Test void emptyAndUnavailableStorageAreNotInventedStock() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute("CREATE TABLE stock (entity_type TEXT, itemspawner_type TEXT, stack_size INTEGER)");
            assertEquals(0, ProductionObservation.read(connection, "stock").units());
            assertThrows(SQLException.class, () -> ProductionObservation.read(connection, "missing"));
            assertThrows(IllegalArgumentException.class, () -> ProductionObservation.read(connection, "stock;DROP"));
        }
    }
}
