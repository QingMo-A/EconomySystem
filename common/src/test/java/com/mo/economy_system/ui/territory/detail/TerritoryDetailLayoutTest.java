package com.mo.economy_system.ui.territory.detail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TerritoryDetailLayoutTest {
  @Test
  void allNestedControlsStayInsideTheVirtualViewport() {
    List<PlayerSummary> players = IntStream.range(0, 12)
        .mapToObj(index -> new PlayerSummary(new java.util.UUID(0, 100 + index), "player" + index))
        .toList();
    for (TerritoryDetailViewKind view : TerritoryDetailViewKind.values()) {
      TerritoryDetailState state = new TerritoryDetailState(
          TerritoryDetailTestFixtures.territory(List.of()), players, view, 0, 5, "",
          ScreenState.READY, null, -1, 1);
      for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720}, {180, 120}}) {
        TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(size[0], size[1], state);
        UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
        for (UiRect rect : List.of(layout.title(), layout.subtitle(), layout.search(), layout.rows(),
            layout.previousButton(), layout.pageText(), layout.nextButton(), layout.retryButton(),
            layout.backButton())) {
          assertTrue(viewport.contains(rect), view + " " + size[0] + "x" + size[1] + " " + rect);
        }
        assertFalse(layout.previousButton().overlaps(layout.nextButton()));
        assertFalse(layout.backButton().overlaps(layout.previousButton()));
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
}
