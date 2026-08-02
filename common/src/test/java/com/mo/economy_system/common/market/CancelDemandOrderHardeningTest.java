package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancelDemandOrderHardeningTest {
  @Test void mismatchedRemovedOrderIsRestoredWithoutRefund() {
    Fixture fixture = new Fixture();
    fixture.actual = fixture.changedQuantity();
    CancelDemandOrderOutcome outcome = fixture.execute();
    assertEquals(CancelDemandOrderResult.ORDER_CHANGED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertEquals(fixture.actual, outcome.transactionOrder().orElseThrow());
    assertEquals(1, fixture.restoreCalls);
    assertEquals(0, fixture.creditCalls);
    assertEquals("repository-contract", fixture.failures.get(0).stage());
  }

  @Test void mismatchedOrderRestoreFailureIsChanged() {
    Fixture fixture = new Fixture();
    fixture.actual = fixture.changedQuantity();
    fixture.restoreResult = MarketOrderRestoreResult.PERSIST_FAILED;
    CancelDemandOrderOutcome outcome = fixture.execute();
    assertEquals(CancelDemandOrderResult.ROLLBACK_FAILED, outcome.result());
    assertEquals(MarketMutationState.CHANGED, outcome.mutationState());
    assertEquals(0, fixture.creditCalls);
  }

  @Test void nullOrExceptionalRestoreIsUnknown() {
    Fixture nullRestore = new Fixture();
    nullRestore.creditResult = null;
    nullRestore.restoreResult = null;
    assertEquals(MarketMutationState.UNKNOWN, nullRestore.execute().mutationState());
    Fixture thrownRestore = new Fixture();
    thrownRestore.creditResult = BalanceMutationResult.PERSIST_FAILED;
    thrownRestore.restoreThrows = true;
    CancelDemandOrderOutcome outcome = thrownRestore.execute();
    assertEquals(MarketMutationState.UNKNOWN, outcome.mutationState());
    assertNotNull(thrownRestore.failures.get(0).restoreError());
  }

  @Test void reporterExceptionDoesNotChangeOutcomeAndRefundUsesRequesterTotalOnce() {
    Fixture fixture = new Fixture();
    fixture.reporterThrows = true;
    CancelDemandOrderOutcome success = fixture.execute();
    assertEquals(CancelDemandOrderResult.SUCCESS, success.result());
    assertEquals(fixture.expected.sellerId(), fixture.creditedOwner);
    assertEquals(fixture.expected.totalPrice(), fixture.creditedAmount);
    assertEquals(1, fixture.creditCalls);
    assertEquals(CancelDemandOrderResult.NOT_FOUND, fixture.execute().result());
    assertEquals(1, fixture.creditCalls);
  }

  private static final class Fixture
      implements CancelDemandOrderService.Account, CancelDemandOrderService.Repository {
    final UUID actor = UUID.randomUUID();
    final List<CancelDemandOrderFailure> failures = new ArrayList<>();
    MarketOrder expected = order(2);
    MarketOrder actual = expected;
    BalanceMutationResult creditResult = BalanceMutationResult.SUCCESS;
    MarketOrderRestoreResult restoreResult = MarketOrderRestoreResult.RESTORED;
    boolean restoreThrows;
    boolean reporterThrows;
    int creditCalls;
    int restoreCalls;
    UUID creditedOwner;
    int creditedAmount;
    boolean removed;

    CancelDemandOrderOutcome execute() {
      return CancelDemandOrderService.execute(
          new RemoveDemandOrderMessage(expected.tradeId()),
          new CancelDemandOrderService.Context(
              actor, true, this, this,
              failure -> {
                failures.add(failure);
                if (reporterThrows) throw new IllegalStateException("reporter");
              }));
    }

    public BalanceMutationResult previewCreditExact(UUID owner, int amount) {
      return BalanceMutationResult.SUCCESS;
    }

    public BalanceMutationResult creditExact(UUID owner, int amount) {
      creditCalls++;
      creditedOwner = owner;
      creditedAmount = amount;
      return creditResult;
    }

    public MarketOrder find(UUID id) { return removed ? null : expected; }

    public DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(
        UUID id, MarketOrder ignored) {
      removed = true;
      return DemandOrderRemovalResult.removed(
          new MarketOrderRemoval(
              actual,
              () -> {
                restoreCalls++;
                if (restoreThrows) throw new IllegalStateException("restore");
                if (restoreResult == MarketOrderRestoreResult.RESTORED) removed = false;
                return restoreResult;
              }));
    }

    MarketOrder changedQuantity() {
      return new MarketOrder(
          expected.type(), expected.tradeId(), expected.item(), expected.quantity() + 1,
          expected.totalPrice(), expected.sellerName(), expected.sellerId(),
          expected.listingTime(), expected.expirationTime(), false);
    }

    private MarketOrder order(int quantity) {
      return new MarketOrder(
          MarketOrderType.DEMAND, UUID.randomUUID(), MarketOrderCodecTest.item(), quantity, 17,
          "requester", UUID.randomUUID(), 1, 2, false);
    }
  }
}
