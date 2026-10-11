package github.nighter.smartspawner.production;

import java.sql.*;
import java.util.*;


/** Additive sidecar schema, no writes to upstream stock/loot/XP. Called only by the worker. */
public final class ProductionOwnershipRepository {
    public record Ownership(UUID owner, UUID farm) {}
    private final String source;
    private final String table;
    private final String history;
    private final String legacyInputs;
    public ProductionOwnershipRepository(String source) {
        if (!source.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Unsafe table");
        this.source = source;
        this.table = source + "_production_owners_v1";
        this.history = source + "_production_owner_events_v1";
        this.legacyInputs = source + "_production_legacy_inputs_v1";
    }

    public void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "spawner_id VARCHAR(64) PRIMARY KEY, owner_uuid VARCHAR(36), farm_uuid VARCHAR(36),"
                    + "assignment_kind VARCHAR(32) NOT NULL, updated_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + legacyInputs + " ("
                    + "spawner_id VARCHAR(64) PRIMARY KEY, world VARCHAR(255) NOT NULL,"
                    + "loc_x INTEGER NOT NULL, loc_y INTEGER NOT NULL, loc_z INTEGER NOT NULL,"
                    + "last_player VARCHAR(64), captured_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + history + " ("
                    + "operation_id VARCHAR(36) PRIMARY KEY, spawner_id VARCHAR(64) NOT NULL,"
                    + "actor_uuid VARCHAR(36), old_owner_uuid VARCHAR(36), old_farm_uuid VARCHAR(36),"
                    + "new_owner_uuid VARCHAR(36), new_farm_uuid VARCHAR(36),"
                    + "assignment_kind VARCHAR(32) NOT NULL, updated_at BIGINT NOT NULL)");
        }
    }

