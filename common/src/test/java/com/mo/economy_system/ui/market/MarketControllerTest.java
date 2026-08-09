package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketControllerTest {
  @Test
  void networkPagingUsesProtocolPageSizeWithoutSkippingOrders() {
    FakePort port = new FakePort();
    MarketController controller = new MarketController(MarketTestFixtures.VIEWER, port);

    assertEquals(MarketController.NETWORK_PAGE_SIZE, controller.state().pageSize());
    controller.handle(new MarketEvent.Initialize(10));
    assertEquals(new Request(0, 0, MarketOrderFilter.ALL, ""), port.requests.get(0));
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new MarketEvent.DataLoaded(0, 1, 0, 20, 10, 10,
        MarketTestFixtures.orders(9)));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(3, controller.state().totalPages());

    controller.handle(new MarketEvent.NextPage());
    assertEquals(new Request(1, 9, MarketOrderFilter.ALL, ""), port.requests.get(1));
    controller.handle(new MarketEvent.DataLoaded(1, 1, 9, 20, 10, 10,
        MarketTestFixtures.orders(9)));
    assertEquals(1, controller.state().page());

    controller.handle(new MarketEvent.Scroll(1));
    assertEquals(new Request(2, 18, MarketOrderFilter.ALL, ""), port.requests.get(2));
    controller.handle(new MarketEvent.DataLoaded(2, 1, 18, 20, 10, 10,
        MarketTestFixtures.orders(2)));
    assertEquals(2, controller.state().page());

    controller.handle(new MarketEvent.PreviousPage());
    assertEquals(new Request(3, 9, MarketOrderFilter.ALL, ""), port.requests.get(3));
  }

  @Test
  void staleDuplicateFailureTimeoutAndRetryAreDeterministic() {
    FakePort port = new FakePort();
    MarketController controller = new MarketController(MarketTestFixtures.VIEWER, port);
    controller.handle(new MarketEvent.Initialize(100));

    controller.handle(new MarketEvent.DataLoaded(99, 1, 0, 9, 9, 0,
        MarketTestFixtures.orders(9)));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new MarketEvent.DataFailed(99, "ignored"));
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new MarketEvent.Tick(100 + MarketController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.market.sync_timeout", controller.state().errorKey());
    controller.handle(new MarketEvent.DataLoaded(0, 1, 0, 9, 9, 0,
        MarketTestFixtures.orders(9)));
    assertEquals(ScreenState.ERROR, controller.state().screenState());

    controller.handle(new MarketEvent.Retry(200));
    assertEquals(new Request(1, 0, MarketOrderFilter.ALL, ""), port.requests.get(1));
    controller.handle(new MarketEvent.DataLoaded(1, 2, 0, 0, 0, 0, List.of()));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
    controller.handle(new MarketEvent.DataLoaded(1, 3, 0, 9, 9, 0,
        MarketTestFixtures.orders(9)));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
  }

  @Test
  void filteringActionsAndNavigationRemainInCommonController() {
    FakePort port = new FakePort();
    MarketController controller = new MarketController(MarketTestFixtures.VIEWER, port);
    controller.handle(new MarketEvent.Initialize(0));
    var rows = List.of(
        MarketTestFixtures.order(0, MarketOrderType.SALES, MarketTestFixtures.VIEWER, false),
        MarketTestFixtures.order(1, MarketOrderType.SALES, new java.util.UUID(4, 1), false),
        MarketTestFixtures.order(2, MarketOrderType.DEMAND, new java.util.UUID(4, 2), false),
        MarketTestFixtures.order(3, MarketOrderType.DEMAND, MarketTestFixtures.VIEWER, true),
        MarketTestFixtures.order(4, MarketOrderType.DEMAND, MarketTestFixtures.VIEWER, false),
        MarketTestFixtures.order(5, MarketOrderType.DEMAND, new java.util.UUID(4, 3), true));
    controller.handle(new MarketEvent.DataLoaded(0, 1, 0, rows.size(), 2, 4, rows));

    controller.handle(new MarketEvent.ActionClicked(MarketAction.BUY, rows.get(0).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.BUY, rows.get(1).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.REMOVE_SALES, rows.get(0).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.DELIVER_DEMAND, rows.get(2).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.CONFIRM_DEMAND, rows.get(3).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.REMOVE_DEMAND, rows.get(4).tradeId()));
    controller.handle(new MarketEvent.ActionClicked(MarketAction.DELIVER_DEMAND, rows.get(5).tradeId()));
    assertEquals(List.of(MarketAction.BUY, MarketAction.REMOVE_SALES, MarketAction.DELIVER_DEMAND,
        MarketAction.CONFIRM_DEMAND, MarketAction.REMOVE_DEMAND), port.confirmed);

    controller.handle(new MarketEvent.FilterChanged(MarketOrderFilter.DEMAND));
    assertEquals(new Request(1, 0, MarketOrderFilter.DEMAND, ""), port.requests.get(1));
    controller.handle(new MarketEvent.QueryChanged("  stone  "));
    assertEquals(new Request(2, 0, MarketOrderFilter.DEMAND, "stone"), port.requests.get(2));

    controller.handle(new MarketEvent.ActionClicked(MarketAction.CREATE_SALES, null));
    assertEquals(List.of(MarketAction.CREATE_SALES), port.created);
    controller.handle(new MarketEvent.ActionClicked(MarketAction.BACK, null));
    UiNavigation.Route route = assertInstanceOf(UiNavigation.Route.class,
        controller.pollNavigation().orElseThrow());
    assertEquals(EconomyUiRoute.HOME, route.route());
  }

  private static final class FakePort implements MarketPort {
    private long nextId;
    private final List<Request> requests = new ArrayList<>();
    private final List<MarketAction> confirmed = new ArrayList<>();
    private final List<MarketAction> created = new ArrayList<>();

    @Override public long nextRequestId() { return nextId++; }
    @Override public void requestPage(long requestId, int offset, MarketOrderFilter filter, String query) {
      requests.add(new Request(requestId, offset, filter, query));
    }
    @Override public void submit(MarketAction action, MarketRow row) {}
    @Override public void confirm(MarketAction action, MarketRow row) { confirmed.add(action); }
    @Override public void create(MarketAction action) { created.add(action); }
  }

  private record Request(long requestId, int offset, MarketOrderFilter filter, String query) {}
}
