package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketActionPostPlanTest {
  @Test
  void unchangedOnlySchedulesFeedback() {
    assertEquals(List.of("feedback"), stages(MarketMutationState.UNCHANGED, false, false));
  }

  @Test
  void changedAndUnknownScheduleBroadcastFirst() {
    assertEquals(
        List.of("broadcast", "feedback"), stages(MarketMutationState.CHANGED, false, false));
    assertEquals(
        List.of("broadcast", "feedback"), stages(MarketMutationState.UNKNOWN, false, false));
  }

  @Test
  void successfulCounterpartyActionSchedulesNoticeLast() {
    assertEquals(
        List.of("broadcast", "feedback", "notice"),
        stages(MarketMutationState.CHANGED, true, true));
    assertEquals(
        List.of("broadcast", "feedback"), stages(MarketMutationState.CHANGED, true, false));
    assertEquals(
        List.of("broadcast", "feedback"), stages(MarketMutationState.CHANGED, false, true));
  }

  @Test
  void planAndExecutorKeepRunningAfterFailures() {
    List<String> events = new ArrayList<>();
    var plan =
        MarketActionPostPlan.build(
            MarketMutationState.CHANGED,
            true,
            true,
            () -> {
              events.add("broadcast");
              throw new IllegalStateException();
            },
            () -> {
              events.add("feedback");
              throw new IllegalStateException();
            },
            () -> events.add("notice"));
    IsolatedPostActions.runAll(plan, (stage, failure) -> events.add(stage + "-failed"));
    assertEquals(
        List.of("broadcast", "broadcast-failed", "feedback", "feedback-failed", "notice"), events);
  }

  private static List<String> stages(MarketMutationState state, boolean success, boolean notice) {
    return MarketActionPostPlan.build(state, success, notice, () -> {}, () -> {}, () -> {}).stream()
        .map(IsolatedPostActions.NamedAction::stage)
        .toList();
  }
}
