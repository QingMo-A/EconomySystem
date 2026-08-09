package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.*;
import org.junit.jupiter.api.Test;

class DemandOrderDeliveryServiceTest {
  @Test
  void successPaysAndDeliversOnce() {
    F f = new F();
    DemandOrderDeliveryOutcome o = f.run();
    assertEquals(DemandOrderDeliveryResult.SUCCESS, o.result());
    assertEquals(MarketMutationState.CHANGED, o.mutationState());
    assertEquals(17, f.balance);
    assertEquals(15, f.items);
    assertTrue(f.order.delivered());
    assertEquals(DemandOrderDeliveryResult.ALREADY_DELIVERED, f.run().result());
    assertEquals(17, f.balance);
  }

  @Test
  void staleTransitionCompensates() {
    F f = new F();
    f.status = DemandDeliveryTransitionStatus.ORDER_CHANGED;
    DemandOrderDeliveryOutcome o = f.run();
    assertEquals(DemandOrderDeliveryResult.ORDER_CHANGED, o.result());
    assertEquals(MarketMutationState.CHANGED, o.mutationState());
    assertEquals(0, f.balance);
    assertEquals(20, f.items);
  }

  @Test
  void unknownTransitionLeavesPriorMutationsUntouched() {
    F f = new F();
    f.nullTransition = true;
    DemandOrderDeliveryOutcome o = f.run();
    assertEquals(DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN, o.result());
    assertEquals(MarketMutationState.UNKNOWN, o.mutationState());
    assertEquals(17, f.balance);
    assertEquals(15, f.items);
    assertEquals(0, f.debitCalls);
    assertEquals(0, f.rollbackCalls);
  }

  @Test
  void transitionMutationThenExceptionIsNotBlindlyCompensated() {
    F f = new F();
    f.transitionMutatesThenThrows = true;

    DemandOrderDeliveryOutcome outcome = f.run();

    assertEquals(DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN, outcome.result());
    assertEquals(MarketMutationState.UNKNOWN, outcome.mutationState());
    assertTrue(f.order.delivered());
    assertEquals(17, f.balance);
    assertEquals(15, f.items);
    assertEquals(0, f.debitCalls);
    assertEquals(0, f.rollbackCalls);
  }

  @Test void nullMessageAndMissingOrderFailWithoutMutation() {
    F f = new F(); assertEquals(DemandOrderDeliveryResult.INVALID_CONTEXT,
        DemandOrderDeliveryService.execute(null, f.context()).result());
    f.order = null; assertEquals(DemandOrderDeliveryResult.NOT_FOUND,
        DemandOrderDeliveryService.execute(new DeliverDemandOrderMessage(UUID.randomUUID()), f.context()).result());
    assertEquals(0, f.creditCalls); assertEquals(0, f.removeCalls);
  }

  @Test void invalidOrderStatesFailBeforeExternalMutation() {
    F f = new F(); f.order = f.copy(MarketOrderType.SALES, false, 5, 17); assertEquals(DemandOrderDeliveryResult.WRONG_ORDER_TYPE, f.run().result());
    f = new F(); f.order = f.copy(MarketOrderType.DEMAND, true, 5, 17); assertEquals(DemandOrderDeliveryResult.ALREADY_DELIVERED, f.run().result());
    f = new F(); f.supplier = f.order.sellerId(); assertEquals(DemandOrderDeliveryResult.SELF_DELIVERY, f.run().result());
  }

  @Test void invalidPriceAndQuantityFailClosed() {
    F valid = new F(); assertThrows(IllegalArgumentException.class,
        () -> valid.copy(MarketOrderType.DEMAND, false, 5, 0));
    assertThrows(IllegalArgumentException.class,
        () -> valid.copy(MarketOrderType.DEMAND, false, 0, 17));
  }

  @Test void snapshotRestoreNullAndExceptionAreReported() {
    F f = new F(); f.restoreNull = true; assertEquals(DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, f.run().result()); assertEquals("snapshot-restore", f.reports.get(0).stage());
    f = new F(); f.restoreThrows = true; assertEquals(DemandOrderDeliveryResult.ITEM_RESTORE_FAILED, f.run().result()); assertNotNull(f.reports.get(0).primaryError());
  }

  @Test void inventoryOwnerMismatchAndExceptionFailClosed() {
    F f = new F(); f.ownerMismatch = true; assertEquals(DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, f.run().result());
    f = new F(); f.ownerThrows = true; assertEquals(DemandOrderDeliveryResult.INVENTORY_CONTEXT_FAILED, f.run().result()); assertEquals("inventory-owner", f.reports.get(0).stage());
  }

