package com.mo.economy_system.ui.territory.detail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TerritoryDetailLayoutTest {
  @Test
  void managementCenterControlsStayInsideTheVirtualViewport() {
    List<PlayerSummary> players = IntStream.range(0, 12)
        .mapToObj(index -> new PlayerSummary(new java.util.UUID(0, 100 + index), "player" + index))
        .toList();
    var territory = TerritoryDetailTestFixtures.territory(List.of(
        new Member(TerritoryDetailTestFixtures.ALICE, "alice"),
        new Member(TerritoryDetailTestFixtures.BOB, "bob")));

    for (TerritoryDetailViewKind view : TerritoryDetailViewKind.values()) {
      TerritoryDetailState state = new TerritoryDetailState(
          territory, players, view, 0, 5, "", ScreenState.READY, null, -1, 1);
      for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
          {1920, 1080}, {180, 120}, {160, 90}, {640, 80}}) {
        TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(size[0], size[1], state);
        UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
        for (UiRect rect : List.of(layout.title(), layout.subtitle(), layout.navigationPanel(), layout.content(),
            layout.search(), layout.settingsTitle(), layout.rows(), layout.previousButton(), layout.pageText(), layout.nextButton(),
            layout.retryButton(), layout.backButton())) {
          assertTrue(viewport.contains(rect), view + " " + size[0] + "x" + size[1] + " " + rect);
        }
        assertFalse(layout.navigationPanel().overlaps(layout.content()));
        assertFalse(layout.previousButton().overlaps(layout.nextButton()));
        for (var nav : layout.navigationButtons()) {
          assertTrue(layout.navigationPanel().contains(nav.rect()));
        }
        for (var row : layout.accessCards()) {
          assertTrue(row.card().contains(row.head()));
          assertTrue(row.card().contains(row.name()));
          assertTrue(row.card().contains(row.status()));
          assertTrue(row.card().contains(row.actionButton()));
        }
        for (var row : layout.ruleCards()) {
          assertTrue(row.card().contains(row.name()));
          assertTrue(row.card().contains(row.description()));
          assertTrue(row.card().contains(row.actionButton()));
        }
        for (var row : layout.transferCards()) {
          assertTrue(row.card().contains(row.head()));
          assertTrue(row.card().contains(row.name()));
          assertTrue(row.card().contains(row.description()));
          assertTrue(row.card().contains(row.actionButton()));
        }
      }
    }
  }

  @Test
  void fiveDefaultRulesFitOnePageAndPagerStaysInsideContentWhenNeeded() {
    TerritoryDetailState rules = new TerritoryDetailState(
        TerritoryDetailTestFixtures.territory(List.of()), List.of(), TerritoryDetailViewKind.RULES,
        0, 5, "", ScreenState.READY, null, -1, 0);
    TerritoryDetailLayout.Layout rulesLayout = TerritoryDetailLayout.calculate(640, 360, rules);
    assertEquals(5, rulesLayout.pageSize());
    assertEquals(5, rulesLayout.ruleCards().size());

    List<Member> members = IntStream.range(0, 8)
        .mapToObj(index -> new Member(new java.util.UUID(0, 200 + index), "member" + index))
        .toList();
    TerritoryDetailState memberState = new TerritoryDetailState(
        TerritoryDetailTestFixtures.territory(members), List.of(), TerritoryDetailViewKind.ACCESS,
        0, 5, "", ScreenState.READY, null, -1, 0);
    TerritoryDetailLayout.Layout memberLayout = TerritoryDetailLayout.calculate(640, 360, memberState);
    assertTrue(memberLayout.pageSize() < members.size());
    assertTrue(memberLayout.content().contains(memberLayout.previousButton()));
    assertTrue(memberLayout.content().contains(memberLayout.pageText()));
    assertTrue(memberLayout.content().contains(memberLayout.nextButton()));
  }

  @Test
  void navigationAndSettingsHaveStableSections() {
    TerritoryDetailState main = new TerritoryDetailState(
        TerritoryDetailTestFixtures.territory(List.of()), List.of(), TerritoryDetailViewKind.MAIN,
        0, 5, "", ScreenState.READY, null, -1, 0);
    TerritoryDetailLayout.Layout mainLayout = TerritoryDetailLayout.calculate(640, 360, main);
    assertEquals(5, mainLayout.navigationButtons().size());
    assertEquals(2, mainLayout.quickActions().size());

    TerritoryDetailState settings = new TerritoryDetailState(
        main.territory(), List.of(), TerritoryDetailViewKind.SETTINGS,
        0, 5, "", ScreenState.READY, null, -1, 0);
    TerritoryDetailLayout.Layout settingsLayout = TerritoryDetailLayout.calculate(640, 360, settings);
    assertEquals(List.of(TerritoryDetailAction.COPY_ID, TerritoryDetailAction.RESIZE,
            TerritoryDetailAction.TRANSFER, TerritoryDetailAction.DELETE),
        settingsLayout.settingsActions().stream().map(TerritoryDetailLayout.SettingAction::action).toList());
    assertTrue(settingsLayout.settingsActions().get(0).row().y()
        >= settingsLayout.settingsTitle().bottom() + 12);
    for (int index = 1; index < settingsLayout.settingsActions().size(); index++) {
      assertFalse(settingsLayout.settingsActions().get(index - 1).row()
          .overlaps(settingsLayout.settingsActions().get(index).row()));
    }
  }
}
