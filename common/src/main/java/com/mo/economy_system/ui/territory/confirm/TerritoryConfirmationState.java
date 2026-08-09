package com.mo.economy_system.ui.territory.confirm;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Common confirmation state for territory and member removal. */
public record TerritoryConfirmationState(TerritoryConfirmationKind kind, UUID territoryId, String territoryName,
                                         UUID memberId, String memberName, ScreenState screenState,
                                         Set<TerritoryConfirmationAction> actions) {
  public TerritoryConfirmationState {
    kind = Objects.requireNonNull(kind, "kind"); territoryId = Objects.requireNonNull(territoryId, "territoryId");
    territoryName = Objects.requireNonNullElse(territoryName, ""); memberName = Objects.requireNonNullElse(memberName, "");
    if (kind == TerritoryConfirmationKind.REMOVE_MEMBER && memberId == null) throw new IllegalArgumentException("member id required");
    screenState = Objects.requireNonNull(screenState, "screenState"); actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }
  public boolean can(TerritoryConfirmationAction action) { return actions.contains(action); }
}
