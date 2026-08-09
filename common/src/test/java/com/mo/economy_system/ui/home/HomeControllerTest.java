package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeControllerTest {
  @Test
  void loadsBothSourcesAndRejectsStaleResponses() {
    FakePort port = new FakePort();
    HomeController controller = new HomeController("alice", port);

    controller.handle(new HomeEvent.Initialize(100L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1L, port.requestId);
    controller.handle(new HomeEvent.MarketLoaded(2L, 1L, 2, 3));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new HomeEvent.BalanceLoaded(2L, 1L, 99,
        List.of(new AccountBalance("wrong", 1))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new HomeEvent.BalanceLoaded(1L, 1L, 42,
        List.of(new AccountBalance("alice", 42), new AccountBalance("bob", 7))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new HomeEvent.MarketLoaded(1L, 1L, 2, 3));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(42, controller.state().balance());
    assertEquals(2, controller.state().sellOrders());
    assertEquals(3, controller.state().demandOrders());
  }

  @Test
  void timesOutRetriesPagesAndNavigates() {
    FakePort port = new FakePort();
    HomeController controller = new HomeController("alice", port);
    controller.handle(new HomeEvent.Initialize(10L));
    controller.handle(new HomeEvent.Tick(10L + HomeController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.home.sync_timeout", controller.state().errorKey());

    controller.handle(new HomeEvent.Retry(20L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(2L, port.requestId);
    controller.handle(new HomeEvent.BalanceLoaded(2L, 2L, 1, accounts(13)));
    controller.handle(new HomeEvent.MarketLoaded(2L, 2L, 0, 0));
    controller.handle(new HomeEvent.ViewportChanged(5));
    controller.handle(new HomeEvent.Scroll(1));
    assertEquals(5, controller.state().leaderboardOffset());
    controller.handle(new HomeEvent.ActionClicked(EconomyUiRoute.MARKET));
    assertEquals(EconomyUiRoute.MARKET,
        assertInstanceOf(UiNavigation.Route.class, controller.pollNavigation().orElseThrow()).route());
  }

  private static List<AccountBalance> accounts(int count) {
    List<AccountBalance> values = new ArrayList<>();
    for (int i = 0; i < count; i++) values.add(new AccountBalance("player" + i, i));
    return values;
  }

  private static final class FakePort implements HomePort {
    private long next;
    private long requestId = -1;
    @Override public long nextRequestId() { return ++next; }
    @Override public void requestBalance(long id) { requestId = id; }
    @Override public void requestMarketSummary(long id) { requestId = id; }
  }
}
