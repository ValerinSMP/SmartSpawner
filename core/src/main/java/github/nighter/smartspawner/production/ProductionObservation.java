package github.nighter.smartspawner.production;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

/** Read-only accounting of persisted stock. Does not certify ownership or provenance. */
public final class ProductionObservation {
    private ProductionObservation() {}

    public record TypeStock(long blocks, long units, long invalidBlocks) {}
    public record Snapshot(long observedAtMillis, long durationMillis,
                           long blocks, long units, long invalidBlocks,
                           Map<String, TypeStock> types) {
        public Snapshot {
            types = Map.copyOf(types);
        }
    }

    public static Snapshot read(Connection connection, String table) throws SQLException {
        if (!table.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Unsafe table");
        long started = System.nanoTime();
        // One consistent SELECT, no loaded-world/player API and no stock, loot or XP writes.
        String sql = "SELECT entity_type, itemspawner_type, COUNT(*) AS blocks, "
                + "SUM(CASE WHEN stack_size > 0 THEN CAST(stack_size AS BIGINT) ELSE 0 END) AS units, "
                + "SUM(CASE WHEN stack_size > 0 THEN 0 ELSE 1 END) AS invalid_blocks "
                + "FROM " + table + " GROUP BY entity_type, itemspawner_type";
        Map<String, TypeStock> types = new TreeMap<>();
        long blocks = 0, units = 0, invalid = 0;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    String entity = rows.getString("entity_type");
                    String material = rows.getString("itemspawner_type");
                    String type = "ITEM".equals(entity) ? "ITEM:" + material : "MOB:" + entity;
                    long b = rows.getLong("blocks"), u = rows.getLong("units");
                    long i = rows.getLong("invalid_blocks");
                    TypeStock previous = types.getOrDefault(type, new TypeStock(0, 0, 0));
                    types.put(type, new TypeStock(Math.addExact(previous.blocks(), b),
                            Math.addExact(previous.units(), u), Math.addExact(previous.invalidBlocks(), i)));
                    blocks = Math.addExact(blocks, b);
                    units = Math.addExact(units, u);
                    invalid = Math.addExact(invalid, i);
                }
            }
        }
        return new Snapshot(System.currentTimeMillis(), (System.nanoTime() - started) / 1_000_000,
                blocks, units, invalid, types);
    }
}
