package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomyLedger;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.mo.economy_system.platform.nbt.NbtData;
import org.junit.jupiter.api.Test;

class MarketExpirationServiceTest {
  @Test
  void expiresAtTheExactDeadlineAndRefundsUndeliveredDemandExactly() {
    MarketOrder order = order(MarketOrderType.DEMAND, false, 40L);
    MarketLedger market = ledger(order);
    Accounts accounts = new Accounts();
    Delivery delivery = new Delivery(true);

    List<MarketExpirationOutcome> outcomes =
        MarketExpirationService.expire(40L, context(market, accounts, delivery));

    assertEquals(MarketExpirationResult.REFUNDED, outcomes.get(0).result());
    assertEquals(37, accounts.credited);
    assertTrue(market.orders().isEmpty());
    assertFalse(delivery.called);
  }

  @Test
  void returnsSalesAndDeliveredDemandToTheDeliveryPort() {
    MarketOrder sales = order(MarketOrderType.SALES, false, 40L);
    MarketOrder deliveredDemand = order(MarketOrderType.DEMAND, true, 40L);
    MarketLedger market = ledger(sales, deliveredDemand);
    Accounts accounts = new Accounts();
    Delivery delivery = new Delivery(true);

    List<MarketExpirationOutcome> outcomes =
        MarketExpirationService.expire(40L, context(market, accounts, delivery));

    assertEquals(2, outcomes.size());
    assertTrue(outcomes.stream().allMatch(MarketExpirationOutcome::succeeded));
    assertEquals(2, delivery.calls.size());
    assertEquals(0, accounts.credited);
    assertTrue(market.orders().isEmpty());
  }

  @Test
  void deliveryFailureRestoresTheRemovedOrderForRetry() {
    MarketOrder sales = order(MarketOrderType.SALES, false, 40L);
    MarketLedger market = ledger(sales);

    List<MarketExpirationOutcome> outcomes =
        MarketExpirationService.expire(40L, context(market, new Accounts(), new Delivery(false)));

    assertEquals(MarketExpirationResult.DELIVERY_FAILED, outcomes.get(0).result());
    assertEquals(List.of(sales), market.orders());
  }

