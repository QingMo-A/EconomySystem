package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RemoveSalesOrderServiceTest {
  @Test
  void ownerRemovesOnceAndReceivesItems() {
    Fixture f = new Fixture();
    assertState(f.run(), RemoveSalesOrderResult.SUCCESS, MarketMutationState.CHANGED);
    assertEquals(3, f.inventory.count);
    assertState(f.run(), RemoveSalesOrderResult.NOT_FOUND, MarketMutationState.UNCHANGED);
    assertEquals(3, f.inventory.count);
  }

  @Test
  void operatorReturnsItemsByMailboxEvenWhenOwnerIsOffline() {
    Fixture f = new Fixture();
    f.actor = UUID.randomUUID();
    f.operator = true;
    f.resolveMode = 1;
    assertState(f.run(), RemoveSalesOrderResult.SUCCESS, MarketMutationState.CHANGED);
    assertEquals(0, f.inventory.count);
    assertEquals(f.owner, f.mailbox.ownerId);
    assertEquals(3, f.mailbox.quantity);
    assertNull(f.repository.current);
  }

  @Test
  void operatorMailboxFailuresDoNotCreateAnInfiniteReturnPath() {
    Fixture f = new Fixture();
    f.actor = UUID.randomUUID();
    f.operator = true;
    f.mailbox.preflight = DemandMailboxResult.FULL;
    assertState(f.run(), RemoveSalesOrderResult.MAILBOX_FULL, MarketMutationState.UNCHANGED);
    assertNotNull(f.repository.current);

    f = new Fixture();
    f.actor = UUID.randomUUID();
    f.operator = true;
    f.mailbox.delivery = DemandMailboxResult.FAILED;
    assertState(
        f.run(), RemoveSalesOrderResult.MAILBOX_DELIVERY_FAILED, MarketMutationState.UNCHANGED);
    assertNotNull(f.repository.current);

    f = new Fixture();
    f.actor = UUID.randomUUID();
    f.operator = true;
    f.mailbox.delivery = DemandMailboxResult.UNKNOWN;
    assertState(
        f.run(), RemoveSalesOrderResult.MAILBOX_STATE_UNKNOWN, MarketMutationState.UNKNOWN);
    assertNull(f.repository.current);
  }

  @Test
  void permissionExistenceAndTypeFailClosed() {
    Fixture f = new Fixture();
    f.actor = UUID.randomUUID();
    assertState(f.run(), RemoveSalesOrderResult.NOT_OWNER, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.repository.current = null;
    assertState(f.run(), RemoveSalesOrderResult.NOT_FOUND, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.repository.current = f.order(MarketOrderType.DEMAND);
    assertState(f.run(), RemoveSalesOrderResult.WRONG_ORDER_TYPE, MarketMutationState.UNCHANGED);
  }

  @Test
  void offlineOwnerIsDistinctFromResolverFailure() {
    Fixture f = new Fixture();
    f.resolveMode = 1;
    assertState(f.run(), RemoveSalesOrderResult.OWNER_OFFLINE, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.resolveMode = 2;
    assertState(
        f.run(), RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.resolveMode = 3;
    assertState(
        f.run(), RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED, MarketMutationState.UNCHANGED);
  }

  @Test
  void receiverIdentityFailuresAreRejected() {
    Fixture f = new Fixture();
    f.inventory.wrongOwner = true;
    assertState(f.run(), RemoveSalesOrderResult.INVALID_CONTEXT, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.inventory.ownerThrows = true;
    assertState(
        f.run(), RemoveSalesOrderResult.RECEIVER_RESOLUTION_FAILED, MarketMutationState.UNCHANGED);
  }

  @Test
  void materializerAndCapacityFailuresDoNotDelete() {
    Fixture f = new Fixture();
    f.materializeNull = true;
    assertState(f.run(), RemoveSalesOrderResult.ITEM_RESTORE_FAILED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.materializeThrows = true;
    assertState(f.run(), RemoveSalesOrderResult.ITEM_RESTORE_FAILED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.inventory.accept = false;
    assertState(f.run(), RemoveSalesOrderResult.INVENTORY_FULL, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.inventory.capacityThrows = true;
    assertState(
        f.run(), RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED, MarketMutationState.UNCHANGED);
  }

  @Test
  void repositoryFailuresHaveKnownOrUnknownMutation() {
    Fixture f = new Fixture();
    f.repository.removeThrows = true;
    assertState(f.run(), RemoveSalesOrderResult.ORDER_REMOVE_FAILED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.repository.removeNull = true;
    assertState(f.run(), RemoveSalesOrderResult.ORDER_REMOVE_FAILED, MarketMutationState.UNKNOWN);
    f = new Fixture();
    f.repository.status = SalesOrderRemovalStatus.PERSIST_FAILED;
    assertState(f.run(), RemoveSalesOrderResult.ORDER_REMOVE_FAILED, MarketMutationState.UNCHANGED);
  }

  @Test
  void changedOrderIsRestoredOrReportedChanged() {
    Fixture f = new Fixture();
    f.repository.removedOverride =
        new MarketOrder(
            MarketOrderType.SALES,
            f.trade,
            MarketOrderCodecTest.item(),
            3,
            10,
            "changed",
            f.owner,
            2,
            3,
            false);
    assertState(f.run(), RemoveSalesOrderResult.ORDER_CHANGED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.repository.removedOverride =
        new MarketOrder(
            MarketOrderType.SALES,
            f.trade,
            MarketOrderCodecTest.item(),
            3,
            10,
            "changed",
            f.owner,
            2,
            3,
            false);
    f.repository.restore = false;
    assertState(f.run(), RemoveSalesOrderResult.ROLLBACK_FAILED, MarketMutationState.CHANGED);
  }

  @Test
  void insertionNullAndFailuresAlwaysAttemptOrderRestore() {
    Fixture f = new Fixture();
    f.inventory.insertMode = 1;
    assertState(f.run(), RemoveSalesOrderResult.ROLLBACK_FAILED, MarketMutationState.UNCHANGED);
    assertNotNull(f.repository.current);
    f = new Fixture();
    f.inventory.insertMode = 2;
    assertState(
        f.run(), RemoveSalesOrderResult.INVENTORY_MUTATION_FAILED, MarketMutationState.UNCHANGED);
    f = new Fixture();
    f.inventory.insertMode = 3;
    assertState(f.run(), RemoveSalesOrderResult.ROLLBACK_FAILED, MarketMutationState.UNCHANGED);
  }

  @Test
  void insertionAndOrderRestoreFailureMarksMarketChanged() {
    Fixture f = new Fixture();
    f.inventory.insertMode = 3;
    f.repository.restore = false;
    f.reporterThrows = true;
    assertState(f.run(), RemoveSalesOrderResult.ROLLBACK_FAILED, MarketMutationState.CHANGED);
  }

  private static void assertState(
      RemoveSalesOrderOutcome outcome, RemoveSalesOrderResult result, MarketMutationState state) {
    assertEquals(result, outcome.result());
    assertEquals(state, outcome.mutationState());
  }

  private static final class Fixture {
    final UUID owner = UUID.randomUUID(), trade = UUID.randomUUID();
    UUID actor = owner;
    boolean operator, materializeNull, materializeThrows, reporterThrows;
    int resolveMode;
    final FakeInventory inventory = new FakeInventory();
    final FakeRepository repository = new FakeRepository();
    final FakeMailbox mailbox = new FakeMailbox();

    Fixture() {
      repository.current = order(MarketOrderType.SALES);
    }

    MarketOrder order(MarketOrderType type) {
      return new MarketOrder(
          type, trade, MarketOrderCodecTest.item(), 3, 10, "owner", owner, 1, 2, false);
    }

    RemoveSalesOrderOutcome run() {
      return RemoveSalesOrderService.execute(
          new RemoveSalesOrderMessage(trade),
          new RemoveSalesOrderService.Context(
              actor,
              operator,
              order -> {
                if (materializeThrows) throw new IllegalStateException();
                return materializeNull ? null : new Object();
              },
              id -> {
                if (resolveMode == 2) return null;
                if (resolveMode == 3) throw new IllegalStateException();
                return resolveMode == 1 ? Optional.empty() : Optional.of(inventory);
              },
              repository,
              mailbox,
              (a, b, c, d, e, f, g, h, i) -> {
                if (reporterThrows) throw new IllegalStateException();
              }));
    }

    final class FakeInventory implements TransactionalInventory {
      int count, insertMode;
      boolean accept = true, wrongOwner, ownerThrows, capacityThrows;

      public UUID ownerId() {
        if (ownerThrows) throw new IllegalStateException();
        return wrongOwner ? UUID.randomUUID() : owner;
      }

      public boolean canAccept(Object item, int quantity) {
        if (capacityThrows) throw new IllegalStateException();
        return accept;
      }

      public InventoryInsertionResult insert(Object item, int quantity) {
        if (insertMode == 1) return null;
        if (insertMode == 2) return InventoryInsertionResult.failure(true);
        if (insertMode == 3) return InventoryInsertionResult.failure(false);
        count += quantity;
        return InventoryInsertionResult.success(
            () -> {
              count -= quantity;
              return true;
            });
      }
    }

    final class FakeMailbox implements RemoveSalesOrderService.Mailbox {
      DemandMailboxResult preflight = DemandMailboxResult.SUCCESS;
      DemandMailboxResult delivery = DemandMailboxResult.SUCCESS;
      UUID ownerId;
      int quantity;

      @Override
      public DemandMailboxResult preflight(UUID ownerId, Object template, int quantity) {
        return preflight;
      }

      @Override
      public DemandMailboxResult deliver(
          UUID ownerId, MarketOrder order, Object template, int quantity) {
        if (delivery == DemandMailboxResult.SUCCESS) {
          this.ownerId = ownerId;
          this.quantity = quantity;
        }
        return delivery;
      }
    }

    final class FakeRepository implements RemoveSalesOrderService.Repository {
      MarketOrder current, removedOverride;
      boolean removeThrows, removeNull, restore = true;
      SalesOrderRemovalStatus status = SalesOrderRemovalStatus.REMOVED;

      public MarketOrder find(UUID id) {
        return current;
      }

      public SalesOrderRemovalResult removeSalesTransactional(UUID id) {
        if (removeThrows) throw new IllegalStateException();
        if (removeNull) return null;
        if (status != SalesOrderRemovalStatus.REMOVED)
          return SalesOrderRemovalResult.failure(status);
        MarketOrder removed = removedOverride == null ? current : removedOverride;
        current = null;
        return SalesOrderRemovalResult.removed(
            new MarketOrderRemoval(
                removed,
                () -> {
                  if (!restore) return MarketOrderRestoreResult.PERSIST_FAILED;
                  current = removed;
                  return MarketOrderRestoreResult.RESTORED;
                }));
      }
    }
  }
}
