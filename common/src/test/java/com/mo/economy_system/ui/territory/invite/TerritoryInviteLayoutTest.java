package com.mo.economy_system.ui.territory.invite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteLayoutTest {
  @Test
  void controlsAndInviteButtonsStayWithinViewport() {
    List<PlayerSummary> players = new ArrayList<>();
    for (int i = 0; i < 20; i++) players.add(new PlayerSummary(new UUID(0, i + 10), "player" + i));
    TerritoryInviteState state = new TerritoryInviteState(new UUID(0, 1), "territory",
        new UUID(0, 2), new UUID(0, 3), Set.of(), players, "", 0, 5,
        ScreenState.READY, null, -1, 1, 0);
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {160, 90}, {640, 80}}) {
      var layout = TerritoryInviteLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      for (UiRect rect : List.of(layout.title(), layout.subtitle(), layout.search(), layout.rows(),
          layout.previousButton(), layout.pageText(), layout.nextButton(), layout.backButton(), layout.retryButton())) {
        assertTrue(viewport.contains(rect), size[0] + "x" + size[1] + " " + rect);
      }
      assertFalse(layout.previousButton().overlaps(layout.nextButton()));
      for (var row : layout.playerRows()) {
        assertTrue(layout.rows().contains(row.row()));
        assertTrue(row.row().contains(row.inviteButton()));
      }
    }
  }
}
