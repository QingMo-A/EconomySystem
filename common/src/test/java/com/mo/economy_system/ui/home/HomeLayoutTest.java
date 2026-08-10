package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.text.UiTextMetrics;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeLayoutTest {
  @Test
  void controlsStayInsideVirtualViewportAtSupportedSizes() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 12,
        accounts(20), 3, 4, 0, 5, ScreenState.READY, null, 1, 1, 1);
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {640, 80}}) {
      HomeLayout.Layout layout = HomeLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      for (UiRect rect : List.of(layout.balanceCard(), layout.tradeCard(), layout.leaderboardCard(),
          layout.footer(), layout.retryButton())) {
        assertTrue(viewport.contains(rect), size[0] + "x" + size[1] + " " + rect);
      }
      assertFalse(layout.balanceCard().overlaps(layout.tradeCard()));
      for (HomeLayout.NavButton button : layout.navButtons()) assertTrue(viewport.contains(button.rect()));
      for (HomeLayout.LeaderboardRow row : layout.rows()) assertTrue(layout.leaderboardCard().contains(row.rect()));
    }
  }

  @Test
  void footerUsesLegacyScaledContentWidthWithoutWholePanelClamp() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 12,
        accounts(2), 3, 4, 0, 5, ScreenState.READY, null, 1, 1, 1);
    UiTextMetrics longMetrics = new UiTextMetrics() {
      @Override public int width(String text) {
        return "EconomySystem".equals(text) ? 1000 : 10;
      }

      @Override public int lineHeight() {
        return 9;
      }
    };
    HomeLayout.Layout layout = HomeLayout.calculate(640, 360, state, longMetrics, 1.0f);
    int contentWidth = 14 + longMetrics.width("EconomySystem");
    float expectedScale = Math.min(1.0f,
        (float) layout.leftPanelWidth() / contentWidth);
    int expectedWidth = (int) (contentWidth * expectedScale) + 16;
    assertEquals(expectedScale, layout.footerScale(), 0.00001f);
    assertEquals(expectedWidth, layout.footer().width());
    assertTrue(layout.footer().width() > layout.leftPanelWidth());
  }

  @Test
  void renderAndHitTestGeometryShareMetricsAndAnimationProgress() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 12,
        accounts(20), 3, 4, 0, 10, ScreenState.READY, null, 1, 1, 1);
    for (int lineHeight : new int[] {9, 11}) {
      UiTextMetrics metrics = new UiTextMetrics() {
        @Override public int width(String text) { return text == null ? 0 : text.length() * 7; }
        @Override public int lineHeight() { return lineHeight; }
      };
      for (float progress : new float[] {0.0f, 0.5f, 1.0f}) {
        HomeLayout.Layout rendered = HomeLayout.calculate(854, 480, state, metrics, progress);
        HomeLayout.Layout hitTest = HomeLayout.calculate(854, 480, state, metrics, progress);
        assertEquals(rendered.navButtons(), hitTest.navButtons());
        assertEquals(rendered.balanceCard(), hitTest.balanceCard());
        assertEquals(rendered.tradeCard(), hitTest.tradeCard());
        assertEquals(rendered.retryButton(), hitTest.retryButton());
        assertEquals(rendered.rows(), hitTest.rows());
        assertEquals(rendered.pageSize(), hitTest.pageSize());
      }
    }
  }

  private static List<AccountBalance> accounts(int count) {
    List<AccountBalance> values = new ArrayList<>();
    for (int i = 0; i < count; i++) values.add(new AccountBalance("player" + i, i));
    return values;
  }
}
