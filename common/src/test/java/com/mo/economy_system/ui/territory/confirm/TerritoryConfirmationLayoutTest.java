package com.mo.economy_system.ui.territory.confirm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryConfirmationLayoutTest {
  @Test
  void destructiveDecisionRemainsReachableAtSupportedViewports() {
    TerritoryConfirmationState state = new TerritoryConfirmationState(
        TerritoryConfirmationKind.REMOVE_TERRITORY, new UUID(1, 1), "spawn", null, "",
        ScreenState.READY, Set.of(TerritoryConfirmationAction.values()));
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {640, 80}}) {
      TerritoryConfirmationLayout.Layout layout = TerritoryConfirmationLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      assertTrue(viewport.contains(layout.card()));
      assertTrue(layout.card().contains(layout.message()));
      assertTrue(layout.card().contains(layout.confirm()));
      assertTrue(layout.card().contains(layout.cancel()));
      assertFalse(layout.confirm().overlaps(layout.cancel()));
    }
  }
}
