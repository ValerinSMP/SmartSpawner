package github.nighter.smartspawner.production;
import java.util.*;
public final class OwnSpawnerListing {
    public enum Filter { ALL, PRODUCING, HELD; public Filter next() { return values()[(ordinal()+1)%values().length]; } }
    public enum Sort { TYPE, STOCK, PRODUCING; public Sort next() { return values()[(ordinal()+1)%values().length]; } }
    public record Entry(String id, UUID responsible, String world, int x, int y, int z,
                        String type, int stock, int productive) {}
    public record Page(List<Entry> entries, int number, int pages, int blocks, long stock, long productive,
                       List<String> types) {}
    private OwnSpawnerListing() {}
    public static boolean canInspect(UUID viewer,UUID target,boolean ownPermission,boolean othersPermission) {
        return viewer!=null && target!=null && ownPermission && (viewer.equals(target) || othersPermission);
    }
    public static Page page(UUID viewer, Collection<Entry> entries, int requested) {
        return page(viewer,entries,requested,Filter.ALL,null,Sort.TYPE);
    }
    public static Page page(UUID viewer, Collection<Entry> entries, int requested, Filter filter, String type, Sort sort) {
        Objects.requireNonNull(viewer); Objects.requireNonNull(filter); Objects.requireNonNull(sort);
        var own=entries.stream().filter(entry -> viewer.equals(entry.responsible())).toList();
        var types=own.stream().map(Entry::type).distinct().sorted().toList();
        Comparator<Entry> order=Comparator.comparing(Entry::world).thenComparing(Entry::type).thenComparing(Entry::id);
        if(sort==Sort.STOCK) order=Comparator.comparingInt(Entry::stock).reversed().thenComparing(order);
        if(sort==Sort.PRODUCING) order=Comparator.comparingInt(Entry::productive).reversed().thenComparing(order);
        var selected=own.stream().filter(entry -> type==null || type.equals(entry.type()))
                .filter(entry -> filter==Filter.ALL || filter==Filter.PRODUCING && entry.productive()>0
                        || filter==Filter.HELD && entry.productive()<entry.stock()).sorted(order).toList();
        int pages=Math.max(1,(selected.size()+44)/45),number=Math.max(1,Math.min(requested,pages));
        int from=(number-1)*45;
        return new Page(List.copyOf(selected.subList(from,Math.min(from+45,selected.size()))),number,pages,selected.size(),
                selected.stream().mapToLong(Entry::stock).sum(),selected.stream().mapToLong(Entry::productive).sum(),types);
    }
}
