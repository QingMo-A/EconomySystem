package com.mo.economy_system.ui.market;

/** Target adapter for the final, user-confirmed market mutation. */
public interface MarketConfirmPort {
  void submit(MarketAction action, MarketRow row);
}
