package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancelDemandOrderModelInvariantTest {
  @Test void removalResultRejectsIllegalStatusHandleCombinations() {
    MarketOrderRemoval removal = removal(order(MarketOrderType.DEMAND, false));
    assertThrows(NullPointerException.class, () -> new DemandOrderRemovalResult(null, null));
    assertThrows(IllegalArgumentException.class,
        () -> new DemandOrderRemovalResult(DemandOrderRemovalStatus.REMOVED, null));
    assertThrows(IllegalArgumentException.class,
        () -> new DemandOrderRemovalResult(DemandOrderRemovalStatus.NOT_FOUND, removal));
    assertThrows(IllegalArgumentException.class,
        () -> DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.REMOVED));
    assertThrows(NullPointerException.class, () -> DemandOrderRemovalResult.removed(null));
    assertSame(removal, DemandOrderRemovalResult.removed(removal).removal());
    assertNull(DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.NOT_FOUND).removal());
  }

  @Test void outcomeFactoriesEnforceOrderAndMutationSemantics() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false);
    assertEquals(MarketMutationState.CHANGED,
        CancelDemandOrderOutcome.success(demand).mutationState());
    assertThrows(IllegalArgumentException.class,
        () -> CancelDemandOrderOutcome.success(order(MarketOrderType.SALES, false)));
    assertThrows(IllegalArgumentException.class,
        () -> CancelDemandOrderOutcome.success(order(MarketOrderType.DEMAND, true)));
    assertThrows(IllegalArgumentException.class,
        () -> CancelDemandOrderOutcome.validationFailure(CancelDemandOrderResult.SUCCESS));
    assertTrue(CancelDemandOrderOutcome.changedFailure(
        CancelDemandOrderResult.ORDER_CHANGED, null).transactionOrder().isEmpty());
    assertTrue(CancelDemandOrderOutcome.uncertainFailure(
        CancelDemandOrderResult.STATE_UNKNOWN).transactionOrder().isEmpty());
  }

  @Test void failureRequiresIdentityStageResultAndState() {
    UUID tradeId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    assertThrows(NullPointerException.class, () -> failure(null, actorId, "find"));
    assertThrows(NullPointerException.class, () -> failure(tradeId, null, "find"));
    assertThrows(NullPointerException.class, () -> failure(tradeId, actorId, null));
    assertThrows(IllegalArgumentException.class, () -> failure(tradeId, actorId, " "));
  }

  @Test void combinedErrorIncludesEachDistinctCauseOnce() {
    RuntimeException repository = new IllegalStateException("repository");
    RuntimeException refund = new IllegalStateException("refund");
    RuntimeException restore = new IllegalStateException("restore");
    CancelDemandOrderFailure failure = new CancelDemandOrderFailure(
        UUID.randomUUID(), UUID.randomUUID(), null, false, "order-restore",
        CancelDemandOrderResult.ROLLBACK_FAILED, MarketMutationState.UNKNOWN,
        DemandOrderRemovalStatus.REMOVED, null, null, repository, repository, refund, restore);
    assertSame(repository, failure.combinedError());
    assertArrayEquals(new Throwable[] {refund, restore}, repository.getSuppressed());
    assertSame(repository, failure.combinedError());
    assertEquals(2, repository.getSuppressed().length);
  }

  @Test void postPlanUsesRealSuccessAndAlwaysProvidesFeedback() {
    var success = MarketActionPostPlan.build(
        MarketMutationState.CHANGED, true, true, () -> {}, () -> {}, () -> {});
    assertEquals(java.util.List.of("broadcast", "feedback", "notice"),
        success.stream().map(IsolatedPostActions.NamedAction::stage).toList());
    var failure = MarketActionPostPlan.build(
        MarketMutationState.UNKNOWN, false, true, () -> {}, () -> {}, () -> {});
    assertEquals(java.util.List.of("broadcast", "feedback"),
        failure.stream().map(IsolatedPostActions.NamedAction::stage).toList());
    var unchanged = MarketActionPostPlan.build(
        MarketMutationState.UNCHANGED, false, true, () -> {}, () -> {}, () -> {});
    assertEquals(java.util.List.of("feedback"),
        unchanged.stream().map(IsolatedPostActions.NamedAction::stage).toList());
  }

  private static CancelDemandOrderFailure failure(UUID tradeId, UUID actorId, String stage) {
    return new CancelDemandOrderFailure(
        tradeId, actorId, null, false, stage, CancelDemandOrderResult.STATE_UNKNOWN,
        MarketMutationState.UNKNOWN, null, null, null, null, null, null, null);
  }

  private static MarketOrderRemoval removal(MarketOrder order) {
    return new MarketOrderRemoval(order, () -> MarketOrderRestoreResult.RESTORED);
  }

  private static MarketOrder order(MarketOrderType type, boolean delivered) {
    return new MarketOrder(type, UUID.randomUUID(), MarketOrderCodecTest.item(), 2, 17,
        "requester", UUID.randomUUID(), 1, 2, delivered);
  }
}
