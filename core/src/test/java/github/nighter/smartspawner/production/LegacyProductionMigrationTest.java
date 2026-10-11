package github.nighter.smartspawner.production;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacyProductionMigrationTest {
    private final UUID player = new UUID(0,1), protector = new UUID(0,2);
    private final LegacyProductionMigration.Row row = new LegacyProductionMigration.Row("old","world",0,12,0,"name");
    @Test void approvedLastInteractorPrecedesProtectionOwnerAndIsLabelledAsInference() {
        var d = LegacyProductionMigration.decide("local", row, player,
                new LegacyProductionMigration.Claim("rpb:world:p", protector, false), null);
        assertEquals(player,d.ownership().owner());
        assertEquals("LEGACY_LAST_INTERACTOR", d.kind());
    }
    @Test void missingOrUnresolvableNameFallsBackToProtectionOwner() {
        var d = LegacyProductionMigration.decide("local", row,null,
                new LegacyProductionMigration.Claim("rpb:world:p",protector,false),null);
        assertEquals(protector,d.ownership().owner());
        assertEquals("LEGACY_PROTECTION_OWNER",d.kind());
        assertNull(LegacyProductionMigration.decide("local",row,null,LegacyProductionMigration.Claim.none(),null));
    }
    @Test void aRecordedAssignmentNeverChangesOnFurtherInteractionsOrProtectionTransfer() {
        var existing = new ProductionOwnershipRepository.Ownership(player,new UUID(0,3));
        assertEquals(existing,LegacyProductionMigration.decide("local",row,protector,
                new LegacyProductionMigration.Claim("changed",protector,true),existing).ownership());
    }
    @Test void retainedItemOwnerGetsFarmWithoutBecomingThePlacer() {
        var d = LegacyProductionMigration.decide("local",row,protector,
                new LegacyProductionMigration.Claim("p",protector,false),
                new ProductionOwnershipRepository.Ownership(player,null));
        assertEquals(player,d.ownership().owner());
        assertNotNull(d.ownership().farm());
    }
    @Test void protectionFarmIsNonAdditiveAcrossResponsiblePlayersAndHeights() {
        var claim = new LegacyProductionMigration.Claim("p",protector,false);
        var a=LegacyProductionMigration.decide("local",row,player,claim,null);
        var b=LegacyProductionMigration.decide("local",new LegacyProductionMigration.Row("second","world",32,300,32,"friend"),protector,claim,null);
        assertEquals(a.ownership().farm(),b.ownership().farm());
        assertNotEquals(a.ownership().owner(),b.ownership().owner());
    }
    @Test void unprotectedCellsAreBoundedDeterministicAndDoNotBecomeOwnerGlobalCaps() {
        var a=LegacyProductionMigration.decide("local",row,player,LegacyProductionMigration.Claim.none(),null);
        var same=LegacyProductionMigration.decide("local",new LegacyProductionMigration.Row("b","world",32,300,32,"other"),protector,LegacyProductionMigration.Claim.none(),null);
        var next=LegacyProductionMigration.decide("local",new LegacyProductionMigration.Row("c","world",33,12,0,"name"),player,LegacyProductionMigration.Claim.none(),null);
        assertEquals(a.ownership().farm(),same.ownership().farm());
        assertNotEquals(a.ownership().farm(),next.ownership().farm());
        var negative=LegacyProductionMigration.decide("local",new LegacyProductionMigration.Row("d","world",-1,12,0,"name"),player,LegacyProductionMigration.Claim.none(),null);
        assertNotEquals(a.ownership().farm(),negative.ownership().farm());
    }
    @Test void ambiguousProtectionDoesNotInventOwnershipOrFarm() {
        assertNull(LegacyProductionMigration.decide("local",row,null,new LegacyProductionMigration.Claim(null,null,true),null));
    }
    @Test void oldUnmarkedStockAndNewUnassignedItemsCanJoinButConfirmedForeignDeclarationsCannot() {
        var target = new ProductionOwnershipRepository.Ownership(player,new UUID(0,3));
        assertTrue(LegacyProductionMigration.compatibleForStack(target,new ProductionOwnershipRepository.Ownership(null,null)));
        assertTrue(LegacyProductionMigration.compatibleForStack(target,new ProductionOwnershipRepository.Ownership(player,null)));
        assertFalse(LegacyProductionMigration.compatibleForStack(target,new ProductionOwnershipRepository.Ownership(protector,null)));
        assertFalse(LegacyProductionMigration.compatibleForStack(target,new ProductionOwnershipRepository.Ownership(player,new UUID(0,4))));
    }

    @Test void lastInteractorStillWinsInsideOneFarmWithMultipleProtectionOwners() {
        var shared=new LegacyProductionMigration.Claim("shared",null,true);
        var d=LegacyProductionMigration.decide("local",row,player,shared,null);
        assertEquals(player,d.ownership().owner());
        assertNotNull(d.ownership().farm());
        assertNull(LegacyProductionMigration.decide("local",row,null,shared,null));
    }

}
