package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;

/** Shared home request, paging, timeout, and navigation state machine. */
public final class HomeController extends AbstractEconomyScreenController<HomeState, HomeEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final HomePort port;
  private long startedAt;
  private boolean inFlight;
  private boolean balanceLoaded;
  private boolean marketLoaded;

  public HomeController(String playerName, HomePort port) {
    super(new HomeState(playerName, EconomyUiMenu.defaultEntries(), 0, List.of(), 0, 0,
        0, 1, ScreenState.IDLE, null, -1, -1, -1));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override public void handle(HomeEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof HomeEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof HomeEvent.Retry value) request(value.nowNanos());
    else if (event instanceof HomeEvent.BalanceLoaded value) balance(value);
    else if (event instanceof HomeEvent.MarketLoaded value) market(value);
    else if (event instanceof HomeEvent.DataFailed value) failed(value);
    else if (event instanceof HomeEvent.ActionClicked value) action(value.route());
    else if (event instanceof HomeEvent.Scroll value) scroll(Integer.signum(value.steps()));
    else if (event instanceof HomeEvent.ViewportChanged value) viewport(value.pageSize());
    else if (event instanceof HomeEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("home request id exhausted");
    startedAt = nowNanos;
    inFlight = true;
    balanceLoaded = false;
    marketLoaded = false;
    replace(copy(0, state().leaderboardPageSize(), ScreenState.LOADING, null, id,
        state().balanceRevision(), state().marketRevision(), state().balance(), state().accounts(),
        state().sellOrders(), state().demandOrders()));
    port.requestBalance(id);
    port.requestMarketSummary(id);
  }

  private void balance(HomeEvent.BalanceLoaded event) {
    if (!inFlight || event.requestId() != state().requestId()
        || event.revision() < state().balanceRevision()) return;
    balanceLoaded = true;
    replace(copy(state().leaderboardOffset(), state().leaderboardPageSize(), readyState(), null,
        state().requestId(), event.revision(), state().marketRevision(), event.balance(),
        event.accounts(), state().sellOrders(), state().demandOrders()));
  }

  private void market(HomeEvent.MarketLoaded event) {
    if (!inFlight || event.requestId() != state().requestId()
        || event.revision() < state().marketRevision()) return;
    marketLoaded = true;
    replace(copy(state().leaderboardOffset(), state().leaderboardPageSize(), readyState(), null,
        state().requestId(), state().balanceRevision(), event.revision(), state().balance(),
        state().accounts(), event.sellOrders(), event.demandOrders()));
  }

  private ScreenState readyState() {
    return balanceLoaded && marketLoaded ? ScreenState.READY : ScreenState.LOADING;
  }

  private void failed(HomeEvent.DataFailed event) {
    if (!inFlight || event.requestId() != state().requestId()) return;
    inFlight = false;
    replace(copy(state().leaderboardOffset(), state().leaderboardPageSize(), ScreenState.ERROR,
        event.errorKey() == null || event.errorKey().isBlank()
            ? "screen.home.sync_failed" : event.errorKey(), -1, state().balanceRevision(),
        state().marketRevision(), state().balance(), state().accounts(), state().sellOrders(),
        state().demandOrders()));
  }

  private void action(EconomyUiRoute route) {
    // Dashboard data failures must not lock the navigation shell. Cards may show retry/error,
    // while every route remains reachable just as it is in the legacy Home screen.
    if (route == null) return;
    if (route == EconomyUiRoute.HOME) return;
    navigate(new UiNavigation.Route(route));
  }

  private void scroll(int delta) {
    if (delta == 0) return;
    int offset = Math.max(0, Math.min(state().maxOffset(),
        state().leaderboardOffset() + delta));
    replace(copy(offset, state().leaderboardPageSize(),
        state().screenState(), state().errorKey(), state().requestId(), state().balanceRevision(),
        state().marketRevision(), state().balance(), state().accounts(), state().sellOrders(),
        state().demandOrders()));
  }

  private void viewport(int pageSize) {
    int size = Math.max(1, pageSize);
    int offset = Math.min(state().leaderboardOffset(),
        Math.max(0, state().accounts().size() - HomeLayout.LEADERBOARD_VISIBLE_ROWS));
    replace(copy(offset, size, state().screenState(), state().errorKey(), state().requestId(),
        state().balanceRevision(), state().marketRevision(), state().balance(), state().accounts(),
        state().sellOrders(), state().demandOrders()));
  }

  private void tick(long nowNanos) {
    if (inFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      inFlight = false;
      replace(copy(state().leaderboardOffset(), state().leaderboardPageSize(), ScreenState.ERROR,
          "screen.home.sync_timeout", -1, state().balanceRevision(), state().marketRevision(),
          state().balance(), state().accounts(), state().sellOrders(), state().demandOrders()));
    } else if (inFlight && balanceLoaded && marketLoaded) {
      inFlight = false;
      replace(copy(state().leaderboardOffset(), state().leaderboardPageSize(), ScreenState.READY,
          null, -1, state().balanceRevision(), state().marketRevision(), state().balance(),
          state().accounts(), state().sellOrders(), state().demandOrders()));
    }
  }

  private HomeState copy(int offset, int pageSize, ScreenState screenState, String errorKey,
                         long requestId, long balanceRevision, long marketRevision, int balance,
                         List<AccountBalance> accounts, int sellOrders, int demandOrders) {
    return new HomeState(state().playerName(), state().entries(), Math.max(0, balance), accounts,
        sellOrders, demandOrders, Math.max(0, offset), Math.max(1, pageSize), screenState,
        errorKey, requestId, balanceRevision, marketRevision);
  }

  private void replace(HomeState value) { replaceState(value); }
}
