package com.mo.economy_system.ui.territory.buff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritoryBuffCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuffManageControllerTest {
  private static final UUID TERRITORY = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void requestsDataAndRejectsStaleResponses() {
    FakePort port = new FakePort(BuffManageTestFixtures.resources(10, 10));
    BuffManageController controller = controller(port, List.of(
        BuffManageTestFixtures.buff("speed", false, 0, 3, 1, 1, 2)));

    controller.handle(new BuffManageEvent.Initialize(1_000L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1L, port.requestId);
    controller.handle(new BuffManageEvent.DataLoaded(2L, List.of(
        BuffManageTestFixtures.buff("stale", true, 1, 2, 0, 0, 0))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new BuffManageEvent.DataLoaded(1L, List.of(
        BuffManageTestFixtures.buff("speed", true, 1, 3, 2, 1, 2))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    controller.handle(new BuffManageEvent.DataFailed(1L, "duplicate"));
    assertEquals(ScreenState.READY, controller.state().screenState());
  }

  @Test
  void filteringPagingTimeoutAndRetryAreCommonBehavior() {
    FakePort port = new FakePort(BuffManageTestFixtures.resources(10, 10));
    List<Buff> buffs = List.of(
        BuffManageTestFixtures.buff("a", true, 1, 3, 0, 0, 0),
        BuffManageTestFixtures.buff("b", true, 1, 3, 0, 0, 0),
        BuffManageTestFixtures.buff("c", true, 1, 3, 0, 0, 0));
    BuffManageController controller = controller(port, buffs);
    controller.handle(new BuffManageEvent.Initialize(1_000L));
    controller.handle(new BuffManageEvent.DataLoaded(1L, buffs));
    controller.handle(new BuffManageEvent.ViewportChanged(2));
    controller.handle(new BuffManageEvent.NextPage());
    assertEquals(1, controller.state().page());
    assertEquals(2, controller.state().scrollOffset());
    controller.handle(new BuffManageEvent.Scroll(-1));
    assertEquals(0, controller.state().page());
    controller.handle(new BuffManageEvent.FilterChanged("c name"));
    assertEquals(1, controller.state().visibleBuffs().size());

    controller.handle(new BuffManageEvent.Retry(2_000L));
    controller.handle(new BuffManageEvent.Tick(2_000L + BuffManageController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.territory.buff.sync_timeout", controller.state().errorKey());
    controller.handle(new BuffManageEvent.ActionClicked(BuffAction.RETRY, "", 3_000L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
  }

  @Test
  void localAvailabilityIsACommonActionGateAndBackIsNavigationIntent() {
    FakePort port = new FakePort(BuffManageTestFixtures.resources(0, 0));
    Buff buff = BuffManageTestFixtures.buff("costly", false, 0, 2, 3, 4, 5);
    BuffManageController controller = controller(port, List.of(buff));
    controller.handle(new BuffManageEvent.Initialize(1L));
    controller.handle(new BuffManageEvent.DataLoaded(1L, List.of(buff)));
    assertEquals(BuffAvailability.MISSING_ITEMS_AND_EXPERIENCE,
        controller.state().buffs().get(0).availability());
    controller.handle(new BuffManageEvent.ActionClicked(BuffAction.UNLOCK, "costly", 2L));
    assertEquals("message.territory.buff.requirements_missing", port.feedbackKey);
    assertTrue(port.submissions.isEmpty());
    controller.handle(new BuffManageEvent.ActionClicked(BuffAction.BACK, "", 3L));
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
  }

  private static BuffManageController controller(FakePort port, List<Buff> buffs) {
    return new BuffManageController(TERRITORY, "home", buffs, port);
  }

  private static final class FakePort implements BuffManagePort {
    private final BuffResourceSnapshot resources;
    private long requestId;
    private String feedbackKey;
    private final List<String> submissions = new ArrayList<>();

    private FakePort(BuffResourceSnapshot resources) { this.resources = resources; }
    @Override public long nextRequestId() { return ++requestId; }
    @Override public void request(UUID territoryId, long requestId) {}
    @Override public void submit(UUID territoryId, BuffAction action, String buffId) {
      submissions.add(action + ":" + buffId);
    }
    @Override public BuffResourceSnapshot inspect(TerritoryBuffCost cost) { return resources; }
    @Override public void feedback(String translationKey) { feedbackKey = translationKey; }
  }
}
