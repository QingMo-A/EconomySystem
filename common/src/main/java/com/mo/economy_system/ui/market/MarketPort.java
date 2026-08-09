package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderFilter;

/** Target adapter for market requests and order intents. */
public interface MarketPort {
  long nextRequestId();

  void requestPage(long requestId, int offset, MarketOrderFilter filter, String query);

  void submit(MarketAction action, MarketRow row);

  /** Opens the common confirmation flow before the mutation is submitted. */
  default void confirm(MarketAction action, MarketRow row) {
    submit(action, row);
  }

  void create(MarketAction action);
}