    /** Capture the first migration input durably; retries never use a later GUI visitor. */
    public List<LegacyProductionMigration.Row> legacyRows(Connection connection) throws SQLException {
        try (PreparedStatement capture = connection.prepareStatement("INSERT INTO " + legacyInputs
                + " (spawner_id,world,loc_x,loc_y,loc_z,last_player,captured_at)"
                + " SELECT s.spawner_id,s.world,s.loc_x,s.loc_y,s.loc_z,s.last_interacted_player,?"
                + " FROM " + source + " s WHERE NOT EXISTS (SELECT 1 FROM " + legacyInputs + " i WHERE i.spawner_id=s.spawner_id)")) {
            capture.setLong(1, System.currentTimeMillis());
            capture.setQueryTimeout(10);
            capture.executeUpdate();
        }
        List<LegacyProductionMigration.Row> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery("SELECT i.spawner_id,i.world,i.loc_x,i.loc_y,i.loc_z,i.last_player"
                    + " FROM " + legacyInputs + " i JOIN " + source + " s ON s.spawner_id=i.spawner_id")) {
                while (result.next()) rows.add(new LegacyProductionMigration.Row(result.getString(1),
                        result.getString(2), result.getInt(3), result.getInt(4), result.getInt(5), result.getString(6)));
            }
        }
        return List.copyOf(rows);
    }

    public boolean assignMigrated(Connection connection, String id, LegacyProductionMigration.Decision decision) throws SQLException {
        Ownership existing = null;
        try (PreparedStatement query = connection.prepareStatement("SELECT owner_uuid,farm_uuid FROM " + table + " WHERE spawner_id=?")) {
            query.setString(1, id);
            try (ResultSet row = query.executeQuery()) {
                if (row.next()) existing = new Ownership(row.getString(1) == null ? null : UUID.fromString(row.getString(1)),
                        row.getString(2) == null ? null : UUID.fromString(row.getString(2)));
            }
        }
        if (existing != null && existing.owner() != null && existing.farm() != null) return false;
        if (existing != null && existing.owner() != null && !existing.owner().equals(decision.ownership().owner()))
            return false;
        assign(connection, id, decision.ownership(), decision.kind(), null);
        return true;
    }

    public Map<String,Integer> heights(Connection connection) throws SQLException {
        Map<String,Integer> result=new HashMap<>();
        try (Statement query=connection.createStatement()) {
            query.setQueryTimeout(10);
            try(ResultSet rows=query.executeQuery("SELECT spawner_id,loc_y FROM "+source)) {
                while(rows.next()) result.put(rows.getString(1),rows.getInt(2));
            }
        }
        return Map.copyOf(result);
    }

    public Map<String, Ownership> owners(Connection connection) throws SQLException {
        Map<String, Ownership> result = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT spawner_id, owner_uuid, farm_uuid FROM " + table)) {
            while (rows.next()) {
                String owner = rows.getString(2), farm = rows.getString(3);
                result.put(rows.getString(1), new Ownership(owner == null ? null : UUID.fromString(owner),
                        farm == null ? null : UUID.fromString(farm)));
            }
        }
        return Map.copyOf(result);
    }

    public void assign(Connection connection, String id, Ownership ownership, String kind) throws SQLException {
        assign(connection, id, ownership, kind, null);
    }

    public void assign(Connection connection, String id, Ownership ownership, String kind, UUID actor) throws SQLException {
        // Replace both fields together. Keep provenance explicitly separate from assignment.
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String oldOwner = null, oldFarm = null;
            try (PreparedStatement previousOwner = connection.prepareStatement("SELECT owner_uuid,farm_uuid FROM " + table + " WHERE spawner_id = ?")) {
                previousOwner.setString(1, id);
                try (ResultSet row = previousOwner.executeQuery()) {
                    if (row.next()) { oldOwner = row.getString(1); oldFarm = row.getString(2); }
                }
            }
            try (PreparedStatement event = connection.prepareStatement("INSERT INTO " + history
                    + " (operation_id,spawner_id,actor_uuid,old_owner_uuid,old_farm_uuid,new_owner_uuid,new_farm_uuid,assignment_kind,updated_at) VALUES (?,?,?,?,?,?,?,?,?)")) {
                event.setString(1, UUID.randomUUID().toString());
                event.setString(2, id);
                event.setString(3, actor == null ? null : actor.toString());
                event.setString(4, oldOwner);
                event.setString(5, oldFarm);
                event.setString(6, ownership.owner() == null ? null : ownership.owner().toString());
                event.setString(7, ownership.farm() == null ? null : ownership.farm().toString());
                event.setString(8, kind);
                event.setLong(9, System.currentTimeMillis());
                event.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE spawner_id = ?")) {
                delete.setString(1, id);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table
                    + " (spawner_id,owner_uuid,farm_uuid,assignment_kind,updated_at) VALUES (?,?,?,?,?)")) {
                insert.setString(1, id);
                insert.setString(2, ownership.owner() == null ? null : ownership.owner().toString());
                insert.setString(3, ownership.farm() == null ? null : ownership.farm().toString());
                insert.setString(4, kind);
                insert.setLong(5, System.currentTimeMillis());
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public Map<String, ProductionCapacityPlanner.Stock> stock(Connection connection,
            Map<String, Ownership> owners) throws SQLException {
        Map<String, ProductionCapacityPlanner.Stock> result = new HashMap<>();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery("SELECT spawner_id,world,loc_x,loc_z,"
                    + "entity_type,itemspawner_type,stack_size FROM " + source)) {
                while (rows.next()) {
                    String id = rows.getString(1);
                    Ownership ownership = owners.get(id);
                    String entity = rows.getString(5);
                    result.put(id, new ProductionCapacityPlanner.Stock(id, rows.getString(2),
                            rows.getInt(3), rows.getInt(4),
                            "ITEM".equals(entity) ? "ITEM:" + rows.getString(6) : "MOB:" + entity,
                            ownership == null ? null : ownership.owner(),
                            ownership == null ? null : ownership.farm(), rows.getInt(7)));
                }
            }
        }
        return result;
    }
}
