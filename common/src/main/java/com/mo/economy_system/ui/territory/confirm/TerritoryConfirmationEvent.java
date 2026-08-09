package com.mo.economy_system.ui.territory.confirm;

public sealed interface TerritoryConfirmationEvent permits TerritoryConfirmationEvent.ActionClicked {
  record ActionClicked(TerritoryConfirmationAction action) implements TerritoryConfirmationEvent {}
}
