package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import java.util.*;
import com.mo.economy_system.platform.nbt.NbtData;
import org.junit.jupiter.api.Test;

class CreateDemandOrderServiceTest {
  @Test
  void successFreezesTotalOnceWithoutTaxOrQuantityMultiplication() {
    Fixture f = new Fixture();
    assertEquals(CreateDemandOrderResult.SUCCESS, f.execute());
    assertEquals(900, f.account.balance);
    assertEquals(1, f.account.debits);
    assertEquals(0, f.account.credits);
    assertEquals(1, f.repository.orders.size());
    MarketOrder order = f.repository.orders.get(0);
    assertEquals(MarketOrderType.DEMAND, order.type());
    assertEquals(5, order.quantity());
    assertEquals(100, order.totalPrice());
    assertEquals(1, order.item().count());
    assertFalse(order.delivered());
    assertEquals(f.buyer, order.sellerId());
    assertEquals("buyer", order.sellerName());
    assertEquals(10, order.listingTime());
    assertEquals(10 + MarketOrder.EXPIRATION_DURATION_MILLIS, order.expirationTime());
  }

  @Test
  void allPreconditionsLeaveBalanceAndRepositoryUntouched() {
    List<CreateDemandOrderResult> expected =
        List.of(
            CreateDemandOrderResult.INVALID_ITEM_ID,
            CreateDemandOrderResult.INVALID_QUANTITY,
            CreateDemandOrderResult.INVALID_PRICE,
            CreateDemandOrderResult.ITEM_NOT_FOUND,
            CreateDemandOrderResult.QUANTITY_EXCEEDS_LIMIT,
            CreateDemandOrderResult.REPOSITORY_FULL,
            CreateDemandOrderResult.INSUFFICIENT_FUNDS,
            CreateDemandOrderResult.TIME_OVERFLOW,
            CreateDemandOrderResult.ID_GENERATION_FAILED);
    List<Fixture> fixtures = new ArrayList<>();
    Fixture badId = new Fixture();
    badId.message = new CreateDemandOrderMessage(" ", 5, 100);
    fixtures.add(badId);
    Fixture badQty = new Fixture();
    badQty.message = new CreateDemandOrderMessage("minecraft:stone", 0, 100);
    fixtures.add(badQty);
    Fixture badPrice = new Fixture();
    badPrice.message = new CreateDemandOrderMessage("minecraft:stone", 5, 0);
    fixtures.add(badPrice);
    Fixture missing = new Fixture();
    missing.resolve = DemandItemResolveResult.failure(DemandItemResolveResult.Error.ITEM_NOT_FOUND);
    fixtures.add(missing);
    Fixture tooMany = new Fixture();
    tooMany.message = new CreateDemandOrderMessage("minecraft:stone", 65, 100);
    fixtures.add(tooMany);
    Fixture full = new Fixture();
    full.repository.full = true;
    fixtures.add(full);
    Fixture poor = new Fixture();
    poor.account.balance = 99;
    fixtures.add(poor);
    Fixture overflow = new Fixture();
    overflow.now = Long.MAX_VALUE;
    fixtures.add(overflow);
    Fixture noId = new Fixture();
    noId.id = null;
    fixtures.add(noId);
    for (int i = 0; i < fixtures.size(); i++) {
      Fixture f = fixtures.get(i);
      assertEquals(expected.get(i), f.execute());
      assertEquals(i == 6 ? 99 : 1000, f.account.balance);
      assertTrue(f.repository.orders.isEmpty());
      assertEquals(0, f.account.debits);
    }
  }

  @Test
  void knownDebitFailureIsRejectedAndUnknownDebitStateIsSurfaced() {
    Fixture rejected = new Fixture();
    rejected.account.debitFalse = true;
    assertEquals(CreateDemandOrderResult.PAYMENT_FAILED, rejected.execute());
    assertState(rejected, 1000, 1, 0);

    Fixture thrown = new Fixture();
    thrown.account.debitThrows = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, thrown.execute());
    assertState(thrown, 1000, 1, 0);

