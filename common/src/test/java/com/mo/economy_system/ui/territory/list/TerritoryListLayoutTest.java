package com.mo.economy_system.ui.territory.list;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryListLayoutTest {
  @Test
  void gridAndControlsRemainContainedAcrossViewports() {
    TerritoryListState state = new TerritoryListState(
        List.of(new TerritoryListRow(summary(new UUID(0, 1), "one"), Optional.empty()),
            TerritoryListRow.owned(owned(new UUID(0, 2), "two"))),
        0, 1, "", ScreenState.READY, null, -1,
        java.util.Set.of(TerritoryListAction.TELEPORT, TerritoryListAction.MANAGE,
            TerritoryListAction.BACK));
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720}, {180, 120}, {640, 80}}) {
      TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      assertTrue(viewport.contains(layout.searchBackground()));
      assertTrue(viewport.contains(layout.previousButton()));
      assertTrue(viewport.contains(layout.nextButton()));
      for (TerritoryListLayout.Card card : layout.cards()) {
        assertTrue(viewport.contains(card.card()));
        assertTrue(card.card().contains(card.teleportButton()));
        if (card.row().owned()) assertTrue(card.card().contains(card.manageButton()));
      }
      assertFalse(layout.previousButton().overlaps(layout.nextButton()));
    }
  }

  private static Summary summary(UUID id, String name) {
    return new Summary(id, new UUID(0, 8), "owner", name, new Position(0, 64, 0),
        new Position(10, 70, 10), "minecraft:overworld");
  }

  private static Owned owned(UUID id, String name) {
    return new Owned(summary(id, name), List.of(), Optional.empty(),
        Arrays.stream(RuleAction.values()).map(a -> new Rule(a, RuleLevel.OWNER_ONLY)).toList(), List.of());
  }
}
