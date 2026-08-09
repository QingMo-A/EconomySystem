package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryControllerTest {
  @Test
  void rejectsStaleResponsesFiltersPagesAndClaimsWithCurrentRequest() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port);
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    controller.handle(new DeliveryEvent.Initialize(10));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1, port.lastRequest);

    controller.handle(new DeliveryEvent.DataLoaded(0, List.of(DeliveryBoxTestFixtures.entry(first, 1))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    List<DeliveryBoxEntrySnapshot> entries = List.of(
        DeliveryBoxTestFixtures.entry(first, 1), DeliveryBoxTestFixtures.entry(second, 2));
    controller.handle(new DeliveryEvent.DataLoaded(1, entries));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(1, controller.state().requestId());
    assertEquals(2, controller.state().totalPages());

    controller.handle(new DeliveryEvent.FilterChanged(second.toString().substring(0, 8)));
    assertEquals(1, controller.state().filteredRows().size());
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.CLAIM, second, 20));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(second, port.claimed);
    assertEquals(1, port.claimRequest);
    controller.handle(new DeliveryEvent.DataLoaded(1, List.of()));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
  }

  @Test
  void timesOutRetriesAndNavigatesHome() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port);
    controller.handle(new DeliveryEvent.Initialize(100));
    controller.handle(new DeliveryEvent.Tick(100 + DeliveryController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.RETRY, null, 200));
    assertEquals(2, port.lastRequest);
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.BACK, null, 0));
    UiNavigation.Route route = (UiNavigation.Route) controller.pollNavigation().orElseThrow();
    assertEquals(EconomyUiRoute.HOME, route.route());
  }

  private static final class FakePort implements DeliveryPort {
    private long next;
    private long lastRequest = -1;
    private long claimRequest = -1;
    private UUID claimed;
    @Override public long nextRequestId() { return ++next; }
    @Override public void requestData(long requestId) { lastRequest = requestId; }
    @Override public void claim(UUID entryId, long requestId) { claimed = entryId; claimRequest = requestId; }
  }
}
