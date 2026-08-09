package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopControllerTest {
  @Test
  void requestFilteringPagingActionsAndNavigationStayInCommon() {
    FakePort port = new FakePort();
    ShopController controller = new ShopController(port);
    assertEquals(ScreenState.IDLE, controller.state().screenState());

    controller.handle(new ShopEvent.Initialize(10));
    assertEquals(List.of(0L), port.requests);
    controller.handle(new ShopEvent.DataLoaded(99, ShopTestFixtures.items(2)));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new ShopEvent.DataLoaded(0, ShopTestFixtures.items(20)));
    assertEquals(ScreenState.READY, controller.state().screenState());

    controller.handle(new ShopEvent.ViewportChanged(5));
    assertEquals(4, controller.state().totalPages());
    controller.handle(new ShopEvent.NextPage());
    assertEquals(1, controller.state().page());
    controller.handle(new ShopEvent.Scroll(1));
    assertEquals(2, controller.state().page());
    controller.handle(new ShopEvent.PreviousPage());
    assertEquals(1, controller.state().page());

    controller.handle(new ShopEvent.FilterChanged("shop-19"));
    assertEquals(0, controller.state().page());
    assertEquals(1, controller.state().filteredRows().size());
    controller.handle(new ShopEvent.ActionClicked(ShopAction.BUY, "shop-19"));
    controller.handle(new ShopEvent.ActionClicked(ShopAction.BUY, "missing"));
    assertEquals(List.of("shop-19"), port.confirmed);

    controller.handle(new ShopEvent.ActionClicked(ShopAction.BACK, null));
    UiNavigation.Route route = assertInstanceOf(UiNavigation.Route.class,
        controller.pollNavigation().orElseThrow());
    assertEquals(EconomyUiRoute.HOME, route.route());
  }

  @Test
  void failureTimeoutRetryEmptyAndDuplicateResponsesAreDeterministic() {
    FakePort port = new FakePort();
    ShopController controller = new ShopController(port);
    controller.handle(new ShopEvent.Initialize(100));
    controller.handle(new ShopEvent.DataFailed(99, "ignored"));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new ShopEvent.DataFailed(0, "screen.shop.sync_failed"));
    assertEquals(ScreenState.ERROR, controller.state().screenState());

    controller.handle(new ShopEvent.Retry(200));
    assertEquals(List.of(0L, 1L), port.requests);
    controller.handle(new ShopEvent.Tick(200 + ShopController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.sync_timeout", controller.state().errorKey());

    controller.handle(new ShopEvent.Retry(300));
    controller.handle(new ShopEvent.DataLoaded(2, List.of()));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
    controller.handle(new ShopEvent.DataLoaded(2, ShopTestFixtures.items(1)));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
  }

  private static final class FakePort implements ShopPort {
    private long nextId;
    private final List<Long> requests = new ArrayList<>();
    private final List<String> confirmed = new ArrayList<>();

    @Override public long nextRequestId() { return nextId++; }
    @Override public void requestCatalog(long requestId) { requests.add(requestId); }
    @Override public void submit(ShopAction action, ShopRow row, int quantity) {}
    @Override public void confirm(ShopRow row) { confirmed.add(row.item().shopItemId()); }
  }
}
