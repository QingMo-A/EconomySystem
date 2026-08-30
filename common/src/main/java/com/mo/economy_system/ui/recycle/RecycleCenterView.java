package com.mo.economy_system.ui.recycle;

import com.mo.economy_system.common.network.RecycleOfferSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.text.UiNumbers;
import java.util.List;

public final class RecycleCenterView {
  private RecycleCenterView() {}
  public static void render(EconomyUiRenderer renderer, RecycleCenterState state,
                            RecycleCenterLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.list(), EconomyUiTheme.HOME_LEADERBOARD_CARD, false);
    renderer.translatedText("screen.recycle.offers", List.of(), layout.list().x()+10, layout.list().y()+10, 0xFFFFFFFF);
    renderer.card(layout.detail(), EconomyUiTheme.HOME_BALANCE_CARD, false);
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.recycle.loading", List.of(), layout.retry(), 0xFFFFFFFF, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.retry(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retry(), EconomyUiTheme.TERRITORY_BUTTON, "screen.recycle.retry", List.of(), layout.retry().contains(mouseX,mouseY), true);
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.recycle.empty", List.of(), layout.retry(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    } else {
      for (RecycleCenterLayout.Row row : layout.rows()) {
        renderer.card(row.rect(), row.selected()?EconomyUiTheme.HOME_BALANCE_CARD:EconomyUiTheme.HOME_LEADERBOARD_CARD, row.rect().contains(mouseX,mouseY));
        renderer.item(row.offer().itemId(), new com.mo.economy_system.ui.geometry.UiRect(row.rect().x()+4,row.rect().y()+4,16,16));
        renderer.itemDisplayName(row.offer().itemId(), new com.mo.economy_system.ui.geometry.UiRect(row.rect().x()+26,row.rect().y()+3,row.rect().width()-130,18), 0xFFFFFFFF, UiTextAlignment.LEFT);
        renderer.text(UiNumbers.formatInteger(row.offer().currentUnitPrice())+" / "+UiNumbers.formatInteger(row.offer().ownedCount()), row.rect().right()-96,row.rect().y()+6, row.offer().highQuotaRemaining()>0?0xFFFFD54F:0xFFCCCCCC);
      }
      RecycleOfferSnapshot selected = state.selected();
      if (selected != null) {
        renderer.itemWithCount(selected.itemId(), 1, new com.mo.economy_system.ui.geometry.UiRect(layout.detail().x()+12,layout.detail().y()+18,24,24));
        renderer.itemDisplayName(selected.itemId(), new com.mo.economy_system.ui.geometry.UiRect(layout.detail().x()+44,layout.detail().y()+20,layout.detail().width()-56,18), 0xFFFFFFFF, UiTextAlignment.LEFT);
        renderer.text("单价: "+UiNumbers.formatInteger(selected.currentUnitPrice()), layout.detail().x()+12, layout.detail().y()+52, 0xFFFFD54F);
        renderer.text("背包: "+selected.ownedCount(), layout.detail().x()+12, layout.detail().y()+68, 0xFFCCCCCC);
        renderer.text("数量: "+state.amount(), layout.detail().x()+12, layout.detail().y()+86, 0xFFFFFFFF);
        renderer.translatedButton(layout.minus(), EconomyUiTheme.TERRITORY_BUTTON, "screen.recycle.minus", List.of(), layout.minus().contains(mouseX,mouseY), state.amount()>1);
        renderer.translatedButton(layout.plus(), EconomyUiTheme.TERRITORY_BUTTON, "screen.recycle.plus", List.of(), layout.plus().contains(mouseX,mouseY), state.amount()<selected.maxSubmitAmount());
        renderer.translatedButton(layout.all(), EconomyUiTheme.TERRITORY_BUTTON, "screen.recycle.all", List.of(), layout.all().contains(mouseX,mouseY), selected.maxSubmitAmount()>0);
        renderer.translatedButton(layout.submit(), EconomyUiTheme.TERRITORY_BUTTON, "screen.recycle.submit", List.of(), layout.submit().contains(mouseX,mouseY), state.amount()>0);
      }
    }
    renderer.translatedButton(layout.back(), EconomyUiTheme.TERRITORY_BUTTON, "button.common.back", List.of(), layout.back().contains(mouseX,mouseY), true);
  }
}
