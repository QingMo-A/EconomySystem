package com.mo.economy_system.ui.territory.buff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact buff-card, action-style, tooltip and pagination assertions from legacy Screen_TerritoryBuff. */
class TerritoryBuffLegacyReferenceParityTest {
  @Test
  void buffCardsKeepTerritoryThemeAndTranslatedActionStyle() {
    BuffManageState state = state(0);
    BuffManageLayout.Layout layout = BuffManageLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    BuffManageView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("inputFrame")
        && operation.rect().equals(new com.mo.economy_system.ui.geometry.UiRect(
            layout.search().x() - 4, layout.search().y() - 2,
            layout.search().width() + 8, layout.search().height() + 4))),
        "legacy buff search uses four-edge territory chrome");
    var firstCard = layout.cards().get(0).card();
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("fill")
        && operation.rect().equals(new com.mo.economy_system.ui.geometry.UiRect(
            firstCard.x(), firstCard.y(), 2, firstCard.height()))
        && operation.value().equals(Integer.toString(EconomyUiTheme.TERRITORY_ACCENT))),
        "unlocked buff panels use the restrained territory accent stripe");
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("icon")
        && operation.value().equals(UiIcon.BUFF.name())));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedButton")
        && operation.value().startsWith("button.territory.buff.upgrade")
        && operation.value().contains("accent=" + EconomyUiTheme.TERRITORY_BUFF_UPGRADE_BUTTON.accent())));
  }

  @Test
  void buffPagingUsesNativeArrowsAndExactCostTooltipLines() {
    BuffManageState state = state(1);
    BuffManageLayout.Layout layout = BuffManageLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    BuffManageView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("icon")
        && operation.value().equals(UiIcon.ARROW_LEFT.name()) && operation.rect().width() == 12
        && operation.rect().height() == 12));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("icon")
        && operation.value().equals(UiIcon.ARROW_RIGHT.name()) && operation.rect().width() == 12
        && operation.rect().height() == 12));
    var tooltip = BuffManageView.tooltipAt(state, layout, layout.cards().get(0).cost().x(),
        layout.cards().get(0).cost().y()).orElseThrow();
    assertEquals(2, tooltip.lines().size(), "cost tooltip keeps the cost heading and one currency line");
    assertTrue(tooltip.lines().get(1).toString().contains("currency"));
  }

  private static BuffManageState state(int page) {
    Buff first = buff("speed", "Speed", true, 1);
    Buff second = buff("haste", "Haste", true, 1);
    BuffRow one = BuffRow.inspect(first, new BuffResourceSnapshot(Map.of(), 0, true));
    BuffRow two = BuffRow.inspect(second, new BuffResourceSnapshot(Map.of(), 0, true));
    return new BuffManageState(new UUID(4, 4), "spawn", List.of(one, two), page, 1, 0, "",
        ScreenState.READY, null, -1);
  }

  private static Buff buff(String id, String name, boolean unlocked, int level) {
    return new Buff(id, name, "effect." + id, true, 0, 1, 3, unlocked, level,
        List.of(new BuffUpgradeCost(List.of(), 0, 7)));
  }
}
