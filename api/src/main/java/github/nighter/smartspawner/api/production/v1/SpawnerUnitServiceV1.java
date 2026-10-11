package github.nighter.smartspawner.api.production.v1;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Draft v1 contract. No runtime provider is registered in delivery D1.
 * See docs/valerin/PRODUCTION-OWNERSHIP-API-V1.md before implementing.
 * Implementations capture inventory state on the actor's region thread and do IO asynchronously.
 * A completed future is not proof of consumption: only a durable CONSUMED receipt is.
 */
public interface SpawnerUnitServiceV1 {
    int MAJOR_VERSION = 1;

    enum Recognition { ISSUED, LEGACY_UNVERIFIED, FOREIGN, STALE, UNAVAILABLE }
    enum Provenance { VERIFIED_ISSUANCE, LEGACY_UNVERIFIED }
    enum State { PREPARED, RESERVED, CONSUMED, RELEASED, RECOVERY_REQUIRED }
    enum Outcome { APPLIED, REPLAY, CONFLICT, DENIED, UNAVAILABLE, RECOVERY_REQUIRED }

    record OperationKey(String authority, String client, UUID operationId) {
        public OperationKey {
            if (authority == null || authority.isBlank() || client == null || client.isBlank()
                    || operationId == null) throw new IllegalArgumentException("Invalid operation key");
        }
    }

    /** Stable unit interval; identity survives relocation, splitting and merging. */
    record UnitSlice(UUID issuanceId, long firstOrdinal, long count) {
        public UnitSlice {
            if (issuanceId == null || firstOrdinal < 0 || count <= 0) {
                throw new IllegalArgumentException("Invalid unit interval");
            }
            Math.addExact(firstOrdinal, count);
        }
    }

    /** Slot belongs to the player's storage inventory; no lore-based identity. */
    record InventoryTarget(UUID actorId, int slot, String expectedFingerprint) {
        public InventoryTarget {
            if (actorId == null || slot < 0 || slot >= 36
                    || expectedFingerprint == null || expectedFingerprint.isBlank()) {
                throw new IllegalArgumentException("Invalid inventory target");
            }
        }
    }

    /** Quantity/type are authoritative only when recognition == ISSUED. */
    record Assessment(Recognition recognition, String authority, long revision,
                      String currentBaseType, String currentVariant, String issuedBaseType,
                      long quantity, Provenance provenance, List<UnitSlice> units) {
        public Assessment {
            units = List.copyOf(units);
        }
    }

    /**
     * Payload is immutable and bound durably to key. Same key/different actor, purpose,
     * revision, fingerprint or slices must return CONFLICT, never re-use an old payment.
     * Callback completion must not touch inventories without an explicit region hop.
     */
    record ReservationRequest(OperationKey key, InventoryTarget target, long expectedRevision,
                              String purpose, List<UnitSlice> units) {
        public ReservationRequest {
            units = List.copyOf(units);
            if (key == null || target == null || expectedRevision < 0
                    || purpose == null || purpose.isBlank() || units.isEmpty()) {
                throw new IllegalArgumentException("Invalid reservation");
            }
            for (int i = 0; i < units.size(); i++) {
                for (int j = i + 1; j < units.size(); j++) {
                    UnitSlice a = units.get(i), b = units.get(j);
                    if (a.issuanceId().equals(b.issuanceId())
                            && a.firstOrdinal() < b.firstOrdinal() + b.count()
                            && b.firstOrdinal() < a.firstOrdinal() + a.count()) {
                        throw new IllegalArgumentException("Overlapping unit intervals");
                    }
                }
            }
        }
    }

    /** revision and quantity describe the durable receipt, not a GUI cache. */
    record Receipt(OperationKey key, Outcome outcome, State state, long revision,
                   UUID actorId, long quantity, List<UnitSlice> units, String reasonCode) {
        public Receipt { units = List.copyOf(units); }
    }

    CompletionStage<Assessment> inspect(InventoryTarget target);
    CompletionStage<Receipt> reserve(ReservationRequest request);
    CompletionStage<Receipt> consume(OperationKey key);
    /** Can only release an unconsumed reservation with reconciled inventory state. */
    CompletionStage<Receipt> release(OperationKey key);
    CompletionStage<Receipt> lookup(OperationKey key);
}
