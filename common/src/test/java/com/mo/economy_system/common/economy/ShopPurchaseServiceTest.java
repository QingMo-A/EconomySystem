package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mo.economy_system.common.market.InventoryInsertionResult;
import com.mo.economy_system.common.market.TransactionalInventory;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopPurchaseServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void successfulPurchaseDebitsThenDeliversAndRecordsStatistics() {
    FakeContext context = new FakeContext();

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 3), context.context());

    assertEquals(ShopPurchaseResult.SUCCESS, result);
    assertEquals(List.of("debit:30", "insert:3", "record:3"), context.operations);
    assertEquals(List.of("message.shop.buy_successfully"), context.feedback.messages);
  }

  @Test
  void failedInsertionIsRefunded() {
    FakeContext context = new FakeContext();
    context.inventory.succeed = false;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 2), context.context());

    assertEquals(ShopPurchaseResult.DELIVERY_FAILED, result);
    assertEquals(List.of("debit:20", "insert:2", "credit:20"), context.operations);
  }

  @Test
  void failedInventoryRollbackDoesNotRefundAnUncertainDelivery() {
    FakeContext context = new FakeContext();
    context.inventory.succeed = false;
    context.inventory.failureRestored = false;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 2), context.context());

    assertEquals(ShopPurchaseResult.ROLLBACK_FAILED, result);
    assertEquals(List.of("debit:20", "insert:2"), context.operations);
    assertFalse(context.operations.stream().anyMatch(value -> value.startsWith("credit:")));
  }

  @Test
  void persistenceDebitFailureIsNotReportedAsInsufficientFunds() {
    FakeContext context = new FakeContext();
    context.debit = BalanceMutationResult.PERSIST_FAILED;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 2), context.context());

    assertEquals(ShopPurchaseResult.PAYMENT_FAILED, result);
    assertEquals(List.of("debit:20"), context.operations);
  }

  @Test
  void refundFailureIsExplicitAfterInventoryWasRestored() {
    FakeContext context = new FakeContext();
    context.inventory.succeed = false;
    context.refund = BalanceMutationResult.PERSIST_FAILED;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 2), context.context());

    assertEquals(ShopPurchaseResult.REFUND_FAILED, result);
    assertEquals(List.of("debit:20", "insert:2", "credit:20"), context.operations);
  }

  @Test
  void thrownInventoryMutationKeepsPaymentBecauseDeliveryStateIsUnknown() {
    FakeContext context = new FakeContext();
    context.inventory.throwOnInsert = true;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 2), context.context());

    assertEquals(ShopPurchaseResult.STATE_UNKNOWN, result);
    assertEquals(List.of("debit:20", "insert:2"), context.operations);
  }

  @Test
  void feedbackFailureCannotChangeACompletedPurchase() {
    FakeContext context = new FakeContext();
    context.feedback.throwOnSend = true;

    ShopPurchaseResult result = ShopPurchaseService.execute(
        new ShopBuyItemMessage("shop-entry", 1), context.context());

    assertEquals(ShopPurchaseResult.SUCCESS, result);
    assertEquals(List.of("debit:10", "insert:1", "record:1"), context.operations);
  }

  private static final class FakeContext {
    private final List<String> operations = new ArrayList<>();
    private final FakeInventory inventory = new FakeInventory(operations);
    private final FakeFeedback feedback = new FakeFeedback();
    private BalanceMutationResult debit = BalanceMutationResult.SUCCESS;
    private BalanceMutationResult refund = BalanceMutationResult.SUCCESS;

    private ShopPurchaseService.Context context() {
      return new ShopPurchaseService.Context(
          PLAYER,
          new ShopPurchaseService.Catalog() {
            @Override
            public ShopItemSnapshot find(String shopItemId) {
              return "shop-entry".equals(shopItemId)
                  ? new ShopItemSnapshot("shop-entry", "minecraft:stone", 10, 10, 10,
                      "stone", 1.0D, "", "", 0, 0, 0)
                  : null;
            }

            @Override
            public ShopPurchaseService.MaterializedItem materialize(ShopItemSnapshot item) {
              return new ShopPurchaseService.MaterializedItem("stone", "Stone");
            }

            @Override
            public boolean recordPurchase(String shopItemId, int quantity) {
              operations.add("record:" + quantity);
              return true;
            }
          },
          inventory,
          new ShopPurchaseService.Accounts() {
            @Override
            public BalanceMutationResult debit(UUID playerId, int amount, String category, String reason) {
              operations.add("debit:" + amount);
              return debit;
            }

            @Override
            public BalanceMutationResult credit(UUID playerId, int amount, String category, String reason) {
              operations.add("credit:" + amount);
              return refund;
            }
          },
          feedback);
    }
  }

  private static final class FakeInventory implements TransactionalInventory {
    private final List<String> operations;
    private boolean succeed = true;
    private boolean failureRestored = true;
    private boolean throwOnInsert;

    private FakeInventory(List<String> operations) {
      this.operations = operations;
    }

    @Override
    public UUID ownerId() {
      return PLAYER;
    }

    @Override
    public boolean canAccept(Object template, int quantity) {
      return true;
    }

    @Override
    public InventoryInsertionResult insert(Object template, int quantity) {
      operations.add("insert:" + quantity);
      if (throwOnInsert) throw new IllegalStateException("insert");
      return succeed ? InventoryInsertionResult.success(() -> true)
          : InventoryInsertionResult.failure(failureRestored);
    }
  }

  private static final class FakeFeedback implements ShopPurchaseService.Feedback {
    private final List<String> messages = new ArrayList<>();
    private boolean throwOnSend;

    @Override
    public void send(UUID playerId, String translationKey, Object... arguments) {
      if (throwOnSend) throw new IllegalStateException("feedback");
      messages.add(translationKey);
    }
  }
}
