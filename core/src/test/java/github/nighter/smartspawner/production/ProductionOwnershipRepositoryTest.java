package github.nighter.smartspawner.production;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProductionOwnershipRepositoryTest {
    private static final UUID OWNER = new UUID(0, 1), FARM = new UUID(0, 2);
    private void source(Connection connection) throws Exception {
        connection.createStatement().execute("CREATE TABLE stock (spawner_id TEXT, world TEXT, loc_x INTEGER, loc_z INTEGER,"
                + "entity_type TEXT, itemspawner_type TEXT, stack_size INTEGER, exp INTEGER)");
        connection.createStatement().execute("INSERT INTO stock VALUES ('legacy','unloaded',15,16,'BLAZE',NULL,150,42)");
    }
    @Test void additiveMigrationDoesNotAttributeLegacyAndCountsUnloadedStack() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            source(connection);
            var repository = new ProductionOwnershipRepository("stock");
            repository.initialize(connection);
            repository.initialize(connection);
            var owners = repository.owners(connection);
            assertTrue(owners.isEmpty());
            var stock = repository.stock(connection, owners).get("legacy");
            assertEquals(150, stock.quantity());
            assertNull(stock.owner());
            var xp = connection.createStatement().executeQuery("SELECT exp FROM stock");
            assertTrue(xp.next());
            assertEquals(42, xp.getLong(1));
        }
    }
    @Test void assignmentAndTransferRemainDurableAfterRestart(@TempDir Path directory) throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("state.db");
        var repository = new ProductionOwnershipRepository("stock");
        try (var connection = DriverManager.getConnection(url)) {
            source(connection);
            repository.initialize(connection);
            repository.assign(connection, "legacy", new ProductionOwnershipRepository.Ownership(OWNER, FARM), "ADMIN_ASSIGN");
        }
        UUID next = new UUID(0, 3);
        try (var connection = DriverManager.getConnection(url)) {
            assertEquals(OWNER, repository.owners(connection).get("legacy").owner());
            repository.assign(connection, "legacy", new ProductionOwnershipRepository.Ownership(next, FARM), "ADMIN_TRANSFER");
            repository.assign(connection, "legacy", new ProductionOwnershipRepository.Ownership(next, FARM), "ADMIN_TRANSFER");
        }
        try (var connection = DriverManager.getConnection(url)) {
            var owners = repository.owners(connection);
            assertEquals(1, owners.size());
            assertEquals(next, owners.get("legacy").owner());
            assertEquals(150, repository.stock(connection, owners).get("legacy").quantity());
        }
    }
    @Test void interruptedTransferRollsBackPreviousOwner() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            source(connection);
            var repository = new ProductionOwnershipRepository("stock");
            repository.initialize(connection);
            repository.assign(connection, "legacy", new ProductionOwnershipRepository.Ownership(OWNER, FARM), "ADMIN_ASSIGN");
            connection.createStatement().execute("CREATE TRIGGER fail_transfer BEFORE INSERT ON stock_production_owners_v1 BEGIN SELECT RAISE(ABORT,'simulated failure'); END");
            assertThrows(SQLException.class, () -> repository.assign(connection, "legacy",
                    new ProductionOwnershipRepository.Ownership(new UUID(0, 3), FARM), "ADMIN_TRANSFER"));
            assertEquals(OWNER, repository.owners(connection).get("legacy").owner());
            var history = connection.createStatement().executeQuery("SELECT COUNT(*) FROM stock_production_owner_events_v1");
            assertTrue(history.next());
            assertEquals(1, history.getInt(1)); // failed transfer cannot leave a committed audit/assignment split
            assertTrue(connection.getAutoCommit());
        }
    }
    @Test void migrationInputIsDurableAndDoesNotFollowLaterVisitors(@TempDir Path directory) throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        var repository = new ProductionOwnershipRepository("stock");
        try (var connection = DriverManager.getConnection(url)) {
            source(connection);
            connection.createStatement().execute("ALTER TABLE stock ADD COLUMN loc_y INTEGER DEFAULT 12");
            connection.createStatement().execute("ALTER TABLE stock ADD COLUMN last_interacted_player TEXT");
            connection.createStatement().execute("UPDATE stock SET last_interacted_player='FirstPlayer'");
            repository.initialize(connection);
            assertEquals("FirstPlayer", repository.legacyRows(connection).getFirst().lastPlayer());
            connection.createStatement().execute("UPDATE stock SET last_interacted_player='LaterVisitor'");
        }
        try (var connection = DriverManager.getConnection(url)) {
            assertEquals("FirstPlayer", repository.legacyRows(connection).getFirst().lastPlayer());
            assertEquals(12, repository.legacyRows(connection).getFirst().y());
            assertEquals(150, repository.stock(connection,repository.owners(connection)).get("legacy").quantity());
        }
    }

    @Test void automatedMigrationIsIdempotentAndNeverOverwritesAConfirmedOwner() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            source(connection);
            var repository = new ProductionOwnershipRepository("stock");
            repository.initialize(connection);
            var decision = new LegacyProductionMigration.Decision(
                    new ProductionOwnershipRepository.Ownership(OWNER,FARM),"LEGACY_LAST_INTERACTOR");
            assertTrue(repository.assignMigrated(connection,"legacy",decision));
            assertFalse(repository.assignMigrated(connection,"legacy",decision));
            var different = new LegacyProductionMigration.Decision(
                    new ProductionOwnershipRepository.Ownership(new UUID(0,5),FARM),"LEGACY_PROTECTION_OWNER");
            assertFalse(repository.assignMigrated(connection,"legacy",different));
            assertEquals(OWNER,repository.owners(connection).get("legacy").owner());
            var history=connection.createStatement().executeQuery("SELECT COUNT(*) FROM stock_production_owner_events_v1");
            assertTrue(history.next()); assertEquals(1,history.getInt(1));
            assertEquals(150,repository.stock(connection,repository.owners(connection)).get("legacy").quantity());
        }
    }

    @Test void farmLinksAndPlacerSurviveRestartWithoutLootOrOwnerRewrite(@TempDir Path directory) throws Exception {
        String url="jdbc:sqlite:"+directory.resolve("links.db");
        var repository=new ProductionOwnershipRepository("stock");
        UUID other=new UUID(0,9);
        try(var connection=DriverManager.getConnection(url)) {
            source(connection); repository.initialize(connection);
            repository.assign(connection,"legacy",ProductionFarmGroups.placedOwner(OWNER),"NEW_PLACER_PENDING_FARM",OWNER);
            var links=java.util.List.of(new ProductionFarmGroups.Link(FARM,other));
            repository.linkFarms(connection,links); repository.linkFarms(connection,links);
        }
        try(var connection=DriverManager.getConnection(url)) {
            assertEquals(1,repository.farmLinks(connection).size());
            var groups=new ProductionFarmGroups(repository.farmLinks(connection));
            assertEquals(groups.canonical(FARM),groups.canonical(other));
            assertEquals(OWNER,repository.owners(connection).get("legacy").owner());
            assertNull(repository.owners(connection).get("legacy").farm());
            assertEquals(150,repository.stock(connection,repository.owners(connection)).get("legacy").quantity());
        }
    }
    @Test void interruptedFarmUnionRollsBackEveryLink() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            source(connection); var repository=new ProductionOwnershipRepository("stock"); repository.initialize(connection);
            connection.createStatement().execute("CREATE TRIGGER fail_link BEFORE INSERT ON stock_production_farm_links_v1 WHEN NEW.right_uuid='00000000-0000-0000-0000-000000000009' BEGIN SELECT RAISE(ABORT,'failure'); END");
            assertThrows(SQLException.class,()->repository.linkFarms(connection,java.util.List.of(
                new ProductionFarmGroups.Link(OWNER,FARM),new ProductionFarmGroups.Link(OWNER,new UUID(0,9)))));
            assertTrue(repository.farmLinks(connection).isEmpty()); assertTrue(connection.getAutoCommit());
        }
    }

}
