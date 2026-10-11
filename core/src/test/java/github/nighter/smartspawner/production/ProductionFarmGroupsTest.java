package github.nighter.smartspawner.production;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductionFarmGroupsTest {
    private final UUID a=new UUID(0,1), b=new UUID(0,2), c=new UUID(0,3);
    @Test void overlapSameOwnerCanResolveFallback() {
        var claim=ProductionFarmGroups.claim(Map.of("rpb:w:A",Set.of(a),"rpb:w:B",Set.of(a)));
        assertEquals(a,claim.owner()); assertFalse(claim.ambiguous()); assertEquals(2,claim.farmKeys().size());
    }
    @Test void overlapDifferentOwnersUsesKnownActorWithoutInventingFallback() {
        var claim=ProductionFarmGroups.claim(Map.of("rpb:w:A",Set.of(a),"rpb:w:B",Set.of(b)));
        assertNull(claim.owner()); assertTrue(claim.ambiguous());
        var row=new LegacyProductionMigration.Row("x","w",0,70,0,"placer");
        assertNull(LegacyProductionMigration.decide("stock",row,null,claim,null));
        assertEquals(c,LegacyProductionMigration.decide("stock",row,c,claim,null).ownership().owner());
    }
    @Test void groupingIsTransitiveOrderIndependentAndDoesNotGroupUnrelatedFarms() {
        var links=List.of(new ProductionFarmGroups.Link(c,b),new ProductionFarmGroups.Link(b,a));
        var first=new ProductionFarmGroups(links);
        var second=new ProductionFarmGroups(List.of(links.get(1),links.get(0)));
        assertEquals(a,first.canonical(c)); assertEquals(first.canonical(c),second.canonical(c));
        UUID unrelated=new UUID(0,4); assertEquals(unrelated,first.canonical(unrelated));
    }
    @Test void overlapSharesBudgetOutsideIntersectionAcrossOwnersAndTypes() {
        var groups=new ProductionFarmGroups(List.of(new ProductionFarmGroups.Link(a,b)));
        var stock=List.of(new ProductionCapacityPlanner.Stock("a","w",0,0,"MOB:BLAZE",a,groups.canonical(a),80),
                new ProductionCapacityPlanner.Stock("b","w",1000,0,"MOB:ZOMBIE",b,groups.canonical(b),80));
        var grants=ProductionCapacityPlanner.allocate(stock,Map.of(new ProductionCapacityPlanner.Context(a,"w"),100,
                new ProductionCapacityPlanner.Context(b,"w"),100),100,33,100);
        assertEquals(100,grants.values().stream().mapToInt(ProductionCapacityPlanner.Grant::productive).sum());
        assertEquals(160,stock.stream().mapToInt(ProductionCapacityPlanner.Stock::quantity).sum());
    }
    @Test void freshPlacementBelongsToPlacerAndNotTerritoryOrCarriedOwner() {
        var prior=new ProductionOwnershipRepository.Ownership(a,b);
        var owner=ProductionFarmGroups.placedOwner(c);
        assertEquals(c,owner.owner()); assertNull(owner.farm()); assertEquals(a,prior.owner());
        var decision=LegacyProductionMigration.decide("stock",new LegacyProductionMigration.Row("x","w",0,70,0,"old"),
                a,new LegacyProductionMigration.Claim("rpb:w:owner",a,false),owner);
        assertEquals(c,decision.ownership().owner());
    }
    @Test void friendCannotSilentlyAddUnitsToAnotherOwnersStack() {
        var target=new ProductionOwnershipRepository.Ownership(a,b);
        assertTrue(ProductionFarmGroups.canAddToStack(a,target));
        assertFalse(ProductionFarmGroups.canAddToStack(c,target));
        assertFalse(ProductionFarmGroups.canAddToStack(c,new ProductionOwnershipRepository.Ownership(null,b)));
    }
    @Test void manualFarmJoinsPhysicalBudgetWithoutChangingOwner() {
        UUID nativeFarm=ProductionFarmGroups.physical("stock","rpb:w:A");
        var groups=new ProductionFarmGroups(List.of(new ProductionFarmGroups.Link(nativeFarm,b)));
        assertEquals(groups.canonical(nativeFarm),groups.canonical(b));
        var claim=ProductionFarmGroups.claim(Map.of("rpb:w:A",Set.of(c)));
        var prior=new ProductionOwnershipRepository.Ownership(a,b);
        assertEquals(prior,LegacyProductionMigration.decide("stock",new LegacyProductionMigration.Row("x","w",0,0,0,"visitor"),c,claim,prior).ownership());
    }
}
