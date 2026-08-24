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
  void memberPageContainsAuthorizedMembersOnlyAndFiltersThem() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    controller.handle(new TerritoryDetailEvent.Initialize(1L));
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(1L,
        TerritoryDetailTestFixtures.territory(List.of(
            new Member(TerritoryDetailTestFixtures.ALICE, "alice"),
            new Member(TerritoryDetailTestFixtures.BOB, "bob")))));
    controller.handle(new TerritoryDetailEvent.PlayersLoaded(4L, List.of(
        new PlayerSummary(TerritoryDetailTestFixtures.ALICE, "alice"),
        new PlayerSummary(TerritoryDetailTestFixtures.BOB, "bob"),
        new PlayerSummary(new UUID(0, 99), "outsider"))));

    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.ACCESS));
    assertEquals(List.of("alice", "bob"), controller.state().accessRows().stream()
        .map(TerritoryAccessRow::playerName).toList());
    controller.handle(new TerritoryDetailEvent.ViewportChanged(1));
    controller.handle(new TerritoryDetailEvent.Scroll(1));
    assertEquals(1, controller.state().scroll());
    controller.handle(new TerritoryDetailEvent.FilterChanged("alice"));
    assertEquals(0, controller.state().scroll());
    assertEquals(List.of("alice"), controller.state().visibleAccessRows().stream()
        .map(TerritoryAccessRow::playerName).toList());
    assertFalse(controller.state().accessRows().stream()
        .anyMatch(row -> row.playerName().equals("outsider")));
  }

  @Test
  void exactRuleSelectionAndPresetsSubmitAuthoritativeMutations() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    ready(controller);
    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.RULES));

    controller.handle(new TerritoryDetailEvent.RuleLevelClicked(
        RuleAction.PLACE_BLOCK, RuleLevel.MEMBERS, 2_000L));
    assertTrue(port.submissions.contains("rule:PLACE_BLOCK:MEMBERS"));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(2L,
        territoryWithRule(RuleAction.PLACE_BLOCK, RuleLevel.MEMBERS)));

    controller.handle(new TerritoryDetailEvent.PresetClicked(TerritoryRulePreset.OPEN, 3_000L));
    assertTrue(port.submissions.contains("rule:USE_ITEM:EVERYONE"));
    assertTrue(port.submissions.contains("rule:INTERACT_BLOCK:EVERYONE"));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
  }

  @Test
  void managementNavigationUsesOneShellAndKeepsExternalSubpagesExplicit() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    ready(controller);

    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.ACCESS, null));
    assertEquals(TerritoryDetailViewKind.ACCESS, controller.state().view());
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.SETTINGS, null));
    assertEquals(TerritoryDetailViewKind.SETTINGS, controller.state().view());
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TRANSFER, null));
    assertEquals(TerritoryDetailViewKind.TRANSFER, controller.state().view());
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BACK, null));
    assertEquals(TerritoryDetailViewKind.SETTINGS, controller.state().view());
    assertFalse(controller.pollNavigation().isPresent());

    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.BUFFS, null));
    UiNavigation.Target target = assertInstanceOf(UiNavigation.Target.class,
        controller.pollNavigation().orElseThrow());
    assertEquals("territory-buffs", target.targetId());

    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.OVERVIEW, null));
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.INVITE, null));
    target = assertInstanceOf(UiNavigation.Target.class, controller.pollNavigation().orElseThrow());
    assertEquals("territory-invite", target.targetId());
  }

  @Test
  void ownershipTransferSubmitsExactlyOnceAndCloses() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = new TerritoryDetailController(
        TerritoryDetailTestFixtures.territory(List.of()),
        List.of(new PlayerSummary(TerritoryDetailTestFixtures.BOB, "bob")), 1L, port);
    ready(controller);
    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.SETTINGS));
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.TRANSFER, null));

    controller.handle(new TerritoryDetailEvent.ActionClicked(
        TerritoryDetailAction.TRANSFER_OWNERSHIP, TerritoryDetailTestFixtures.BOB));
    controller.handle(new TerritoryDetailEvent.ActionClicked(
        TerritoryDetailAction.TRANSFER_OWNERSHIP, TerritoryDetailTestFixtures.BOB));

    assertEquals(List.of("transfer:" + TerritoryDetailTestFixtures.BOB),
        port.submissions.stream().filter(value -> value.startsWith("transfer:")).toList());
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
  }

  @Test
  void settingsExposeCopyResizeAndDeleteIntent() {
    FakePort port = new FakePort();
    TerritoryDetailController controller = controller(port);
    ready(controller);
    controller.handle(new TerritoryDetailEvent.ViewSelected(TerritoryDetailViewKind.SETTINGS));

    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.COPY_ID, null));
    assertEquals(TerritoryDetailTestFixtures.TERRITORY, port.copiedId);
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.RESIZE, null));
    assertTrue(port.submissions.contains("resize"));
    controller.handle(new TerritoryDetailEvent.ActionClicked(TerritoryDetailAction.DELETE, null));
    UiNavigation.Target target = assertInstanceOf(UiNavigation.Target.class,
        controller.pollNavigation().orElseThrow());
    assertEquals("territory-delete", target.targetId());
  }

  private static TerritoryDetailController controller(FakePort port) {
    return new TerritoryDetailController(TerritoryDetailTestFixtures.territory(List.of()), port);
  }

  private static void ready(TerritoryDetailController controller) {
    controller.handle(new TerritoryDetailEvent.Initialize(1L));
    controller.handle(new TerritoryDetailEvent.TerritoryLoaded(1L,
        TerritoryDetailTestFixtures.territory(List.of())));
  }

  private static Owned territoryWithRule(RuleAction action, RuleLevel level) {
    Owned source = TerritoryDetailTestFixtures.territory(List.of());
    return new Owned(source.summary(), source.authorizedMembers(), source.backpoint(),
        source.rules().stream()
            .map(rule -> rule.action() == action
                ? new com.mo.economy_system.common.territory.TerritorySnapshots.Rule(action, level)
                : rule)
            .toList(), source.buffs());
  }

  private static final class FakePort implements TerritoryDetailPort {
    private long nextId;
    private long lastRequestId = -1;
    private int playerRequests;
    private UUID copiedId;
    private final List<String> submissions = new ArrayList<>();

    @Override public long nextRequestId() { return ++nextId; }
    @Override public void requestTerritory(UUID territoryId, long requestId) { lastRequestId = requestId; }
    @Override public void requestPlayers() { playerRequests++; }
    @Override public void resize(UUID territoryId) { submissions.add("resize"); }
    @Override public void copyTerritoryId(UUID territoryId) { copiedId = territoryId; }
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
