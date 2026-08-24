package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemandOrderDeliveryServiceTest {
  @Test
  void wholeFulfillmentStagesMailboxPaysSupplierCommitsNotificationAndRemovesOrder() {
    Fixture f = new Fixture();

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.SUCCESS, outcome.result());
    assertEquals(MarketMutationState.CHANGED, outcome.mutationState());
    assertFalse(outcome.deliveredOrder().orElseThrow().delivered());
    assertEquals(5, outcome.deliveredOrder().orElseThrow().quantity());
    assertEquals(20, outcome.deliveredOrder().orElseThrow().totalPrice());
    assertEquals(15, f.items);
    assertEquals(20, f.balance);
    assertNull(f.order);
    assertEquals(1, f.mailboxStageCalls);
    assertEquals(1, f.mailboxCommitCalls);
    assertEquals(0, f.mailboxRollbackCalls);
    assertEquals(5, f.mailboxQuantity);
    assertEquals(0, f.mailboxRemaining);
    assertEquals(DemandOrderDeliveryResult.NOT_FOUND, f.run(0).result());
  }

  @Test
  void partialFulfillmentPaysOnlySliceAndKeepsSameTradeIdRemainder() {
    Fixture f = new Fixture();
    f.order = f.order(64, 1_280, false);

    DemandOrderDeliveryOutcome outcome = f.run(10);

    assertEquals(DemandOrderDeliveryResult.SUCCESS, outcome.result());
    assertEquals(10, outcome.deliveredOrder().orElseThrow().quantity());
    assertEquals(200, outcome.deliveredOrder().orElseThrow().totalPrice());
    assertEquals(10, f.mailboxQuantity);
    assertEquals(200, f.mailboxAmount);
    assertEquals(54, f.mailboxRemaining);
    assertEquals(200, f.balance);
    assertEquals(10, 20 - f.items);
    assertNotNull(f.order);
    assertEquals(f.tradeId, f.order.tradeId());
    assertEquals(54, f.order.quantity());
    assertEquals(1_080, f.order.totalPrice());
  }

  @Test
  void nonDivisibleLegacyOrderRejectsPartialButAllowsWholeFulfillment() {
    Fixture partial = new Fixture();
    partial.order = partial.order(5, 17, false);
    assertEquals(DemandOrderDeliveryResult.PARTIAL_FILL_UNSUPPORTED, partial.run(1).result());
    assertEquals(20, partial.items);
    assertEquals(0, partial.balance);
    assertNotNull(partial.order);
    assertEquals(0, partial.mailboxStageCalls);

    Fixture whole = new Fixture();
    whole.order = whole.order(5, 17, false);
    DemandOrderDeliveryOutcome outcome = whole.run(0);
    assertEquals(DemandOrderDeliveryResult.SUCCESS, outcome.result());
    assertEquals(17, whole.balance);
    assertNull(whole.order);
  }

  @Test
  void mailboxFullFailsBeforeOrderInventoryOrPaymentMutation() {
    Fixture f = new Fixture();
    f.mailboxPreflight = DemandMailboxResult.FULL;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.MAILBOX_FULL, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertEquals(20, f.items);
    assertEquals(0, f.balance);
    assertNotNull(f.order);
    assertEquals(0, f.fillCalls);
    assertEquals(0, f.removeCalls);
    assertEquals(0, f.creditCalls);
  }

  @Test
  void mailboxStageFailureRestoresInventoryAndExactOrderBeforePayment() {
    Fixture f = new Fixture();
    MarketOrder original = f.order;
    f.mailboxStageResult = DemandMailboxResult.FAILED;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.MAILBOX_DELIVERY_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertEquals(original, f.order);
    assertEquals(20, f.items);
    assertEquals(0, f.balance);
    assertEquals(0, f.creditCalls);
    assertEquals(1, f.inventoryRollbackCalls);
    assertEquals(1, f.orderRollbackCalls);
  }

  @Test
  void unknownMailboxStageDoesNotBlindlyRestorePotentiallyPersistedAttachment() {
    Fixture f = new Fixture();
    f.mailboxStageResult = DemandMailboxResult.UNKNOWN;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.MAILBOX_STATE_UNKNOWN, outcome.result());
    assertEquals(MarketMutationState.UNKNOWN, outcome.mutationState());
    assertNull(f.order);
    assertEquals(15, f.items);
    assertEquals(0, f.balance);
    assertEquals(0, f.creditCalls);
    assertEquals(0, f.inventoryRollbackCalls);
    assertEquals(0, f.orderRollbackCalls);
  }

  @Test
  void staleOrderCasFailsBeforeSupplierInventoryOrPaymentMutation() {
    Fixture f = new Fixture();
    f.fillStatus = MarketPartialFillStatus.ORDER_CHANGED;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.ORDER_CHANGED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertNotNull(f.order);
    assertEquals(20, f.items);
    assertEquals(0, f.balance);
    assertEquals(0, f.removeCalls);
    assertEquals(0, f.creditCalls);
    assertEquals(0, f.mailboxStageCalls);
  }

  @Test
  void knownCreditFailureRollsBackStagedMailboxInventoryAndOrder() {
    Fixture f = new Fixture();
    MarketOrder original = f.order;
    f.creditResult = BalanceMutationResult.PERSIST_FAILED;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertEquals(original, f.order);
    assertEquals(20, f.items);
    assertEquals(0, f.balance);
    assertEquals(1, f.mailboxStageCalls);
    assertEquals(1, f.mailboxRollbackCalls);
    assertEquals(0, f.mailboxCommitCalls);
    assertEquals(1, f.inventoryRollbackCalls);
    assertEquals(1, f.orderRollbackCalls);
  }

  @Test
  void mailboxRollbackUnknownAfterCreditFailureDoesNotDuplicateByRestoringOtherState() {
    Fixture f = new Fixture();
    f.creditResult = BalanceMutationResult.PERSIST_FAILED;
    f.mailboxRollbackResult = DemandMailboxResult.UNKNOWN;

    DemandOrderDeliveryOutcome outcome = f.run(0);

    assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNKNOWN, outcome.mutationState());
    assertNull(f.order);
    assertEquals(15, f.items);
    assertEquals(0, f.balance);
    assertEquals(0, f.inventoryRollbackCalls);
    assertEquals(0, f.orderRollbackCalls);
  }

  @Test
  void legacyDeliveredOrderRemainsOutsideNewSettlementPath() {
    Fixture f = new Fixture();
    f.order = f.order(5, 20, true);

    assertEquals(DemandOrderDeliveryResult.ALREADY_DELIVERED, f.run(0).result());
    assertEquals(20, f.items);
    assertEquals(0, f.balance);
    assertEquals(0, f.mailboxStageCalls);
  }

  @Test
  void reporterFailureDoesNotChangeBusinessOutcome() {
    Fixture f = new Fixture();
    f.reporterThrows = true;
    f.mailboxPreflight = DemandMailboxResult.FULL;
    assertEquals(DemandOrderDeliveryResult.MAILBOX_FULL, f.run(0).result());
  }

  private static final class Fixture
      implements MarketItemMaterializer,
          TransactionalInventoryRemoval,
          DemandOrderDeliveryService.Account,
          DemandOrderDeliveryService.Repository,
          DemandOrderDeliveryService.Mailbox {
    final UUID supplierId = UUID.randomUUID();
    final UUID requesterId = UUID.randomUUID();
    final UUID tradeId = UUID.randomUUID();
    MarketOrder order = order(5, 20, false);
    int items = 20;
    int balance;
    int removeCalls;
    int inventoryRollbackCalls;
    int creditCalls;
    int debitCalls;
    int fillCalls;
    int orderRollbackCalls;
    int mailboxStageCalls;
    int mailboxCommitCalls;
    int mailboxRollbackCalls;
    int mailboxQuantity;
    int mailboxAmount;
    int mailboxRemaining;
    BalanceMutationResult previewResult = BalanceMutationResult.SUCCESS;
    BalanceMutationResult creditResult = BalanceMutationResult.SUCCESS;
    BalanceMutationResult debitResult = BalanceMutationResult.SUCCESS;
    MarketPartialFillStatus fillStatus;
    DemandMailboxResult mailboxPreflight = DemandMailboxResult.SUCCESS;
    DemandMailboxResult mailboxStageResult = DemandMailboxResult.SUCCESS;
    DemandMailboxResult mailboxRollbackResult = DemandMailboxResult.SUCCESS;
    boolean reporterThrows;
    final List<DemandOrderDeliveryFailure> reports = new ArrayList<>();

    MarketOrder order(int quantity, int totalPrice, boolean delivered) {
      return new MarketOrder(MarketOrderType.DEMAND, tradeId, MarketOrderCodecTest.item(),
          quantity, totalPrice, "requester", requesterId, 1, 2, delivered);
    }

    DemandOrderDeliveryOutcome run(int quantity) {
      UUID id = order == null ? tradeId : order.tradeId();
      return DemandOrderDeliveryService.execute(
          new DeliverDemandOrderMessage(id, quantity),
          new DemandOrderDeliveryService.Context(
              supplierId,
              this,
              this,
              this,
              this,
              this,
              failure -> {
                reports.add(failure);
                if (reporterThrows) throw new IllegalStateException("reporter");
              }));
    }

    @Override
    public Object restore(MarketOrder ignored) {
      return new Object();
    }

    @Override
    public UUID ownerId() {
      return supplierId;
    }

    @Override
    public long countMatching(Object ignored) {
      return items;
    }

    @Override
    public InventoryRemovalResult removeMatching(Object ignored, int quantity) {
      removeCalls++;
      if (items < quantity) return InventoryRemovalResult.failure(true);
      int before = items;
      items -= quantity;
      return InventoryRemovalResult.success(() -> {
        inventoryRollbackCalls++;
        items = before;
        return true;
      });
    }

    @Override
    public BalanceMutationResult previewCreditExact(int amount) {
      return previewResult;
    }

    @Override
    public BalanceMutationResult creditExact(int amount) {
      creditCalls++;
      if (creditResult == BalanceMutationResult.SUCCESS) balance += amount;
      return creditResult;
    }

    @Override
    public BalanceMutationResult debitExact(int amount) {
      debitCalls++;
      if (debitResult == BalanceMutationResult.SUCCESS) balance -= amount;
      return debitResult;
    }

    @Override
    public MarketOrder find(UUID id) {
      return order != null && order.tradeId().equals(id) ? order : null;
    }

    @Override
    public MarketPartialFillTransition fillIfUnchanged(
        UUID id, MarketOrderType expectedType, MarketOrder expected, int quantity) {
      fillCalls++;
      if (fillStatus != null) return MarketPartialFillTransition.failure(fillStatus);
      if (order == null) return MarketPartialFillTransition.failure(MarketPartialFillStatus.NOT_FOUND);
      if (order.type() != expectedType) {
        return MarketPartialFillTransition.failure(MarketPartialFillStatus.WRONG_ORDER_TYPE);
      }
      if (!order.equals(expected)) {
        return MarketPartialFillTransition.failure(MarketPartialFillStatus.ORDER_CHANGED);
      }
      int amount = MarketOrderPricing.fillAmount(order, quantity);
      MarketOrder previous = order;
      MarketOrder remaining = quantity == previous.quantity() ? null
          : new MarketOrder(previous.type(), previous.tradeId(), previous.item(),
              previous.quantity() - quantity, previous.totalPrice() - amount,
              previous.sellerName(), previous.sellerId(), previous.listingTime(),
              previous.expirationTime(), false);
      order = remaining;
      return MarketPartialFillTransition.applied(previous, remaining, quantity, amount, () -> {
        orderRollbackCalls++;
        order = previous;
        return MarketPartialFillRollbackResult.RESTORED;
      });
    }

    @Override
    public DemandMailboxResult preflight(UUID requesterId, Object template, int quantity) {
      return mailboxPreflight;
    }

    @Override
    public DemandOrderDeliveryService.MailboxStage stage(
        UUID requesterId,
        MarketOrder fulfilledSlice,
        Object template,
        int quantity,
        int amount,
        int remainingQuantity) {
      mailboxStageCalls++;
      mailboxQuantity = quantity;
      mailboxAmount = amount;
      mailboxRemaining = remainingQuantity;
      if (mailboxStageResult != DemandMailboxResult.SUCCESS) {
        return DemandOrderDeliveryService.MailboxStage.failure(mailboxStageResult);
      }
      return DemandOrderDeliveryService.MailboxStage.success(
          new DemandOrderDeliveryService.StagedMailboxDelivery() {
            private boolean closed;

            @Override
            public void commit() {
              if (closed) return;
              closed = true;
              mailboxCommitCalls++;
            }

            @Override
            public DemandMailboxResult rollback() {
              if (closed) return DemandMailboxResult.UNKNOWN;
              mailboxRollbackCalls++;
              if (mailboxRollbackResult == DemandMailboxResult.SUCCESS) closed = true;
              return mailboxRollbackResult;
            }
          });
    }
  }
}
