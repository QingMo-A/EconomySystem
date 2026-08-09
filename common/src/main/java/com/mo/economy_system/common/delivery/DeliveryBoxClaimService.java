package com.mo.economy_system.common.delivery;

import com.mo.economy_system.common.market.InventoryInsertionResult;
import com.mo.economy_system.common.market.TransactionalInventory;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative claim: reserve, insert, then persistently remove or compensate. */
public final class DeliveryBoxClaimService {
  private DeliveryBoxClaimService() {}

  public static DeliveryBoxClaimResult claim(DeliveryBoxClaimMessage message, Context context) {
    if (message == null || context == null) return DeliveryBoxClaimResult.INVALID_ENTRY;
    DeliveryBoxRepository.Reservation reservation;
    try {
      reservation = context.repository().reserve(context.ownerId(), message.entryId());
    } catch (RuntimeException failure) {
      report(context, message.entryId(), "reserve", DeliveryBoxClaimResult.STATE_UNKNOWN, failure);
      return DeliveryBoxClaimResult.STATE_UNKNOWN;
    }
    if (reservation == null) return DeliveryBoxClaimResult.NOT_FOUND;
    try {
      DeliveryBoxEntrySnapshot entry = Objects.requireNonNull(reservation.entry(), "entry");
      if (!entry.entryId().equals(message.entryId())
          || !ItemStackSnapshotValidator.validate(entry.item()).isSuccess()) {
        reservation.release();
        return DeliveryBoxClaimResult.INVALID_ENTRY;
      }
      Object template;
      try {
        template = context.materializer().restore(entry);
      } catch (RuntimeException failure) {
        reservation.release();
        report(context, message.entryId(), "restore", DeliveryBoxClaimResult.ITEM_RESTORE_FAILED, failure);
        return DeliveryBoxClaimResult.ITEM_RESTORE_FAILED;
      }
      if (template == null) {
        reservation.release();
        return DeliveryBoxClaimResult.ITEM_RESTORE_FAILED;
      }
      boolean accepts;
      try {
        accepts = context.inventory().canAccept(template, entry.item().count());
      } catch (RuntimeException failure) {
        reservation.release();
        report(context, message.entryId(), "capacity", DeliveryBoxClaimResult.INVENTORY_FAILED, failure);
        return DeliveryBoxClaimResult.INVENTORY_FAILED;
      }
      if (!accepts) {
        reservation.release();
        return DeliveryBoxClaimResult.INVENTORY_FULL;
      }
      InventoryInsertionResult insertion;
      try {
        insertion = context.inventory().insert(template, entry.item().count());
      } catch (RuntimeException failure) {
        reservation.release();
        // Insertion can have partially changed the inventory before throwing. Releasing the
        // reservation is safe, but neither the entry nor the inventory may be compensated
        // blindly.
        report(context, message.entryId(), "insert-state-unknown", DeliveryBoxClaimResult.STATE_UNKNOWN, failure);
        return DeliveryBoxClaimResult.STATE_UNKNOWN;
      }
      if (insertion == null) {
        reservation.release();
        report(context, message.entryId(), "insert-state-unknown", DeliveryBoxClaimResult.STATE_UNKNOWN,
            new IllegalStateException("null inventory insertion result"));
        return DeliveryBoxClaimResult.STATE_UNKNOWN;
      }
      if (!insertion.succeeded() || insertion.rollback() == null) {
        reservation.release();
        return insertion.failureRestored()
            ? DeliveryBoxClaimResult.INVENTORY_FAILED
            : DeliveryBoxClaimResult.ROLLBACK_FAILED;
      }
      DeliveryBoxRepository.CommitResult committed;
      try {
        committed = reservation.commit(context.dirty());
      } catch (RuntimeException failure) {
        committed = DeliveryBoxRepository.CommitResult.STATE_UNKNOWN;
        report(context, message.entryId(), "commit", DeliveryBoxClaimResult.STATE_UNKNOWN, failure);
      }
      if (committed == DeliveryBoxRepository.CommitResult.REMOVED) {
        return DeliveryBoxClaimResult.SUCCESS;
      }
      if (committed == null || committed == DeliveryBoxRepository.CommitResult.STATE_UNKNOWN) {
        // The entry may already have been removed. Keeping the delivered inventory is the only
        // non-destructive choice when repository state cannot be proven.
        reservation.release();
        return DeliveryBoxClaimResult.STATE_UNKNOWN;
      }
      boolean inventoryRestored;
      try {
        inventoryRestored = insertion.rollback().rollback();
      } catch (RuntimeException failure) {
        inventoryRestored = false;
        report(context, message.entryId(), "inventory-rollback", DeliveryBoxClaimResult.ROLLBACK_FAILED, failure);
      }
      reservation.release();
      if (!inventoryRestored) return DeliveryBoxClaimResult.ROLLBACK_FAILED;
      return committed == DeliveryBoxRepository.CommitResult.PERSIST_FAILED
          ? DeliveryBoxClaimResult.PERSIST_FAILED
          : DeliveryBoxClaimResult.STATE_UNKNOWN;
    } catch (RuntimeException failure) {
      try {
        reservation.release();
      } catch (RuntimeException ignored) {
      }
      report(context, message.entryId(), "unexpected", DeliveryBoxClaimResult.STATE_UNKNOWN, failure);
      return DeliveryBoxClaimResult.STATE_UNKNOWN;
    }
  }

  private static void report(
      Context context, UUID entryId, String stage, DeliveryBoxClaimResult result, RuntimeException error) {
    try {
      context.reporter().report(context.ownerId(), entryId, stage, result, error);
    } catch (RuntimeException ignored) {
    }
  }

  public record Context(
      UUID ownerId,
      DeliveryBoxRepository repository,
      Materializer materializer,
      TransactionalInventory inventory,
      DeliveryBoxLedger.DirtyMarker dirty,
      FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(ownerId, "ownerId");
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(materializer, "materializer");
      Objects.requireNonNull(inventory, "inventory");
      Objects.requireNonNull(dirty, "dirty");
      Objects.requireNonNull(reporter, "reporter");
      if (!ownerId.equals(inventory.ownerId())) throw new IllegalArgumentException("inventory owner mismatch");
    }
  }

  @FunctionalInterface
  public interface Materializer {
    Object restore(DeliveryBoxEntrySnapshot entry);
  }

  @FunctionalInterface
  public interface FailureReporter {
    void report(UUID ownerId, UUID entryId, String stage, DeliveryBoxClaimResult result, RuntimeException error);

    static FailureReporter noop() {
      return (ownerId, entryId, stage, result, error) -> {};
    }
  }
}
