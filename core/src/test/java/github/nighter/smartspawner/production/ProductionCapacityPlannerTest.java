package github.nighter.smartspawner.production;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductionCapacityPlannerTest {
    private static final UUID OWNER = new UUID(0, 1), FARM = new UUID(0, 2);
    private ProductionCapacityPlanner.Stock stock(String id, String world, int x, int z, String type, UUID owner, UUID farm, int quantity) {
        return new ProductionCapacityPlanner.Stock(id, world, x, z, type, owner, farm, quantity);
    }
    private Map<ProductionCapacityPlanner.Context, Integer> quota(UUID owner, String world, int quantity) {
        return Map.of(new ProductionCapacityPlanner.Context(owner, world), quantity);
    }
    private int total(Map<String, ProductionCapacityPlanner.Grant> plan) {
        return plan.values().stream().mapToInt(ProductionCapacityPlanner.Grant::productive).sum();
    }
    @Test void actualStackUnitsPartialStockAndDeterministicOrder() {
        var a = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 40);
        var b = stock("b", "world", 1, 1, "MOB:BLAZE", OWNER, FARM, 30);
        var plan = ProductionCapacityPlanner.allocate(List.of(b, a), quota(OWNER, "world", 50), 100, 33, 100);
        assertEquals(40, plan.get("a").productive());
        assertEquals(10, plan.get("b").productive());
        assertEquals(30, b.quantity());
        assertEquals(plan, ProductionCapacityPlanner.allocate(List.of(a, b), quota(OWNER, "world", 50), 100, 33, 100));
    }
    @Test void allSixRankTiersApplyPerTypeWithoutExtraUserTotalCap() {
        for (int tier : List.of(50, 60, 70, 80, 90, 100)) {
            var one = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 150);
            var two = stock("b", "world", 1000, 0, "MOB:ZOMBIE", OWNER, new UUID(0, 3), 150);
            var plan = ProductionCapacityPlanner.allocate(List.of(one, two), quota(OWNER, "world", tier), 100, 33, 100);
            assertEquals(tier, plan.get("a").productive());
            assertEquals(tier, plan.get("b").productive());
            assertEquals(tier * 2, total(plan));
        }
    }
    @Test void legacyUnknownOwnerFarmAndPermissionContextHoldWithoutDeletingStock() {
        var ownerless = stock("a", "world", 0, 0, "MOB:BLAZE", null, FARM, 200);
        var farmless = stock("b", "world", 1000, 0, "MOB:BLAZE", OWNER, null, 200);
        var noContext = stock("c", "unknown", 2000, 0, "MOB:BLAZE", OWNER, FARM, 200);
        var plan = ProductionCapacityPlanner.allocate(List.of(ownerless, farmless, noContext),
                quota(OWNER, "world", 100), 100, 33, 100);
        assertEquals(0, total(plan));
        assertEquals(600, plan.values().stream().mapToInt(g -> g.stock().quantity()).sum());
    }
    @Test void sharedFarmAndFriendAccountsCannotAddTheirQuotas() {
        var one = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 80);
        UUID friend = new UUID(0, 4);
        var two = stock("b", "world", 1000, 0, "MOB:ZOMBIE", friend, FARM, 80);
        var plan = ProductionCapacityPlanner.allocate(List.of(one, two),
                Map.of(new ProductionCapacityPlanner.Context(OWNER, "world"), 100,
                        new ProductionCapacityPlanner.Context(friend, "world"), 100), 100, 33, 100);
        assertEquals(100, total(plan));
        assertEquals(20, plan.get("b").productive());
    }
    @Test void spatialCapCrossesChunksCornersAndTypes() {
        UUID friend = new UUID(0, 4), otherFarm = new UUID(0, 5);
        var one = stock("a", "world", -1, -1, "MOB:BLAZE", OWNER, FARM, 80);
        var two = stock("b", "world", 31, 31, "ITEM:DIAMOND", friend, otherFarm, 80);
        var quotas = Map.of(new ProductionCapacityPlanner.Context(OWNER, "world"), 100,
                new ProductionCapacityPlanner.Context(friend, "world"), 100);
        assertEquals(100, total(ProductionCapacityPlanner.allocate(List.of(one, two), quotas, 100, 33, 100)));
        var outside = stock("b", "world", 32, 31, "ITEM:DIAMOND", friend, otherFarm, 80);
        assertEquals(160, total(ProductionCapacityPlanner.allocate(List.of(one, outside), quotas, 100, 33, 100)));
    }
    @Test void separateWorldsHaveSeparateGeometryButOwnerTypeIsStillOneBudget() {
        var one = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 80);
        var two = stock("b", "other", 0, 0, "MOB:BLAZE", OWNER, new UUID(0, 4), 80);
        var quotas = Map.of(new ProductionCapacityPlanner.Context(OWNER, "world"), 100,
                new ProductionCapacityPlanner.Context(OWNER, "other"), 60);
        assertEquals(60, total(ProductionCapacityPlanner.allocate(List.of(one, two), quotas, 100, 33, 100)));
    }
    @Test void transferReleasesOldOwnerAndChargesNewOwnerWithoutChangingStock() {
        UUID friend = new UUID(0, 4);
        var a = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 50);
        var b = stock("b", "world", 1000, 0, "MOB:BLAZE", OWNER, new UUID(0, 5), 50);
        var quotas = Map.of(new ProductionCapacityPlanner.Context(OWNER, "world"), 50,
                new ProductionCapacityPlanner.Context(friend, "world"), 50);
        assertEquals(50, total(ProductionCapacityPlanner.allocate(List.of(a, b), quotas, 100, 33, 100)));
        var transferred = stock("b", "world", 1000, 0, "MOB:BLAZE", friend, b.farm(), 50);
        assertEquals(100, total(ProductionCapacityPlanner.allocate(List.of(a, transferred), quotas, 100, 33, 100)));
        assertEquals(100, a.quantity() + transferred.quantity());
    }
    @Test void eggChangeReclassifiesUnitsWithoutCreatingAnotherOwnerBudget() {
        var one = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 60);
        var otherFarm = new UUID(0, 10);
        var two = stock("b", "world", 1000, 0, "MOB:ZOMBIE", OWNER, otherFarm, 60);
        assertEquals(100, total(ProductionCapacityPlanner.allocate(List.of(one, two), quota(OWNER, "world", 50), 100, 33, 100)));
        var changed = stock("a", "world", 0, 0, "MOB:ZOMBIE", OWNER, FARM, 60);
        var plan = ProductionCapacityPlanner.allocate(List.of(changed, two), quota(OWNER, "world", 50), 100, 33, 100);
        assertEquals(50, total(plan));
        assertEquals(120, plan.values().stream().mapToInt(g -> g.stock().quantity()).sum());
    }

    @Test void representativeLargeLayoutBenchmarksPlanning(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) {
        List<ProductionCapacityPlanner.Stock> entries = new ArrayList<>();
        Map<ProductionCapacityPlanner.Context, Integer> quotas = new HashMap<>();
        for (int i = 0; i < 3359; i++) {
            UUID owner = new UUID(0, i + 1000);
            entries.add(stock(String.format("%05d", i), "world", (i % 59) * 2 - 60, (i / 59) * 2 - 60,
                    i % 2 == 0 ? "MOB:BLAZE" : "ITEM:DIAMOND", owner, owner, 7));
            quotas.put(new ProductionCapacityPlanner.Context(owner, "world"), 100);
        }
        long started = System.nanoTime();
        var plan = ProductionCapacityPlanner.allocate(entries, quotas, 100, 33, 100);
        long elapsed = (System.nanoTime() - started) / 1_000_000;
        assertEquals(3359, plan.size());
        assertTrue(total(plan) < 3359 * 7);
        System.out.println("Synthetic 3359-block plan duration_ms=" + elapsed + "; NOT a gameplay/TPS benchmark");
    }

    @Test void duplicateIdsAndInvalidSpatialConfigurationAreRejected() {
        var one = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 1);
        assertThrows(IllegalArgumentException.class, () -> ProductionCapacityPlanner.allocate(List.of(one, one),
                quota(OWNER, "world", 50), 100, 33, 100));
        assertThrows(IllegalArgumentException.class, () -> ProductionCapacityPlanner.allocate(List.of(one),
                quota(OWNER, "world", 50), 100, 66, 100));
    }
    @Test void everyWindowSatisfiesCapInRandomBorderLayouts() {
        Random random = new Random(4262);
        List<ProductionCapacityPlanner.Stock> entries = new ArrayList<>();
        Map<ProductionCapacityPlanner.Context, Integer> quotas = new HashMap<>();
        for (int i = 0; i < 160; i++) {
            UUID owner = new UUID(0, i + 100);
            entries.add(stock(String.format("%03d", i), "world", random.nextInt(90) - 45,
                    random.nextInt(90) - 45, i % 2 == 0 ? "MOB:BLAZE" : "ITEM:DIAMOND", owner, owner, 10));
            quotas.put(new ProductionCapacityPlanner.Context(owner, "world"), 100);
        }
        var plan = ProductionCapacityPlanner.allocate(entries, quotas, 100, 33, 100);
        for (int x = -80; x <= 50; x++) {
            for (int z = -80; z <= 50; z++) {
                int sum = 0;
                for (var grant : plan.values()) {
                    var p = grant.stock();
                    if (p.x() >= x && p.x() <= x + 32 && p.z() >= z && p.z() <= z + 32) sum += grant.productive();
                }
                assertTrue(sum <= 100, "Window exceeds spatial cap");
            }
        }
    }

    @Test void warmedSpatialWindowsNeverEmitNegativeCapacity() {
        var entries = new ArrayList<ProductionCapacityPlanner.Stock>();
        var quotas = new HashMap<ProductionCapacityPlanner.Context, Integer>();
        for (int group = 0; group < 96; group++) {
            UUID owner = new UUID(2, group + 1), farm = new UUID(3, group + 1);
            quotas.put(new ProductionCapacityPlanner.Context(owner, "world"), 100);
            String id = String.format("%03d", group);
            int x = group * 100 - 4800;
            entries.add(stock(id + "a", "world", x, 0, "MOB:BLAZE", owner, farm, 80));
            entries.add(stock(id + "b", "world", x + 32, 32, "MOB:ZOMBIE", owner, farm, 21));
            entries.add(stock(id + "c", "world", x + 16, 16, "MOB:SKELETON", owner, farm, 5));
        }
        for (int warmup = 0; warmup < 16; warmup++) {
            var plan = ProductionCapacityPlanner.allocate(entries, quotas, 100, 33, 100);
            assertEquals(9600, total(plan));
            for (var grant : plan.values()) {
                assertTrue(grant.productive() >= 0);
                assertTrue(grant.productive() <= grant.stock().quantity());
                assertEquals(grant.stock().id().endsWith("a") ? 80
                        : grant.stock().id().endsWith("b") ? 20 : 0, grant.productive());
            }
        }
    }

    @Test void invalidGrantCannotBePublishedAsReady() {
        var entry = stock("a", "world", 0, 0, "MOB:BLAZE", OWNER, FARM, 25);
        assertThrows(IllegalArgumentException.class, () -> new ProductionCapacityPlanner.Grant(entry, -1, "STOCK_HELD"));
        assertThrows(IllegalArgumentException.class, () -> new ProductionCapacityPlanner.Grant(entry, 26, "PRODUCTIVE"));
        assertEquals(0, new ProductionCapacityPlanner.Grant(entry, 0, "STOCK_HELD").productive());
    }
}
