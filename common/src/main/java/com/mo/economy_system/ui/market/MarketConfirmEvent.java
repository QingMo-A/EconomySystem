package com.mo.economy_system.ui.market;

public sealed interface MarketConfirmEvent permits MarketConfirmEvent.ActionClicked {
  record ActionClicked(MarketConfirmAction action) implements MarketConfirmEvent {}
}
