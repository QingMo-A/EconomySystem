package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable confirmation state; no target Screen type is stored here. */
public record MarketConfirmState(MarketAction action, MarketRow row, ScreenState screenState,
                                 String errorKey, Set<MarketConfirmAction> actions) {
  public MarketConfirmState {
    action = Objects.requireNonNull(action, "action");
    row = Objects.requireNonNull(row, "row");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public boolean can(MarketConfirmAction value) { return actions.contains(value); }
}
