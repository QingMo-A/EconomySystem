package com.mo.economy_system.ui.territory.detail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryDetailControllerTest {
  @Test
  void requestsBothSnapshotsRejectsStaleResponsesAndTimesOut() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);

    controller.handle(new TerritoryDetailEvent.Initialize(1_000L));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1L, port.lastRequestId);
    assertEquals(1, port.playerRequests);
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(2L,
        TerritoryDetailTestFixtures.territory(List.of())));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(1L,
        TerritoryDetailTestFixtures.territory(List.of(
            new Member(TerritoryDetailTestFixtures.ALICE, "alice")))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    controller.handle(new TerritoryDetailEvent.TerritoryFailed(1L, "duplicate"));
    assertEquals(ScreenState.READY, controller.state().screenState());

    controller.handle(new TerritoryDetailEvent.Retry(2_000L));
    controller.handle(new TerritoryDetailEvent.Tick(
        2_000L + TerritoryDetailController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.territory.detail.sync_timeout", controller.state().errorKey());
  }

  @Test
  void filtersPagesAndRejectsStalePlayerDirectoryRevisions() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    ready(controller);
    controller.handle(new TerritoryDetailEvent.PlayersLoaded(4L, List.of(
        new PlayerSummary(TerritoryDetailTestFixtures.ALICE, "alice"),
        new PlayerSummary(TerritoryDetailTestFixtures.BOB, "bob"))));
    controller.handle(new TerritoryDetailEvent.PlayersLoaded(3L, List.of()));
    assertEquals(2, controller.state().players().size());
    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.ACCESS));
    controller.handle(new TerritoryDetailEvent.ViewportChanged(1));
    controller.handle(new TerritoryDetailEvent.Scroll(1));
    assertEquals(1, controller.state().scroll());
    controller.handle(new TerritoryDetailEvent.FilterChanged("alice"));
    assertEquals(0, controller.state().scroll());
    assertEquals(List.of("alice"), controller.state().visibleAccessRows().stream()
        .map(TerritoryAccessRow::playerName).toList());
  }

  @Test
  void administrationActionsAreValidatedRefreshedAndExposedAsNavigationIntents() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    ready(controller);
    controller.handle(new TerritoryDetailEvent.PlayersLoaded(1L, List.of(
        new PlayerSummary(TerritoryDetailTestFixtures.ALICE, "alice"),
        new PlayerSummary(TerritoryDetailTestFixtures.BOB, "bob"))));

    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.ACCESS));
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TOGGLE_ACCESS,
        TerritoryDetailTestFixtures.ALICE, 2_000L));
    assertEquals("access:" + TerritoryDetailTestFixtures.ALICE + ":true", port.submissions.get(0));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(2L, port.lastRequestId);
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(2L,
        TerritoryDetailTestFixtures.territory(List.of(
            new Member(TerritoryDetailTestFixtures.ALICE, "alice")))));

    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.RULES));
    controller.handle(new TerritoryDetailEvent.RuleClicked(RuleAction.PLACE_BLOCK, 3_000L));
    assertTrue(port.submissions.contains("rule:PLACE_BLOCK:MEMBERS"));
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(3L,
        TerritoryDetailTestFixtures.territory(List.of())));

    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.TRANSFER));
    controller.handle(new TerritoryDetailEvent.ActionClicked(
        TerritoryDetailAction.TRANSFER_OWNERSHIP, TerritoryDetailTestFixtures.BOB));
    assertTrue(port.submissions.contains("transfer:" + TerritoryDetailTestFixtures.BOB));
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());

    TerritoryDetailController navigation = controller(new FakePort());
    ready(navigation);
    navigation.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BUFFS, null));
    UiNavigation.Target target = assertInstanceOf(UiNavigation.Target.class,
        navigation.pollNavigation().orElseThrow());
    assertEquals("territory-buffs", target.targetId());
    navigation.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.ACCESS));
    navigation.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BACK, null));
    assertEquals(TerritoryDetailViewKind.MAIN, navigation.state().view());
    assertFalse(navigation.pollNavigation().isPresent());
  }

  private static TerritoryDetailController controller(FakePort port) {
    return new TerritoryDetailController(TerritoryDetailTestFixtures.territory(List.of()), port);
  }

  private static void ready(TerritoryDetailController controller) {
    controller.handle(new TerritoryDetailEvent.Initialize(1L));
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(1L,
        TerritoryDetailTestFixtures.territory(List.of())));
  }

  private static final class FakePort implements TerritoryDetailPort {
    private long nextId;
    private long lastRequestId = -1;
    private int playerRequests;
    private final List<String> submissions = new ArrayList<>();
    @Override public long nextRequestId() { return ++nextId; }
    @Override public void requestTerritory(UUID territoryId, long requestId) {
      lastRequestId = requestId;
    }
    @Override public void requestPlayers() { playerRequests++; }
    @Override public void resize(UUID territoryId) { submissions.add("resize"); }
    @Override public void submitAccess(UUID territoryId, UUID playerId, boolean allowed) {
      submissions.add("access:" + playerId + ":" + allowed);
    }
    @Override public void submitRule(UUID territoryId, RuleAction action, RuleLevel level) {
      submissions.add("rule:" + action + ":" + level);
    }
    @Override public void submitTransfer(UUID territoryId, UUID playerId) {
      submissions.add("transfer:" + playerId);
    }
  }
}
