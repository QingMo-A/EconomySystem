package com.mo.economy_system.ui.market;

/** Target adapter for inline market detail actions. */
public interface MarketDetailPort {
  void submit(MarketAction action, MarketRow row, int quantity);
}
