package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
import java.util.UUID;

/** Target adapter for market requests and order intents. */
public interface MarketPort {
  long nextRequestId();

  void requestPage(long requestId, int offset, MarketOrderFilter filter, String query);

  /** v2 server-side sort request. Legacy adapters may inherit DEFAULT behavior temporarily. */
  default void requestPage(
      long requestId, int offset, MarketOrderFilter filter, MarketOrderSort sort, String query) {
    requestPage(requestId, offset, filter, query);
  }

  /**
   * Silent live refresh may ask the server to return the page containing an already selected trade.
   * Adapters that do not support focus yet can safely fall back to the requested page.
   */
  default void requestPage(
      long requestId,
      int offset,
      MarketOrderFilter filter,
      MarketOrderSort sort,
      String query,
      UUID focusTradeId) {
    requestPage(requestId, offset, filter, sort, query);
  }

  void submit(MarketAction action, MarketRow row);

  /** Opens the common confirmation flow before the mutation is submitted. */
  default void confirm(MarketAction action, MarketRow row) {
    submit(action, row);
  }

  void create(MarketAction action);
}
