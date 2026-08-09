package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import java.util.List;

public sealed interface HomeEvent permits HomeEvent.Initialize, HomeEvent.BalanceLoaded,
    HomeEvent.MarketLoaded, HomeEvent.DataFailed, HomeEvent.ActionClicked,
    HomeEvent.Scroll, HomeEvent.ViewportChanged, HomeEvent.Retry, HomeEvent.Tick {
  record Initialize(long nowNanos) implements HomeEvent {}
  record BalanceLoaded(long requestId, long revision, int balance,
                       List<AccountBalance> accounts) implements HomeEvent {
    public BalanceLoaded { accounts = List.copyOf(accounts); }
  }
  record MarketLoaded(long requestId, long revision, int sellOrders,
                      int demandOrders) implements HomeEvent {}
  record DataFailed(long requestId, String errorKey) implements HomeEvent {}
  record ActionClicked(EconomyUiRoute route) implements HomeEvent {}
  record Scroll(int steps) implements HomeEvent {}
  record ViewportChanged(int pageSize) implements HomeEvent {
    public ViewportChanged { if (pageSize < 1) throw new IllegalArgumentException("pageSize"); }
  }
  record Retry(long nowNanos) implements HomeEvent {}
  record Tick(long nowNanos) implements HomeEvent {}
}
