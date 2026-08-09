package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable, loader-neutral content and action state for check consent. */
public record CheckConsentState(
    String requesterName,
    String checkTypeId,
    ScreenState screenState,
    Set<CheckConsentAction> actions) {
  public CheckConsentState {
    requesterName = Objects.requireNonNullElse(requesterName, "");
    checkTypeId = Objects.requireNonNullElse(checkTypeId, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public boolean can(CheckConsentAction action) {
    return actions.contains(action);
  }
}
