package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;

/** Platform requests used by the shared home controller. */
public interface HomePort {
  long nextRequestId();
  void requestBalance(long requestId);
  void requestMarketSummary(long requestId);
  default void open(EconomyUiRoute route) {}
}
