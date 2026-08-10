package com.mo.economy_system.ui.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact table/chrome assertions transcribed from the legacy balance-log screen. */
class BalanceLogLegacyReferenceParityTest {
  @Test
  void legacyPanelUsesGoldThemeAndFixedTableGeometry() {
    BalanceLogState state = ready();
    BalanceLogLayout.Layout layout = BalanceLogLayout.calculate(640, 360, state);
    assertEquals(new UiRect(12, 12, 616, 336), layout.panel());
    assertEquals(new UiRect(22, 40, 70, 22), layout.tabs().get(0).rect());
    assertEquals(new UiRect(22, 70, 596, 22), layout.rows().get(0).rect());
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    BalanceLogView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("card")
        && op.value().contains("accent=" + EconomyUiTheme.BALANCE_ACCENT)),
        "balance panel uses the legacy gold accent");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("button")
        && op.value().contains("accent=" + (EconomyUiTheme.BALANCE_ACCENT & 0x00FFFFFF))),
        "active category tab uses the gold action style");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("fill")
        && op.value().equals(Integer.toString(0x301A2633))),
        "table rows use the legacy alternating fill instead of card chrome");
  }

  @Test
  void legacyRowsUseExactColumnsNumberFormatDescriptionWidthAndDeltaColors() {
    BalanceLogEntry positive = new BalanceLogEntry(1_700_000_000_000L,
        BalanceLogRequestMessage.ALL_CATEGORIES, "positive reason", 25, 100, 125);
    BalanceLogEntry negative = new BalanceLogEntry(1_700_000_001_000L,
        "market", "negative reason", -7, 125, 118);
    BalanceLogState state = new BalanceLogState(
        List.of(new BalanceLogRow(positive), new BalanceLogRow(negative)),
        BalanceLogRequestMessage.ALL_CATEGORIES, 0, 50, 2, 0, 8, ScreenState.READY, null, 1,
        Set.of(BalanceLogAction.BACK));
    BalanceLogLayout.Layout layout = BalanceLogLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    BalanceLogView.render(renderer, state, layout, 0, 0);
    int firstX = layout.rows().get(0).rect().x();
    int secondX = layout.rows().get(1).rect().x();
    // Legacy source uses category x+82; this assertion failed before the production correction.
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("text")
        && op.rect().equals(new UiRect(firstX + 82, layout.rows().get(0).rect().y() + 4, 0, 0))));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("text")
        && op.rect().equals(new UiRect(firstX + 132, layout.rows().get(0).rect().y() + 4, 0, 0))
        && op.value().equals("+25")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("text")
        && op.rect().equals(new UiRect(secondX + 132, layout.rows().get(1).rect().y() + 4, 0, 0))
        && op.value().equals("-7")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("textInRect")
        && op.rect().equals(new UiRect(firstX + 320, layout.rows().get(0).rect().y() + 2,
            Math.max(80, layout.rows().get(0).rect().width() - 330), 18))));
    assertTrue(renderer.paints().stream().anyMatch(p -> p.kind().equals("text")
        && p.rect().x() == firstX + 132 && p.argb() == EconomyUiTheme.TEXT_SUCCESS));
    assertTrue(renderer.paints().stream().anyMatch(p -> p.kind().equals("text")
        && p.rect().x() == secondX + 132 && p.argb() == EconomyUiTheme.TEXT_ERROR));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("textInRect")
        && op.value().startsWith("1 / 1  \u5171 2 \u6761")),
        "legacy pagination includes the localized total-count suffix");
  }

  private static BalanceLogState ready() {
    BalanceLogEntry entry = new BalanceLogEntry(1_700_000_000_000L,
        BalanceLogRequestMessage.ALL_CATEGORIES, "legacy reason", 25, 100, 125);
    return new BalanceLogState(List.of(new BalanceLogRow(entry)),
        BalanceLogRequestMessage.ALL_CATEGORIES, 0, 50, 1, 0, 8, ScreenState.READY, null, 1,
        Set.of(BalanceLogAction.BACK, BalanceLogAction.RETRY));
  }
}
