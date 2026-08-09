package com.mo.economy_system.ui.balance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BalanceLogLayoutTest {
  @Test
  void tabsAndRowsStayInsidePanelAcrossViewports() {
    BalanceLogState state = new BalanceLogState(List.of(
        new BalanceLogRow(new BalanceLogEntry(1, "系统", "seed", 1, 0, 1))),
        "全部", 0, 50, 1, 0, 8, ScreenState.READY, null, 1, Set.of());
    for (int[] viewport : new int[][] {{1, 1}, {120, 80}, {640, 360}, {1920, 1080}}) {
      BalanceLogLayout.Layout layout = BalanceLogLayout.calculate(viewport[0], viewport[1], state);
      assertTrue(layout.panel().width() > 0 && layout.panel().height() > 0);
      for (BalanceLogLayout.Tab tab : layout.tabs()) assertTrue(layout.panel().contains(tab.rect()));
      for (BalanceLogLayout.Row row : layout.rows()) assertTrue(layout.panel().contains(row.rect()));
    }
  }
}
