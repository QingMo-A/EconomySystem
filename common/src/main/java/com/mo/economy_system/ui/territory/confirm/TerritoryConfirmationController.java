package com.mo.economy_system.ui.territory.confirm;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Set;

/** Common decision state machine for destructive territory operations. */
public final class TerritoryConfirmationController extends AbstractEconomyScreenController<TerritoryConfirmationState, TerritoryConfirmationEvent> {
  private final TerritoryConfirmationPort port;
  public TerritoryConfirmationController(TerritoryConfirmationKind kind, java.util.UUID territoryId, String territoryName,
                                         java.util.UUID memberId, String memberName, TerritoryConfirmationPort port) {
    super(new TerritoryConfirmationState(kind, territoryId, territoryName, memberId, memberName, ScreenState.READY,
        Set.of(TerritoryConfirmationAction.CONFIRM, TerritoryConfirmationAction.CANCEL)));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }
  @Override public void handle(TerritoryConfirmationEvent event) {
    if (!(event instanceof TerritoryConfirmationEvent.ActionClicked value) || value.action() == null || !state().can(value.action())) return;
    replaceState(new TerritoryConfirmationState(state().kind(), state().territoryId(), state().territoryName(),
        state().memberId(), state().memberName(), state().screenState(), Set.of()));
    if (value.action() == TerritoryConfirmationAction.CONFIRM) {
      if (state().kind() == TerritoryConfirmationKind.REMOVE_TERRITORY) port.removeTerritory(state().territoryId());
      else port.removeMember(state().territoryId(), state().memberId());
    }
    navigate(new UiNavigation.Back());
  }
}
