package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.UiChromePlan;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeReferenceParityTest {
  @Test
  void canonical640By360GeometryMatchesLegacyReference() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 42,
        List.of(new AccountBalance("alice", 42), new AccountBalance("bob", 7)), 2, 3,
        0, 10, ScreenState.READY, null, -1, 1, 1);
    HomeLayout.Layout layout = HomeLayout.calculate(640, 360, state, UiTextMetrics.APPROXIMATE, 1);
    assertEquals(160, layout.leftPanelWidth());
    assertEquals(172, layout.rightPanelStartX());
    assertEquals(456, layout.rightPanelWidth());
    assertEquals(List.of(12, 48, 84, 120, 156),
        layout.navButtons().stream().map(button -> button.rect().y()).toList());
    assertEquals(new UiRect(12, 12, 136, 28), layout.navButtons().get(0).rect());
    assertEquals(new UiRect(172, 12, 224, 70), layout.balanceCard());
    assertEquals(new UiRect(404, 12, 224, 70), layout.tradeCard());
    assertEquals(new UiRect(172, 90, 456, 258), layout.leaderboardCard());
    assertEquals(128, layout.rows().get(0).rect().y());
    assertEquals(13, layout.rows().get(0).rect().height());
    assertEquals(348, layout.footer().bottom());
  }

  @Test
  void navigationStyleRetainsReferenceAlphaAndAlignment() {
    var style = EconomyUiTheme.HOME_NAV_SHOP_STYLE;
    assertEquals(0x99, style.backgroundAlpha());
    assertEquals(0xAA, style.backgroundAlphaHover());
    assertEquals(10, style.padding());
    assertTrue(style.textShadow());
    assertEquals(com.mo.economy_system.ui.renderer.UiTextAlignment.LEFT, style.alignment());
    assertEquals(0x99 << 24, UiChromePlan.buttonChrome(new UiRect(12, 12, 136, 28), style,
        false, true).get(0).argb() & 0xFF000000);
    assertEquals(UiIcon.SHOP, UiIcon.valueOf("SHOP"));
  }
}