  @Test
  void exactCreditFailureRestoresDemandInsteadOfSaturatingTheBalance() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false, 40L);
    MarketLedger market = ledger(demand);
    EconomyLedger balances = new EconomyLedger(() -> {});
    balances.setBalance(demand.sellerId(), EconomyLedger.MAX_BALANCE);
    MarketExpirationService.Context context = new MarketExpirationService.Context(
        new LedgerRepository(market),
        balances::creditExact,
        (owner, item, quantity, source) -> true,
        MarketExpirationService.FailureReporter.noop());

    List<MarketExpirationOutcome> outcomes = MarketExpirationService.expire(40L, context);

    assertEquals(MarketExpirationResult.CREDIT_FAILED, outcomes.get(0).result());
    assertEquals(EconomyLedger.MAX_BALANCE, balances.getBalance(demand.sellerId()));
    assertEquals(List.of(demand), market.orders());
  }

  @Test
  void staleRemovalDoesNotIssueAnyCompensation() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false, 40L);
    Accounts accounts = new Accounts();
    Delivery delivery = new Delivery(true);
    MarketExpirationService.Context context = new MarketExpirationService.Context(
        new MarketExpirationService.Repository() {
          public List<MarketOrder> orders() {
            return List.of(demand);
          }

          public MarketOrderRemovalResult removeIfUnchanged(MarketOrder expected) {
            return MarketOrderRemovalResult.failure(MarketOrderRemovalStatus.ORDER_CHANGED);
          }
        },
        accounts,
        delivery,
        MarketExpirationService.FailureReporter.noop());

    List<MarketExpirationOutcome> outcomes = MarketExpirationService.expire(40L, context);

    assertEquals(MarketExpirationResult.ORDER_CHANGED, outcomes.get(0).result());
    assertEquals(0, accounts.credited);
    assertFalse(delivery.called);
  }

  @Test
  void removalMutationThenExceptionDoesNotCompensateAnUnknownState() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false, 40L);
    MarketLedger market = ledger(demand);
    Accounts accounts = new Accounts();
    Delivery delivery = new Delivery(true);
    MarketExpirationService.Repository repository = new MarketExpirationService.Repository() {
      public List<MarketOrder> orders() {
        return market.orders();
      }

      public MarketOrderRemovalResult removeIfUnchanged(MarketOrder expected) {
        assertEquals(MarketOrderRemovalStatus.REMOVED,
            market.removeIfUnchanged(expected).status());
        throw new IllegalStateException("remove after mutation");
      }
    };

    List<MarketExpirationOutcome> outcomes = MarketExpirationService.expire(
        40L,
        new MarketExpirationService.Context(
            repository, accounts, delivery, MarketExpirationService.FailureReporter.noop()));

    assertEquals(MarketExpirationResult.STATE_UNKNOWN, outcomes.get(0).result());
    assertTrue(market.orders().isEmpty());
    assertEquals(0, accounts.credited);
    assertFalse(delivery.called);
  }

  @Test
  void creditMutationThenExceptionDoesNotRestoreTheRemovedDemand() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false, 40L);
    MarketLedger market = ledger(demand);
    Accounts accounts = new Accounts();
    accounts.throwAfterCredit = true;

    List<MarketExpirationOutcome> outcomes =
        MarketExpirationService.expire(40L, context(market, accounts, new Delivery(true)));

    assertEquals(MarketExpirationResult.STATE_UNKNOWN, outcomes.get(0).result());
    assertEquals(37, accounts.credited);
    assertTrue(market.orders().isEmpty());
  }

  @Test
  void deliveryMutationThenExceptionDoesNotRestoreTheRemovedOrder() {
    MarketOrder sales = order(MarketOrderType.SALES, false, 40L);
    MarketLedger market = ledger(sales);
    Delivery delivery = new Delivery(true);
    delivery.throwAfterEnqueue = true;

    List<MarketExpirationOutcome> outcomes =
        MarketExpirationService.expire(40L, context(market, new Accounts(), delivery));

    assertEquals(MarketExpirationResult.STATE_UNKNOWN, outcomes.get(0).result());
    assertEquals(1, delivery.calls.size());
    assertTrue(market.orders().isEmpty());
  }

  @Test
  void scheduleRunsOnlyAtTheSharedInterval() {
    assertTrue(MarketExpirationSchedule.shouldRun(0));
    assertTrue(MarketExpirationSchedule.shouldRun(MarketExpirationSchedule.INTERVAL_TICKS));
    assertFalse(MarketExpirationSchedule.shouldRun(1));
    assertFalse(MarketExpirationSchedule.shouldRun(-1));
  }

  private static MarketExpirationService.Context context(
      MarketLedger market, Accounts accounts, Delivery delivery) {
    return new MarketExpirationService.Context(
        new LedgerRepository(market), accounts, delivery, MarketExpirationService.FailureReporter.noop());
  }

  private static MarketLedger ledger(MarketOrder... orders) {
    MarketLedger ledger = new MarketLedger(() -> {});
    for (MarketOrder order : orders) assertTrue(ledger.add(order));
    return ledger;
  }

  private static MarketOrder order(MarketOrderType type, boolean delivered, long expirationTime) {
    return new MarketOrder(
        type,
        UUID.randomUUID(),
        item(),
        3,
        37,
        "owner",
        UUID.randomUUID(),
        10L,
        expirationTime,
        delivered);
  }

  private static ItemStackSnapshot item() {
    return ItemStackSnapshot.create(
            "minecraft:stone",
            1,
            java.util.Optional.empty(),
            List.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            true,
            true,
            0,
            0,
            false,
            true,
            java.util.OptionalInt.empty(),
            true,
            java.util.OptionalInt.empty(),
            NbtData.emptyCompound())
        .orElseThrow();
  }

  private record LedgerRepository(MarketLedger ledger) implements MarketExpirationService.Repository {
    public List<MarketOrder> orders() {
      return ledger.orders();
    }

    public MarketOrderRemovalResult removeIfUnchanged(MarketOrder expected) {
      return ledger.removeIfUnchanged(expected);
    }
  }

  private static final class Accounts implements MarketExpirationService.Accounts {
    private int credited;
    private boolean throwAfterCredit;

    public BalanceMutationResult credit(UUID ownerId, int amount, String category, String reason) {
      credited += amount;
      if (throwAfterCredit) throw new IllegalStateException("credit after mutation");
      return BalanceMutationResult.SUCCESS;
    }
  }

  private static final class Delivery implements MarketExpirationService.Delivery {
    private final boolean result;
    private final List<UUID> calls = new ArrayList<>();
    private boolean called;
    private boolean throwAfterEnqueue;

    private Delivery(boolean result) {
      this.result = result;
    }

    public boolean enqueue(UUID ownerId, ItemStackSnapshot item, int quantity, String source) {
      called = true;
      calls.add(ownerId);
      if (throwAfterEnqueue) throw new IllegalStateException("delivery after mutation");
      return result;
    }
  }
}
