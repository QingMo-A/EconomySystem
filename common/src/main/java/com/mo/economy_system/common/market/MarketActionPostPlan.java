package com.mo.economy_system.common.market;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MarketActionPostPlan {
  private MarketActionPostPlan() {}

  public static List<IsolatedPostActions.NamedAction> build(
      MarketMutationState mutationState,
      boolean success,
      boolean notifyCounterparty,
      Runnable broadcast,
      Runnable feedback,
      Runnable notice) {
    Objects.requireNonNull(mutationState, "mutationState");
    Objects.requireNonNull(broadcast, "broadcast");
    Objects.requireNonNull(feedback, "feedback");
    Objects.requireNonNull(notice, "notice");
    List<IsolatedPostActions.NamedAction> actions = new ArrayList<>(3);
    if (mutationState.requiresInvalidation()) {
      actions.add(new IsolatedPostActions.NamedAction("broadcast", broadcast));
    }
    actions.add(new IsolatedPostActions.NamedAction("feedback", feedback));
    if (success && notifyCounterparty) {
      actions.add(new IsolatedPostActions.NamedAction("notice", notice));
    }
    return List.copyOf(actions);
  }
}
