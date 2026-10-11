package github.nighter.smartspawner.production;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OwnSpawnerListingTest {
    private final UUID viewer=new UUID(0,1);
    private OwnSpawnerListing.Entry entry(String id,UUID owner) {
        return new OwnSpawnerListing.Entry(id,owner,"unloaded",1,300,-1,"MOB:BLAZE",70,50);
    }
    @Test void includesOnlyRequesterAndDoesNotExposeOtherPlayersOrUnresolvedRecords() {
        var page=OwnSpawnerListing.page(viewer,List.of(entry("mine",viewer),entry("foreign",new UUID(0,2)),entry("unknown",null)),1);
        assertEquals(1,page.blocks());assertEquals(70,page.stock());assertEquals(50,page.productive());
        assertEquals("mine",page.entries().getFirst().id());
        assertEquals(300,page.entries().getFirst().y());
    }
    @Test void paginationAndStaleOutOfBoundsRequestsStayInsideOwnSnapshot() {
        List<OwnSpawnerListing.Entry> entries=new ArrayList<>();
        for(int i=0;i<46;i++) entries.add(entry(String.format("%02d",i),viewer));
        assertEquals(45,OwnSpawnerListing.page(viewer,entries,1).entries().size());
        var last=OwnSpawnerListing.page(viewer,entries,Integer.MAX_VALUE);
        assertEquals(2,last.number());assertEquals(2,last.pages());assertEquals(1,last.entries().size());
        assertEquals(46*70,last.stock());
        assertEquals(1,OwnSpawnerListing.page(viewer,List.of(),0).number());
        assertTrue(OwnSpawnerListing.page(viewer,List.of(),1).entries().isEmpty());
    }
    @Test void filtersIncludePartialStacksWithoutLeakingForeignTypesOrTotals() {
        var entries=List.of(entry("partial",viewer),new OwnSpawnerListing.Entry("held",viewer,"w",0,0,0,"MOB:ZOMBIE",50,0),
                new OwnSpawnerListing.Entry("full",viewer,"w",0,0,0,"MOB:BLAZE",50,50),
                new OwnSpawnerListing.Entry("foreign",new UUID(0,9),"w",0,0,0,"ITEM:DIAMOND",999,999));
        var producing=OwnSpawnerListing.page(viewer,entries,1,OwnSpawnerListing.Filter.PRODUCING,null,OwnSpawnerListing.Sort.TYPE);
        assertEquals(2,producing.blocks());assertEquals(120,producing.stock());assertEquals(100,producing.productive());
        assertEquals(List.of("MOB:BLAZE","MOB:ZOMBIE"),producing.types());
        var held=OwnSpawnerListing.page(viewer,entries,1,OwnSpawnerListing.Filter.HELD,null,OwnSpawnerListing.Sort.TYPE);
        assertEquals(2,held.blocks());assertEquals(120,held.stock());assertEquals(50,held.productive());
        var mob=OwnSpawnerListing.page(viewer,entries,1,OwnSpawnerListing.Filter.ALL,"MOB:ZOMBIE",OwnSpawnerListing.Sort.TYPE);
        assertEquals(1,mob.blocks()); assertEquals(50,mob.stock());
    }
    @Test void filteredPagesClampAndSortingIsDeterministic() {
        var entries=List.of(entry("partial",viewer),new OwnSpawnerListing.Entry("full",viewer,"w",0,0,0,"MOB:ZOMBIE",60,60));
        assertEquals("partial",OwnSpawnerListing.page(viewer,entries,1,OwnSpawnerListing.Filter.ALL,null,OwnSpawnerListing.Sort.STOCK).entries().getFirst().id());
        assertEquals("full",OwnSpawnerListing.page(viewer,entries,1,OwnSpawnerListing.Filter.ALL,null,OwnSpawnerListing.Sort.PRODUCING).entries().getFirst().id());
        var empty=OwnSpawnerListing.page(viewer,entries,999,OwnSpawnerListing.Filter.ALL,"ITEM:DIAMOND",OwnSpawnerListing.Sort.TYPE);
        assertEquals(1,empty.number());assertEquals(0,empty.stock());assertTrue(empty.entries().isEmpty());
    }

}