    Fixture returnedNull = new Fixture();
    returnedNull.account.debitNull = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, returnedNull.execute());
    assertState(returnedNull, 1000, 1, 0);

    Fixture mutatedThenThrew = new Fixture();
    mutatedThenThrew.account.debitMutatesThenThrows = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, mutatedThenThrew.execute());
    assertState(mutatedThenThrew, 900, 1, 0);
  }

  @Test
  void repositoryFailureRefundsExactlyOnceAndLeavesNoOrder() {
    Fixture rejected = new Fixture();
    rejected.repository.addFalse = true;
    assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED, rejected.execute());
    assertState(rejected, 1000, 1, 1);
    Fixture thrown = new Fixture();
    thrown.repository.addThrows = true;
    assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED, thrown.execute());
    assertState(thrown, 1000, 1, 1);
  }

  @Test
  void knownRefundFailureIsExplicitAndUnknownRefundStateIsSurfaced() {
    Fixture rejected = new Fixture();
    rejected.repository.addFalse = true;
    rejected.account.creditFalse = true;
    assertEquals(CreateDemandOrderResult.REFUND_FAILED, rejected.execute());
    assertState(rejected, 900, 1, 1);

    Fixture thrown = new Fixture();
    thrown.repository.addFalse = true;
    thrown.account.creditThrows = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, thrown.execute());
    assertState(thrown, 900, 1, 1);

    Fixture returnedNull = new Fixture();
    returnedNull.repository.addFalse = true;
    returnedNull.account.creditNull = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, returnedNull.execute());
    assertState(returnedNull, 900, 1, 1);

    Fixture mutatedThenThrew = new Fixture();
    mutatedThenThrew.repository.addFalse = true;
    mutatedThenThrew.account.creditMutatesThenThrows = true;
    assertEquals(CreateDemandOrderResult.STATE_UNKNOWN, mutatedThenThrew.execute());
    assertState(mutatedThenThrew, 1000, 1, 1);
  }

  @Test
  void duplicateIdUsesAtomicRepositoryContractAndRefunds() {
    Fixture f = new Fixture();
    f.repository.orders.add(
        new MarketOrder(
            MarketOrderType.SALES, f.id, item(), 1, 1, "x", UUID.randomUUID(), 1, 2, false));
    assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED, f.execute());
    assertEquals(1000, f.account.balance);
    assertEquals(1, f.repository.orders.size());
  }

  private static void assertState(Fixture f, int balance, int debits, int credits) {
    assertEquals(balance, f.account.balance);
    assertEquals(debits, f.account.debits);
    assertEquals(credits, f.account.credits);
    assertTrue(f.repository.orders.isEmpty());
  }

  private static final class Fixture {
    UUID buyer = UUID.randomUUID(), id = UUID.randomUUID();
    long now = 10;
    CreateDemandOrderMessage message = new CreateDemandOrderMessage("minecraft:stone", 5, 100);
    DemandItemResolveResult resolve =
        DemandItemResolveResult.success(new ResolvedDemandItem("minecraft:stone", item(), 64));
    FakeAccount account = new FakeAccount();
    FakeRepository repository = new FakeRepository();

    CreateDemandOrderResult execute() {
      return CreateDemandOrderService.execute(
          message,
          new CreateDemandOrderService.Context(
              raw -> resolve,
              account,
              repository,
              buyer,
              "buyer",
              () -> id,
              () -> now,
              CreateDemandOrderService.FailureReporter.noop()));
    }
  }

  private static final class FakeAccount implements CreateDemandOrderService.Account {
    int balance = 1000, debits, credits;
    boolean debitFalse, debitThrows, debitNull, debitMutatesThenThrows;
    boolean creditFalse, creditThrows, creditNull, creditMutatesThenThrows;

    public boolean canDebit(int amount) {
      return balance >= amount;
    }

    public BalanceMutationResult debitExact(int amount) {
      debits++;
      if (debitMutatesThenThrows) {
        balance -= amount;
        throw new IllegalStateException();
      }
      if (debitThrows) throw new IllegalStateException();
      if (debitNull) return null;
      if (debitFalse) return BalanceMutationResult.PERSIST_FAILED;
      balance -= amount;
      return BalanceMutationResult.SUCCESS;
    }

    public BalanceMutationResult creditExact(int amount) {
      credits++;
      if (creditMutatesThenThrows) {
        balance += amount;
        throw new IllegalStateException();
      }
      if (creditThrows) throw new IllegalStateException();
      if (creditNull) return null;
      if (creditFalse) return BalanceMutationResult.PERSIST_FAILED;
      balance += amount;
      return BalanceMutationResult.SUCCESS;
    }
  }

  private static final class FakeRepository implements CreateDemandOrderService.Repository {
    List<MarketOrder> orders = new ArrayList<>();
    boolean full, addFalse, addThrows;

    public boolean isFull() {
      return full;
    }

    public boolean add(MarketOrder order) {
      if (addThrows) throw new IllegalStateException();
      if (addFalse
          || orders.stream().anyMatch(existing -> existing.tradeId().equals(order.tradeId())))
        return false;
      orders.add(order);
      return true;
    }
  }

  private static ItemStackSnapshot item() {
    return ItemStackSnapshot.create(
            "minecraft:stone",
            1,
            Optional.empty(),
            List.of(),
            Map.of(),
            Map.of(),
            true,
            true,
            0,
            0,
            false,
            true,
            OptionalInt.empty(),
            true,
            OptionalInt.empty(),
            NbtData.emptyCompound())
        .orElseThrow();
  }
}
