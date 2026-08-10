package com.mo.economy_system.ui.market;

/** Semantic actions exposed by the shared market list. */
public enum MarketAction {
  BUY,
  REMOVE_SALES,
  /** Operator-only removal of another player's sales order. */
  ADMIN_REMOVE_SALES,
  DELIVER_DEMAND,
  CONFIRM_DEMAND,
  REMOVE_DEMAND,
  CREATE_SALES,
  CREATE_DEMAND,
  RETRY,
  BACK
}
