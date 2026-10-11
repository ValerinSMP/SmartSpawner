package github.nighter.smartspawner.production;

import github.nighter.smartspawner.api.production.v1.SpawnerUnitServiceV1.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UnitApiContractTest {
    private final UUID id=UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Test void rejectsAmbiguousOverlappingIdentityIntervals() {
        var key=new OperationKey("survival","recycler",id);
        var target=new InventoryTarget(id,0,"fingerprint");
        assertThrows(IllegalArgumentException.class,()->new ReservationRequest(key,target,1,"campaign",
                List.of(new UnitSlice(id,0,10),new UnitSlice(id,9,2))));
        assertDoesNotThrow(()->new ReservationRequest(key,target,1,"campaign",
                List.of(new UnitSlice(id,0,10),new UnitSlice(id,10,2))));
    }
    @Test void requestCannotBeChangedAfterSubmission() {
        var units=new ArrayList<>(List.of(new UnitSlice(id,0,10)));
        var r=new ReservationRequest(new OperationKey("survival","recycler",id),new InventoryTarget(id,0,"f"),1,"campaign",units);
        units.clear(); assertEquals(1,r.units().size());
        assertThrows(UnsupportedOperationException.class,()->r.units().clear());
    }
    @Test void rejectsZeroOrOverflowingQuantityAndMissingFingerprint() {
        assertThrows(IllegalArgumentException.class,()->new UnitSlice(id,0,0));
        assertThrows(ArithmeticException.class,()->new UnitSlice(id,Long.MAX_VALUE,1));
        assertThrows(IllegalArgumentException.class,()->new InventoryTarget(id,0,""));
    }
}
