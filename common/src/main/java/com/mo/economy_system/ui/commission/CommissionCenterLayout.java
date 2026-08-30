package com.mo.economy_system.ui.commission;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

public final class CommissionCenterLayout {
  public static final int BACKGROUND_COLOR = 0x500A0A14;
  private CommissionCenterLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, CommissionCenterState state,
      UiTextMetrics metrics) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), pad = EconomyUiTheme.PANEL_PADDING;
    UiRect list = new UiRect(pad, 18, Math.max(1, width / 2 - pad), Math.max(1, height - 38));
    UiRect detail = new UiRect(list.right() + 10, 18, Math.max(1, width - list.right() - 10 - pad), list.height());
    List<Card> cards = new ArrayList<>();
    int cardHeight = 42;
    for (int i = 0; i < state.commissions().size(); i++) {
      int y = list.y() + 30 + i * (cardHeight + 5);
      if (y + cardHeight > list.bottom() - 26) break;
      CommissionCenterState s = state;
      cards.add(new Card(state.commissions().get(i), new UiRect(list.x() + 8, y, list.width() - 16, cardHeight),
          state.commissions().get(i).commissionId().equals(s.selectedCommissionId())));
    }
    UiRect submit = new UiRect(detail.x() + 12, detail.bottom() - 34, Math.max(1, detail.width() - 24), 22);
    UiRect back = new UiRect(pad, height - pad - 22, 82, 22);
    UiRect retry = new UiRect(detail.x() + 12, detail.y() + detail.height() / 2, Math.max(1, detail.width() - 24), 22);
    return new Layout(scale, list, detail, List.copyOf(cards), submit, back, retry, metrics);
  }

  public record Layout(UiScale scale, UiRect list, UiRect detail, List<Card> cards, UiRect submit,
                       UiRect back, UiRect retry, UiTextMetrics metrics) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record Card(com.mo.economy_system.common.commission.CommissionInstance commission,
                     UiRect rect, boolean selected) {}
}