  @Test void previewFailuresAndBalanceLimitDoNotRemoveItems() {
    F f = new F(); f.preview = null; assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED, f.run().result());
    f = new F(); f.previewThrows = true; assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED, f.run().result());
    f = new F(); f.preview = BalanceMutationResult.BALANCE_LIMIT; assertEquals(DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT, f.run().result()); assertEquals(0, f.removeCalls);
  }

  @Test void inventoryCountExceptionAndInsufficientItemsFailBeforeRemoval() {
    F f = new F(); f.countThrows = true; assertEquals(DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED, f.run().result());
    f = new F(); f.items = 4; assertEquals(DemandOrderDeliveryResult.INSUFFICIENT_ITEMS, f.run().result()); assertEquals(0, f.removeCalls);
  }

  @Test void removalNullExceptionAndExplicitFailuresAreFailClosed() {
    F f = new F(); f.removeNull = true; assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, f.run().result());
    f = new F(); f.removeThrows = true; assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, f.run().result());
    f = new F(); f.removalFailure = true; f.failureRestored = true; assertEquals(DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED, f.run().result());
    f = new F(); f.removalFailure = true; assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, f.run().result());
  }

  @Test void knownCreditFailuresRollbackInventory() {
    F f = new F(); f.credit = BalanceMutationResult.BALANCE_LIMIT; assertEquals(DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT, f.run().result()); assertEquals(1, f.rollbackCalls);
    f = new F(); f.credit = BalanceMutationResult.PERSIST_FAILED; f.rollbackSucceeds = false; assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, f.run().result());
  }

  @Test void unknownCreditStateDoesNotBlindlyRestoreInventory() {
    F returnedNull = new F(); returnedNull.credit = null;
    DemandOrderDeliveryOutcome nullOutcome = returnedNull.run();
    assertEquals(DemandOrderDeliveryResult.PAYMENT_STATE_UNKNOWN, nullOutcome.result());
    assertEquals(MarketMutationState.UNKNOWN, nullOutcome.mutationState());
    assertEquals(15, returnedNull.items); assertEquals(0, returnedNull.rollbackCalls);

    F thrown = new F(); thrown.creditThrows = true;
    assertEquals(DemandOrderDeliveryResult.PAYMENT_STATE_UNKNOWN, thrown.run().result());
    assertEquals(15, thrown.items); assertEquals(0, thrown.rollbackCalls);

    F mutatedThenThrew = new F(); mutatedThenThrew.creditMutatesThenThrows = true;
    assertEquals(DemandOrderDeliveryResult.PAYMENT_STATE_UNKNOWN, mutatedThenThrew.run().result());
    assertEquals(17, mutatedThenThrew.balance); assertEquals(15, mutatedThenThrew.items);
    assertEquals(0, mutatedThenThrew.debitCalls); assertEquals(0, mutatedThenThrew.rollbackCalls);
  }

  @Test void creditFailureTelemetryDistinguishesActiveRollbackFromRemovalFailureRestore() {
    F restored = new F();
    restored.credit = BalanceMutationResult.PERSIST_FAILED;
    assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED, restored.run().result());
    DemandOrderDeliveryFailure restoredFailure = restored.reports.get(restored.reports.size() - 1);
    assertNull(restoredFailure.inventoryRemovalFailureRestored());
    assertTrue(restoredFailure.inventoryRollbackSucceeded());
    assertFalse(restoredFailure.paymentCreditSucceeded());

    F failed = new F();
    failed.credit = BalanceMutationResult.PERSIST_FAILED;
    failed.rollbackSucceeds = false;
    assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, failed.run().result());
    DemandOrderDeliveryFailure failedFailure = failed.reports.get(failed.reports.size() - 1);
    assertNull(failedFailure.inventoryRemovalFailureRestored());
    assertFalse(failedFailure.inventoryRollbackSucceeded());
    assertFalse(failedFailure.paymentCreditSucceeded());
  }

  @Test void failedOrUnknownPaymentReversalNeverRestoresInventory() {
    F rejected = new F(); rejected.status = DemandDeliveryTransitionStatus.PERSIST_FAILED;
    rejected.debit = BalanceMutationResult.PERSIST_FAILED;
    DemandOrderDeliveryOutcome rejectedOutcome = rejected.run();
    assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, rejectedOutcome.result());
    assertEquals(MarketMutationState.UNKNOWN, rejectedOutcome.mutationState());
    assertEquals(17, rejected.balance); assertEquals(15, rejected.items);
    assertEquals(1, rejected.debitCalls); assertEquals(0, rejected.rollbackCalls);
    assertEquals(MarketMutationState.UNKNOWN,
        rejected.reports.get(rejected.reports.size() - 1).mutationState());

    F unknown = new F(); unknown.status = DemandDeliveryTransitionStatus.PERSIST_FAILED;
    unknown.debitMutatesThenThrows = true; unknown.rollbackThrows = true;
    DemandOrderDeliveryOutcome unknownOutcome = unknown.run();
    assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, unknownOutcome.result());
    assertEquals(MarketMutationState.UNKNOWN, unknownOutcome.mutationState());
    assertEquals(0, unknown.balance); assertEquals(15, unknown.items);
    assertEquals(1, unknown.debitCalls); assertEquals(0, unknown.rollbackCalls);
    DemandOrderDeliveryFailure report = unknown.reports.get(unknown.reports.size() - 1);
    assertEquals(unknown.order.sellerId(), report.requesterId());
    assertNotNull(report.paymentError()); assertNull(report.inventoryError());
  }

  @Test void reporterExceptionDoesNotChangeSuccessfulOrFailedOutcome() {
    F f = new F(); f.reporterThrows = true; f.items = 0; assertEquals(DemandOrderDeliveryResult.INSUFFICIENT_ITEMS, f.run().result());
    f = new F(); f.reporterThrows = true; assertEquals(DemandOrderDeliveryResult.SUCCESS, f.run().result());
  }

  static final class F
      implements MarketItemMaterializer,
          TransactionalInventoryRemoval,
          DemandOrderDeliveryService.Account,
          DemandOrderDeliveryService.Repository {
    UUID supplier = UUID.randomUUID();
    MarketOrder order =
        new MarketOrder(
            MarketOrderType.DEMAND,
            UUID.randomUUID(),
            MarketOrderCodecTest.item(),
            5,
            17,
            "buyer",
            UUID.randomUUID(),
            1,
            2,
            false);
    int items = 20, balance, removeCalls, rollbackCalls, creditCalls, debitCalls;
    DemandDeliveryTransitionStatus status = DemandDeliveryTransitionStatus.UPDATED;
    BalanceMutationResult preview = BalanceMutationResult.SUCCESS;
    BalanceMutationResult credit = BalanceMutationResult.SUCCESS, debit = BalanceMutationResult.SUCCESS;
    boolean nullTransition, restoreNull, restoreThrows, ownerMismatch, ownerThrows, previewThrows,
        countThrows, removeNull, removeThrows, removalFailure, failureRestored, creditThrows,
        creditMutatesThenThrows, rollbackSucceeds = true, rollbackThrows, debitThrows,
        debitMutatesThenThrows, transitionMutatesThenThrows, reporterThrows;
    List<DemandOrderDeliveryFailure> reports = new ArrayList<>();

    public Object restore(MarketOrder o) {
      if (restoreThrows) throw new IllegalStateException("restore");
      if (restoreNull) return null;
      return new Object();
    }

    public UUID ownerId() {
      if (ownerThrows) throw new IllegalStateException("owner");
      if (ownerMismatch) return UUID.randomUUID();
      return supplier;
    }

    public long countMatching(Object o) {
      if (countThrows) throw new IllegalStateException("count");
      return items;
    }

    public InventoryRemovalResult removeMatching(Object o, int q) {
      removeCalls++;
      if (removeThrows) throw new IllegalStateException("remove");
      if (removeNull) return null;
      if (removalFailure) return InventoryRemovalResult.failure(failureRestored);
      int before = items;
      items -= q;
      return InventoryRemovalResult.success(
          () -> {
            rollbackCalls++;
            if (rollbackThrows) throw new IllegalStateException("rollback");
            if (!rollbackSucceeds) return false;
            items = before;
            return true;
          });
    }

    public BalanceMutationResult previewCreditExact(int a) {
      if (previewThrows) throw new IllegalStateException("preview");
      return preview;
    }

    public BalanceMutationResult creditExact(int a) {
      creditCalls++;
      if (creditMutatesThenThrows) {
        balance += a;
        throw new IllegalStateException("credit");
      }
      if (creditThrows) throw new IllegalStateException("credit");
      if (credit != BalanceMutationResult.SUCCESS) return credit;
      balance += a;
      return BalanceMutationResult.SUCCESS;
    }

    public BalanceMutationResult debitExact(int a) {
      debitCalls++;
      if (debitMutatesThenThrows) {
        balance -= a;
        throw new IllegalStateException("debit");
      }
      if (debitThrows) throw new IllegalStateException("debit");
      if (debit != BalanceMutationResult.SUCCESS) return debit;
      balance -= a;
      return BalanceMutationResult.SUCCESS;
    }

    public MarketOrder find(UUID id) {
      return order;
    }

    public DemandDeliveryTransition markDemandDeliveredIfUnchanged(UUID id, MarketOrder expected) {
      if (nullTransition) return null;
      if (status != DemandDeliveryTransitionStatus.UPDATED)
        return DemandDeliveryTransition.failure(status);
      MarketOrder updated = copy(MarketOrderType.DEMAND, true, order.quantity(), order.totalPrice());
      DemandDeliveryTransition t = DemandDeliveryTransition.updated(order, updated);
      order = updated;
      if (transitionMutatesThenThrows) throw new IllegalStateException("transition");
      return t;
    }

    DemandOrderDeliveryOutcome run() {
      return DemandOrderDeliveryService.execute(new DeliverDemandOrderMessage(order.tradeId()), context());
    }

    DemandOrderDeliveryService.Context context() {
      return new DemandOrderDeliveryService.Context(supplier, this, this, this, this, failure -> {
        reports.add(failure); if (reporterThrows) throw new IllegalStateException("reporter");
      });
    }

    MarketOrder copy(MarketOrderType type, boolean delivered, int quantity, int price) {
      return new MarketOrder(type, order.tradeId(), order.item(), quantity, price, order.sellerName(),
          order.sellerId(), order.listingTime(), order.expirationTime(), delivered);
    }
  }
}
