package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Set;

/** Shared confirm/cancel semantics for buy, remove and demand-order actions. */
public final class MarketConfirmController
    extends AbstractEconomyScreenController<MarketConfirmState, MarketConfirmEvent> {
  private final MarketConfirmPort port;

  public MarketConfirmController(MarketAction action, MarketRow row, MarketConfirmPort port) {
    super(new MarketConfirmState(action, row, ScreenState.READY, null,
        Set.of(MarketConfirmAction.CONFIRM, MarketConfirmAction.CANCEL)));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(MarketConfirmEvent event) {
    if (!(event instanceof MarketConfirmEvent.ActionClicked value) || value.action() == null
        || !state().can(value.action())) return;
    if (value.action() == MarketConfirmAction.CANCEL) {
      replaceState(new MarketConfirmState(state().action(), state().row(), state().screenState(),
          state().errorKey(), Set.of()));
      navigate(new UiNavigation.Back());
      return;
    }
    replaceState(new MarketConfirmState(state().action(), state().row(), state().screenState(),
        state().errorKey(), Set.of()));
    port.submit(state().action(), state().row());
    navigate(new UiNavigation.Back());
  }
}
