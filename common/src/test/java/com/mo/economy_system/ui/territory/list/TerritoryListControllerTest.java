package com.mo.economy_system.ui.territory.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryListControllerTest {
  private static final UUID OWNER = new UUID(0, 1);
  private static final UUID FIRST = new UUID(0, 2);

  @Test
  void loadsFiltersPagesAndRejectsStaleResponses() {
    FakePort port = new FakePort();
    TerritoryListController controller = new TerritoryListController(port);
    controller.handle(new TerritoryListEvent.Initialize(10));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1, port.requestId);
    controller.handle(new TerritoryListEvent.DataLoaded(2, List.of(owned("wrong")), List.of()));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new TerritoryListEvent.DataLoaded(1, List.of(owned("alpha")),
        List.of(summary(new UUID(0, 3), "beta"))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    controller.handle(new TerritoryListEvent.FilterChanged("beta"));
    assertEquals(1, controller.state().visibleRows().size());
    controller.handle(new TerritoryListEvent.DataFailed(1, "late"));
    assertEquals(ScreenState.READY, controller.state().screenState());
  }

  @Test
  void timeoutRetryAndActionsUsePortOrNavigation() {
    FakePort port = new FakePort();
    TerritoryListController controller = new TerritoryListController(port);
    controller.handle(new TerritoryListEvent.Initialize(100));
    controller.handle(new TerritoryListEvent.Tick(100 + TerritoryListController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    controller.handle(new TerritoryListEvent.Retry(200));
    assertEquals(2, port.requestId);
    controller.handle(new TerritoryListEvent.DataLoaded(2, List.of(owned("alpha")), List.of()));
    UUID id = controller.state().rows().get(0).summary().territoryId();
    controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.TELEPORT, id));
    assertEquals(TerritoryListAction.TELEPORT, port.action);
    controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.MANAGE, id));
    assertEquals(TerritoryListAction.MANAGE, port.action);
    controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.BACK, null));
    assertEquals(com.mo.economy_system.common.client.ui.EconomyUiRoute.HOME,
        assertInstanceOf(UiNavigation.Route.class, controller.pollNavigation().orElseThrow()).route());
    assertTrue(controller.pollNavigation().isEmpty());
  }

  private static Owned owned(String name) {
    return new Owned(summary(FIRST, name), List.of(new Member(new UUID(0, 4), "member")),
        Optional.empty(), Arrays.stream(RuleAction.values()).map(a -> new Rule(a, RuleLevel.OWNER_ONLY)).toList(),
        List.of());
  }

  private static Summary summary(UUID id, String name) {
    return new Summary(id, OWNER, "owner", name, new Position(0, 64, 0),
        new Position(10, 70, 10), "minecraft:overworld");
  }

  private static final class FakePort implements TerritoryListPort {
    private long requestId;
    private TerritoryListAction action;
    @Override public long nextRequestId() { return ++requestId; }
    @Override public void requestTerritories(long requestId) {}
    @Override public void submit(TerritoryListAction action, TerritoryListRow row) { this.action = action; }
  }
}
