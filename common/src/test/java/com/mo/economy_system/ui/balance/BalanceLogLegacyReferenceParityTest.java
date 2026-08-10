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

  private static BalanceLogState ready() {
    BalanceLogEntry entry = new BalanceLogEntry(1_700_000_000_000L,
        BalanceLogRequestMessage.ALL_CATEGORIES, "legacy reason", 25, 100, 125);
    return new BalanceLogState(List.of(new BalanceLogRow(entry)),
        BalanceLogRequestMessage.ALL_CATEGORIES, 0, 50, 1, 0, 8, ScreenState.READY, null, 1,
        Set.of(BalanceLogAction.BACK, BalanceLogAction.RETRY));
  }
}
