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
  void unknownTransitionInvalidates() {
    F f = new F();
    f.nullTransition = true;
    DemandOrderDeliveryOutcome o = f.run();
    assertEquals(DemandOrderDeliveryResult.LEDGER_STATE_UNKNOWN, o.result());
    assertEquals(MarketMutationState.UNKNOWN, o.mutationState());
    assertEquals(0, f.balance);
    assertEquals(20, f.items);
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
    int items = 20, balance;
    DemandDeliveryTransitionStatus status = DemandDeliveryTransitionStatus.UPDATED;
    boolean nullTransition;

    public Object restore(MarketOrder o) {
      return new Object();
    }

    public UUID ownerId() {
      return supplier;
    }

    public long countMatching(Object o) {
      return items;
    }

    public InventoryRemovalResult removeMatching(Object o, int q) {
      int before = items;
      items -= q;
      return InventoryRemovalResult.success(
          () -> {
            items = before;
            return true;
          });
    }

    public BalanceMutationResult previewCreditExact(int a) {
      return BalanceMutationResult.SUCCESS;
    }

    public BalanceMutationResult creditExact(int a) {
      balance += a;
      return BalanceMutationResult.SUCCESS;
    }

    public BalanceMutationResult debitExact(int a) {
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
      MarketOrder updated =
          new MarketOrder(
              order.type(),
              order.tradeId(),
              order.item(),
              order.quantity(),
              order.totalPrice(),
              order.sellerName(),
              order.sellerId(),
              order.listingTime(),
              order.expirationTime(),
              true);
      DemandDeliveryTransition t = DemandDeliveryTransition.updated(order, updated);
      order = updated;
      return t;
    }

    DemandOrderDeliveryOutcome run() {
      return DemandOrderDeliveryService.execute(
          new DeliverDemandOrderMessage(order.tradeId()),
          new DemandOrderDeliveryService.Context(
              supplier, this, this, this, this, DemandOrderDeliveryService.FailureReporter.noop()));
    }
  }
}
