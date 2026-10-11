package github.nighter.smartspawner.production;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static github.nighter.smartspawner.production.ProductionSimulation.*;

class ProductionSimulationTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private Placement row(String id, UUID owner, String type, long stock, int x, int y, int z) {
        return new Placement(id,"world",x,y,z,type,owner,Ownership.VERIFIED,stock,0,1);
    }
    private Report plan(List<Placement> rows, long cap, Optional<SpatialProposal> spatial) {
        return ProductionSimulation.plan(rows,Map.of(A,new Quota(PermissionStatus.RESOLVED,cap),
                B,new Quota(PermissionStatus.RESOLVED,cap)),spatial);
    }
    @Test void countsUnitsInsideStacksAndDifferentChunksWithoutDeletingStock() {
        Report r=plan(List.of(row("a",A,"BLAZE",40,0,0,0),row("b",A,"BLAZE",40,1600,0,0)),50,Optional.empty());
        assertEquals(80,r.totalStock()); assertEquals(50,r.wouldProduce()); assertEquals(30,r.wouldHold());
        assertEquals(10,r.allocations().get(1).wouldProduce());
    }
    @Test void baseTypeCapsDoNotCreateAGlobalOwnerCap() {
        assertEquals(100,plan(List.of(row("a",A,"BLAZE",50,0,0,0),row("b",A,"ZOMBIE",50,0,0,0)),50,Optional.empty()).wouldProduce());
    }
    @Test void unresolvedLegacyDoesNotBecomeProtectionOwner() {
        Placement legacy=new Placement("a","world",0,0,0,"BLAZE",null,Ownership.LEGACY_UNRESOLVED,200,0,1);
        Report r=plan(List.of(legacy),100,Optional.empty());
        assertEquals(200,r.wouldHold()); assertEquals("OWNERSHIP_UNRESOLVED",r.allocations().getFirst().reason());
    }
    @Test void unresolvedOfflinePermissionDoesNotInventARank() {
        Report r=ProductionSimulation.plan(List.of(row("a",A,"BLAZE",100,0,0,0)),
                Map.of(A,new Quota(PermissionStatus.UNKNOWN,100)),Optional.empty());
        assertEquals(0,r.wouldProduce()); assertEquals("PERMISSION_CONTEXT_UNKNOWN",r.allocations().getFirst().reason());
    }
    @Test void respectsEachEffectiveTierAndContextDenialsAlreadyResolvedByAdapter() {
        for(int tier:new int[]{50,60,70,80,90,100}) assertEquals(tier,tierQuota(Set.of(tier)));
        assertEquals(90,tierQuota(Set.of(60,90))); assertEquals(50,tierQuota(Set.of()));
        assertThrows(IllegalArgumentException.class,()->tierQuota(Set.of(200)));
    }
    @Test void selectedUnitOrderIsStableAfterReloadOrInputReordering() {
        var a=row("a",A,"BLAZE",40,0,0,0); var b=row("b",A,"BLAZE",40,1,0,0);
        assertEquals(plan(List.of(a,b),50,Optional.empty()),plan(List.of(b,a),50,Optional.empty()));
    }
    @Test void ownerTransferRecomputesUsageAndNeverAddsUnits() {
        var before=List.of(row("a",A,"BLAZE",40,0,0,0),row("b",A,"BLAZE",40,100,0,0));
        var after=List.of(before.getFirst(),row("b",B,"BLAZE",40,100,0,0));
        assertEquals(80,plan(after,50,Optional.empty()).totalStock());
        assertEquals(50,plan(before,50,Optional.empty()).wouldProduce());
        assertEquals(80,plan(after,50,Optional.empty()).wouldProduce());
    }
    @Test void neighboringChunksCornersAndAllHeightsShareOneSpatialBudget() {
        Report r=plan(List.of(row("a",A,"BLAZE",80,15,-64,15),row("b",B,"ZOMBIE",80,16,320,16)),100,
                Optional.of(new SpatialProposal(32,100)));
        assertEquals(100,r.wouldProduce()); assertEquals(160,r.totalStock());
    }
    @Test void additionalOwnersCannotMultiplySameFarmFootprint() {
        List<Placement> rows=List.of(row("a",A,"BLAZE",100,0,0,0),row("b",B,"BLAZE",100,0,64,0));
        assertEquals(100,plan(rows,100,Optional.of(new SpatialProposal(32,100))).wouldProduce());
        assertThrows(IllegalArgumentException.class,()->plan(List.of(rows.getFirst(),rows.getFirst()),100,Optional.empty()));
    }
    @Test void differentWorldsDoNotShareSpatialCapacity() {
        Placement b=new Placement("b","nether",0,0,0,"BLAZE",B,Ownership.VERIFIED,100,0,1);
        assertEquals(200,plan(List.of(row("a",A,"BLAZE",100,0,0,0),b),100,
                Optional.of(new SpatialProposal(32,100))).wouldProduce());
    }
    @Test void slidingWindowsIncludeSpanBoundaryAndDoNotMergeInfiniteChains() {
        var rows=List.of(row("a",A,"BLAZE",80,0,0,0),row("b",B,"BLAZE",80,32,0,0),
                row("c",A,"ZOMBIE",80,64,0,0));
        assertEquals(180,plan(rows,100,Optional.of(new SpatialProposal(32,100))).wouldProduce());
        assertEquals(160,plan(List.of(row("a",A,"BLAZE",80,0,0,0),row("b",B,"BLAZE",80,33,0,0)),
                100,Optional.of(new SpatialProposal(32,100))).wouldProduce());
    }
    @Test void parallelPlanningIsReadOnlyAndRepeatable() throws Exception {
        var rows=List.of(row("a",A,"BLAZE",100,0,0,0));
        var expected=plan(rows,50,Optional.empty());
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=List.<Callable<Report>>of(()->plan(rows,50,Optional.empty()),()->plan(rows,50,Optional.empty()));
            for(var f:pool.invokeAll(tasks)) assertEquals(expected,f.get());
        }
        assertEquals(100,rows.getFirst().stock());
    }
    @Test void longCountsRejectOverflowInsteadOfWrapping() {
        assertThrows(ArithmeticException.class,()->plan(List.of(row("a",A,"BLAZE",Long.MAX_VALUE,0,0,0),
                row("b",B,"BLAZE",1,0,0,0)),100,Optional.empty()));
    }
}
