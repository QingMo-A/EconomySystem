package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseSalesOrderServiceTest {
  @Test
  void successTransfersOnceInsertsAllAndRemovesOrder() {
    Fixture f = new Fixture();
    PurchaseSalesOrderOutcome out = f.execute();
    assertEquals(PurchaseSalesOrderResult.SUCCESS, out.result());
    assertEquals(MarketMutationState.CHANGED, out.mutationState());
    assertEquals(3, f.inventory.count);
    assertEquals(90, f.accounts.buyer);
    assertEquals(10, f.accounts.seller);
    assertNull(f.repository.current);
    assertEquals(1, f.accounts.transfers);
  }

  @Test
  void missingWrongTypeAndSelfPurchaseDoNotMutate() {
    Fixture missing = new Fixture();
    missing.repository.current = null;
    assertEquals(PurchaseSalesOrderResult.NOT_FOUND, missing.execute().result());
    Fixture wrong = new Fixture();
    wrong.repository.current = wrong.order(MarketOrderType.DEMAND, wrong.seller);
    assertEquals(PurchaseSalesOrderResult.WRONG_ORDER_TYPE, wrong.execute().result());
    Fixture self = new Fixture();
    self.repository.current = self.order(MarketOrderType.SALES, self.buyer);
    assertEquals(PurchaseSalesOrderResult.SELF_PURCHASE, self.execute().result());
    assertEquals(0, self.accounts.transfers);
  }

  @Test
  void balanceAndInventoryPreviewsRunBeforeRemoval() {
    Fixture funds = new Fixture();
    funds.accounts.preview = BalanceTransferResult.INSUFFICIENT_FUNDS;
    assertEquals(PurchaseSalesOrderResult.INSUFFICIENT_FUNDS, funds.execute().result());
    assertNotNull(funds.repository.current);
    Fixture limit = new Fixture();
    limit.accounts.preview = BalanceTransferResult.RECIPIENT_BALANCE_LIMIT;
    assertEquals(PurchaseSalesOrderResult.SELLER_BALANCE_LIMIT, limit.execute().result());
    Fixture full = new Fixture();
    full.inventory.accept = false;
    assertEquals(PurchaseSalesOrderResult.INVENTORY_FULL, full.execute().result());
    assertNotNull(full.repository.current);
  }

  @Test
  void nullBalancePreviewFailsClosedBeforeRemoval() {
    Fixture fixture = new Fixture();
    fixture.accounts.preview = null;
    PurchaseSalesOrderOutcome outcome = fixture.execute();
    assertEquals(PurchaseSalesOrderResult.PAYMENT_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertNotNull(fixture.repository.current);
  }

  @Test
  void nullTransferRollsBackInventoryAndOrder() {
    Fixture fixture = new Fixture();
    fixture.accounts.transfer = null;
    PurchaseSalesOrderOutcome outcome = fixture.execute();
    assertEquals(PurchaseSalesOrderResult.PAYMENT_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertEquals(0, fixture.inventory.count);
    assertNotNull(fixture.repository.current);
  }

  @Test
  void restoreFailureAndRemoveFailureAreReported() {
    Fixture restore = new Fixture();
    restore.inventory.restoreFails = true;
    assertEquals(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED, restore.execute().result());
    Fixture remove = new Fixture();
    remove.repository.fillStatus = MarketPartialFillStatus.PERSIST_FAILED;
    assertEquals(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, remove.execute().result());
  }

  @Test
  void changedOrderIsRejectedWithoutPayment() {
    Fixture f = new Fixture();
    f.repository.fillStatus = MarketPartialFillStatus.ORDER_CHANGED;
    PurchaseSalesOrderOutcome out = f.execute();
    assertEquals(PurchaseSalesOrderResult.ORDER_CHANGED, out.result());
    assertEquals(MarketMutationState.UNCHANGED, out.mutationState());
    assertEquals(0, f.accounts.transfers);
    assertNotNull(f.repository.current);
  }

  @Test
  void insertionFailureRestoresOrder() {
    Fixture f = new Fixture();
    f.inventory.insertSuccess = false;
    PurchaseSalesOrderOutcome out = f.execute();
    assertEquals(PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED, out.result());
    assertEquals(MarketMutationState.UNCHANGED, out.mutationState());
    assertNotNull(f.repository.current);
    assertEquals(0, f.inventory.count);
  }

  @Test
  void paymentFailureIndependentlyRollsBackInventoryAndOrder() {
    Fixture f = new Fixture();
    f.accounts.transfer = BalanceTransferResult.PERSIST_FAILED;
    PurchaseSalesOrderOutcome out = f.execute();
    assertEquals(PurchaseSalesOrderResult.PAYMENT_FAILED, out.result());
    assertEquals(MarketMutationState.UNCHANGED, out.mutationState());
    assertEquals(0, f.inventory.count);
    assertNotNull(f.repository.current);
    assertEquals(1, f.inventory.rollbacks);
  }

  @Test
  void compensationFailureMarksMarketChangedAndReporterCannotChangeResult() {
    Fixture f = new Fixture();
    f.accounts.transfer = BalanceTransferResult.PERSIST_FAILED;
    f.repository.restoreSuccess = false;
    f.reporterThrows = true;
    PurchaseSalesOrderOutcome out = f.execute();
    assertEquals(PurchaseSalesOrderResult.ROLLBACK_FAILED, out.result());
    assertEquals(MarketMutationState.CHANGED, out.mutationState());
    assertNull(f.repository.current);
    assertEquals(0, f.inventory.count);
  }

  @Test
  void duplicatePurchaseCannotPayOrInsertTwice() {
    Fixture f = new Fixture();
    assertEquals(PurchaseSalesOrderResult.SUCCESS, f.execute().result());
    assertEquals(PurchaseSalesOrderResult.NOT_FOUND, f.execute().result());
    assertEquals(1, f.accounts.transfers);
    assertEquals(3, f.inventory.count);
  }

  @Test
  void partialPurchaseChargesOnlySliceAndKeepsRemainderOnSameTradeId() {
    Fixture f = new Fixture();
    f.repository.current = new MarketOrder(MarketOrderType.SALES, f.trade,
        MarketOrderCodecTest.item(), 64, 1_280, "seller", f.seller, 1, 2, false);
    f.accounts.buyer = 10_000;

    PurchaseSalesOrderOutcome out = f.execute(10);

    assertEquals(PurchaseSalesOrderResult.SUCCESS, out.result());
    assertEquals(10, out.purchasedOrder().orElseThrow().quantity());
    assertEquals(200, out.purchasedOrder().orElseThrow().totalPrice());
    assertEquals(10, f.inventory.count);
    assertEquals(9_800, f.accounts.buyer);
    assertEquals(200, f.accounts.seller);
    assertNotNull(f.repository.current);
    assertEquals(f.trade, f.repository.current.tradeId());
    assertEquals(54, f.repository.current.quantity());
    assertEquals(1_080, f.repository.current.totalPrice());
  }

  @Test
  void nonDivisibleLegacyOrderRejectsPartialButStillAllowsWholeFill() {
    Fixture partial = new Fixture();
    assertEquals(PurchaseSalesOrderResult.PARTIAL_FILL_UNSUPPORTED, partial.execute(1).result());
    assertNotNull(partial.repository.current);

    Fixture whole = new Fixture();
    assertEquals(PurchaseSalesOrderResult.SUCCESS, whole.execute().result());
    assertNull(whole.repository.current);
  }

  private static final class Fixture {
    final UUID buyer = UUID.randomUUID(), seller = UUID.randomUUID(), trade = UUID.randomUUID();
    final FakeInventory inventory = new FakeInventory();
    final FakeAccounts accounts = new FakeAccounts();
    final FakeRepository repository = new FakeRepository();
    boolean reporterThrows;

    Fixture() {
      repository.current = order(MarketOrderType.SALES, seller);
    }

    MarketOrder order(MarketOrderType type, UUID owner) {
      return new MarketOrder(
          type, trade, MarketOrderCodecTest.item(), 3, 10, "seller", owner, 1, 2, false);
    }

    PurchaseSalesOrderOutcome execute() {
      return execute(0);
    }

    PurchaseSalesOrderOutcome execute(int quantity) {
      return PurchaseSalesOrderService.execute(
          new PurchaseSalesOrderMessage(trade, quantity),
          new PurchaseSalesOrderService.Context(
              buyer,
              inventory,
              inventory,
              accounts,
              repository,
              (a, b, c, d, e, f, g, h) -> {
                if (reporterThrows) throw new IllegalStateException();
              }));
    }

    final class FakeInventory implements MarketItemMaterializer, TransactionalInventory {
      int count, rollbacks;
      boolean accept = true, restoreFails, insertSuccess = true;

      public UUID ownerId() {
        return buyer;
      }

      public Object restore(MarketOrder o) {
        if (restoreFails) throw new IllegalStateException();
        return new Object();
      }

      public boolean canAccept(Object t, int q) {
        return accept;
      }

      public InventoryInsertionResult insert(Object t, int q) {
        if (!insertSuccess) return InventoryInsertionResult.failure(true);
        int before = count;
        count += q;
        return InventoryInsertionResult.success(
            () -> {
              rollbacks++;
              count = before;
              return true;
            });
      }
    }

    final class FakeAccounts implements PurchaseSalesOrderService.Accounts {
      int buyer = 100, seller, transfers;
      BalanceTransferResult preview = BalanceTransferResult.SUCCESS,
          transfer = BalanceTransferResult.SUCCESS;

      public BalanceTransferResult preview(UUID s, int a) {
        return preview;
      }

      public BalanceTransferResult transfer(UUID s, int a) {
        transfers++;
        if (transfer == BalanceTransferResult.SUCCESS) {
          buyer -= a;
          seller += a;
        }
        return transfer;
      }
    }

    final class FakeRepository implements PurchaseSalesOrderService.Repository {
      MarketOrder current;
      MarketPartialFillStatus fillStatus;
      boolean restoreSuccess = true;

      public MarketOrder find(UUID id) {
        return current;
      }

      public MarketPartialFillTransition fillIfUnchanged(
          UUID id, MarketOrderType expectedType, MarketOrder expected, int quantity) {
        if (fillStatus != null) return MarketPartialFillTransition.failure(fillStatus);
        if (current == null) return MarketPartialFillTransition.failure(MarketPartialFillStatus.NOT_FOUND);
        if (current.type() != expectedType)
          return MarketPartialFillTransition.failure(MarketPartialFillStatus.WRONG_ORDER_TYPE);
        if (!current.equals(expected))
          return MarketPartialFillTransition.failure(MarketPartialFillStatus.ORDER_CHANGED);
        int amount = MarketOrderPricing.fillAmount(current, quantity);
        MarketOrder previous = current;
        MarketOrder remaining = quantity == previous.quantity() ? null
            : new MarketOrder(previous.type(), previous.tradeId(), previous.item(),
                previous.quantity() - quantity, previous.totalPrice() - amount,
                previous.sellerName(), previous.sellerId(), previous.listingTime(),
                previous.expirationTime(), false);
        current = remaining;
        return MarketPartialFillTransition.applied(previous, remaining, quantity, amount, () -> {
          if (!restoreSuccess) return MarketPartialFillRollbackResult.PERSIST_FAILED;
          current = previous;
          return MarketPartialFillRollbackResult.RESTORED;
        });
      }
    }
  }
}
