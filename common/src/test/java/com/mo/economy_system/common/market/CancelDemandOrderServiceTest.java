package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancelDemandOrderServiceTest {
  @Test
  void ownerAndOperatorRefundOriginalOwnerExactlyOnce() {
    Fixture owner = new Fixture();
    assertEquals(CancelDemandOrderResult.SUCCESS, owner.execute().result());
    assertEquals(117, owner.account.balance);
    assertEquals(1, owner.account.credits);
    assertEquals(CancelDemandOrderResult.NOT_FOUND, owner.execute().result());

    Fixture operator = new Fixture();
    operator.actor = UUID.randomUUID();
    operator.operator = true;
    assertEquals(CancelDemandOrderResult.SUCCESS, operator.execute().result());
    assertEquals(operator.owner, operator.account.lastOwner);
  }

  @Test
  void partiallyFilledDemandRefundsOnlyRemainingEscrowValue() {
    Fixture fixture = new Fixture();
    fixture.repository.order = new MarketOrder(
        MarketOrderType.DEMAND,
        UUID.randomUUID(),
        MarketOrderCodecTest.item(),
        54,
        1_080,
        "buyer",
        fixture.owner,
        1,
        2,
        false);

    assertEquals(CancelDemandOrderResult.SUCCESS, fixture.execute().result());
    assertEquals(1_180, fixture.account.balance);
    assertEquals(1, fixture.account.credits);
  }

  @Test
  void validationNeverMutates() {
    Fixture fixture = new Fixture();
    fixture.actor = UUID.randomUUID();
    assertEquals(CancelDemandOrderResult.NOT_OWNER, fixture.execute().result());
    assertNotNull(fixture.repository.order);

    fixture = new Fixture();
    fixture.repository.order = fixture.order(MarketOrderType.DEMAND, true);
    assertEquals(CancelDemandOrderResult.ALREADY_DELIVERED, fixture.execute().result());

    fixture = new Fixture();
    fixture.repository.order = fixture.order(MarketOrderType.SALES, false);
    assertEquals(CancelDemandOrderResult.WRONG_ORDER_TYPE, fixture.execute().result());
  }

  @Test
  void removalStatesAreExplicit() {
    Fixture fixture = new Fixture();
    fixture.repository.status = DemandOrderRemovalStatus.ORDER_CHANGED;
    assertEquals(MarketMutationState.CHANGED, fixture.execute().mutationState());

    fixture = new Fixture();
    fixture.repository.status = DemandOrderRemovalStatus.PERSIST_FAILED;
    assertEquals(MarketMutationState.UNCHANGED, fixture.execute().mutationState());
  }

  @Test
  void refundFailureRestoresOrder() {
    Fixture fixture = new Fixture();
    fixture.account.creditResult = BalanceMutationResult.PERSIST_FAILED;

    CancelDemandOrderOutcome outcome = fixture.execute();

    assertEquals(CancelDemandOrderResult.REFUND_FAILED, outcome.result());
    assertEquals(MarketMutationState.UNCHANGED, outcome.mutationState());
    assertNotNull(fixture.repository.order);
  }

  @Test
  void rollbackFailureMarksMarketChangedForInvalidation() {
    Fixture fixture = new Fixture();
    fixture.account.creditResult = BalanceMutationResult.PERSIST_FAILED;
    fixture.repository.restoreResult = MarketOrderRestoreResult.PERSIST_FAILED;

    CancelDemandOrderOutcome outcome = fixture.execute();
    var plan =
        MarketActionPostPlan.build(
            outcome.mutationState(), true, false, () -> {}, () -> {}, () -> {});

    assertEquals(CancelDemandOrderResult.ROLLBACK_FAILED, outcome.result());
    assertEquals(MarketMutationState.CHANGED, outcome.mutationState());
    assertEquals("broadcast", plan.get(0).stage());
  }

  private static final class Fixture {
    private final UUID owner = UUID.randomUUID();
    private UUID actor = owner;
    private boolean operator;
    private final Account account = new Account();
    private final Repository repository = new Repository();

    private Fixture() {
      repository.order = order(MarketOrderType.DEMAND, false);
    }

    private MarketOrder order(MarketOrderType type, boolean delivered) {
      return new MarketOrder(
          type,
          UUID.randomUUID(),
          MarketOrderCodecTest.item(),
          2,
          17,
          "buyer",
          owner,
          1,
          2,
          delivered);
    }

    private CancelDemandOrderOutcome execute() {
      UUID tradeId =
          repository.order == null ? UUID.randomUUID() : repository.order.tradeId();
      return CancelDemandOrderService.execute(
          new RemoveDemandOrderMessage(tradeId),
          new CancelDemandOrderService.Context(
              actor,
              operator,
              account,
              repository,
              CancelDemandOrderService.FailureReporter.noop()));
    }
  }

  private static final class Account implements CancelDemandOrderService.Account {
    private int balance = 100;
    private int credits;
    private UUID lastOwner;
    private BalanceMutationResult previewResult = BalanceMutationResult.SUCCESS;
    private BalanceMutationResult creditResult = BalanceMutationResult.SUCCESS;

    @Override
    public BalanceMutationResult previewCreditExact(UUID owner, int amount) {
      return previewResult;
    }

    @Override
    public BalanceMutationResult creditExact(UUID owner, int amount) {
      credits++;
      lastOwner = owner;
      if (creditResult == BalanceMutationResult.SUCCESS) balance += amount;
      return creditResult;
    }
  }

  private static final class Repository implements CancelDemandOrderService.Repository {
    private MarketOrder order;
    private DemandOrderRemovalStatus status = DemandOrderRemovalStatus.REMOVED;
    private MarketOrderRestoreResult restoreResult = MarketOrderRestoreResult.RESTORED;

    @Override
    public MarketOrder find(UUID id) {
      return order;
    }

    @Override
    public DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(
        UUID id, MarketOrder expected) {
      if (status != DemandOrderRemovalStatus.REMOVED) {
        return DemandOrderRemovalResult.failure(status);
      }
      MarketOrder removed = order;
      order = null;
      return DemandOrderRemovalResult.removed(
          new MarketOrderRemoval(
              removed,
              () -> {
                if (restoreResult == MarketOrderRestoreResult.RESTORED) order = removed;
                return restoreResult;
              }));
    }
  }
}
