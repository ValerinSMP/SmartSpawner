package github.nighter.smartspawner.production;
import java.util.*;
public final class OwnSpawnerListing {
    public record Entry(String id, UUID responsible, String world, int x, int y, int z,
                        String type, int stock, int productive) {}
    public record Page(List<Entry> entries, int number, int pages, int blocks, long stock, long productive) {}
    private OwnSpawnerListing() {}
    public static Page page(UUID viewer, Collection<Entry> entries, int requested) {
        Objects.requireNonNull(viewer);
        var own = entries.stream().filter(entry -> viewer.equals(entry.responsible()))
                .sorted(Comparator.comparing(Entry::world).thenComparing(Entry::type).thenComparing(Entry::id)).toList();
        int pages = Math.max(1,(own.size()+44)/45), number = Math.max(1,Math.min(requested,pages));
        int from = (number-1)*45;
        return new Page(List.copyOf(own.subList(from,Math.min(from+45,own.size()))),number,pages,own.size(),
                own.stream().mapToLong(Entry::stock).sum(),own.stream().mapToLong(Entry::productive).sum());
    }
}
