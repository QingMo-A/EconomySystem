package com.mo.economy_system.ui.commission_public;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Fixed virtual-coordinate geometry for the public commission browser and detail pane. */
public final class PublicCommissionCenterLayout {
  public static final int BACKGROUND_COLOR = 0x400A0A14;
  private static final int PANEL_GAP = 8;
  private static final int CARD_GAP = 5;
  private static final int CARD_HEIGHT = 44;

  private PublicCommissionCenterLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight,
                                 PublicCommissionCenterState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight,
                                 PublicCommissionCenterState state, UiTextMetrics metrics) {
    if (state == null) throw new NullPointerException("state");
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int pad = EconomyUiTheme.PANEL_PADDING;
    int lineHeight = Math.max(1, metrics.lineHeight());

    UiRect title = new UiRect(pad, pad, Math.max(1, Math.min(220, width - pad * 2)), lineHeight + 10);
    UiRect esc = new UiRect(Math.max(pad, width - pad - 90), pad + 5, 90, lineHeight);
    int footerHeight = 22;
    int contentTop = title.bottom() + 8;
    int contentBottom = Math.max(contentTop + 1, height - pad - footerHeight - 8);
    int contentHeight = Math.max(1, contentBottom - contentTop);
    int contentWidth = Math.max(1, width - pad * 2);
    int listWidth = Math.max(1, Math.round(contentWidth * 0.45f));
    listWidth = Math.min(listWidth, Math.max(1, contentWidth - PANEL_GAP - 1));
    int detailWidth = Math.max(1, contentWidth - listWidth - PANEL_GAP);
    UiRect list = new UiRect(pad, contentTop, listWidth, contentHeight);
    UiRect detail = new UiRect(list.right() + PANEL_GAP, contentTop, detailWidth, contentHeight);
    UiRect listHeader = new UiRect(list.x() + 10, list.y() + 9,
        Math.max(1, list.width() - 20), lineHeight + 4);
    UiRect detailHeader = new UiRect(detail.x() + 10, detail.y() + 9,
        Math.max(1, detail.width() - 20), lineHeight + 4);

    List<Card> cards = new ArrayList<>();
    int cardX = list.x() + 8;
    int cardWidth = Math.max(1, list.width() - 16);
    int cardY = listHeader.bottom() + 8;
    for (PublicCommission commission : state.commissions()) {
      if (cardY + CARD_HEIGHT > list.bottom() - 8) break;
      cards.add(new Card(commission, new UiRect(cardX, cardY, cardWidth, CARD_HEIGHT),
          commission.commissionId().equals(state.selectedCommissionId())));
      cardY += CARD_HEIGHT + CARD_GAP;
    }

    int detailX = detail.x() + 12;
    int detailWidthInner = Math.max(1, detail.width() - 24);
    UiRect target = new UiRect(detailX, detail.y() + 43, detailWidthInner, lineHeight + 5);
    UiRect progress = new UiRect(detailX, target.bottom() + 9, detailWidthInner, lineHeight + 5);
    UiRect reward = new UiRect(detailX, progress.bottom() + 7, detailWidthInner, lineHeight + 5);
    UiRect expiration = new UiRect(detailX, reward.bottom() + 7, detailWidthInner, lineHeight + 5);
    UiRect amountInput = new UiRect(detailX, expiration.bottom() + 14, detailWidthInner, 19);
    UiRect submit = new UiRect(detailX, detail.bottom() - 34, detailWidthInner, 22);
    UiRect message = new UiRect(detailX, submit.y() - lineHeight - 8, detailWidthInner, lineHeight + 3);
    UiRect back = new UiRect(pad, height - pad - footerHeight, 82, footerHeight);
    UiRect retry = new UiRect(detailX, detail.y() + Math.max(1, (detail.height() - 22) / 2),
        detailWidthInner, 22);
    UiRect emptyOrLoading = new UiRect(list.x() + 8, listHeader.bottom() + 8,
        Math.max(1, list.width() - 16), Math.max(1, list.height() - 24));

    return new Layout(scale, title, esc, list, detail, listHeader, detailHeader,
        List.copyOf(cards), target, progress, reward, expiration, amountInput, submit,
        message, back, retry, emptyOrLoading, metrics);
  }

  public record Layout(
      UiScale scale,
      UiRect title,
      UiRect esc,
      UiRect list,
      UiRect detail,
      UiRect listHeader,
      UiRect detailHeader,
      List<Card> cards,
      UiRect target,
      UiRect progress,
      UiRect reward,
      UiRect expiration,
      UiRect amountInput,
      UiRect submit,
      UiRect message,
      UiRect back,
      UiRect retry,
      UiRect emptyOrLoading,
      UiTextMetrics metrics) {
    public Layout {
      cards = List.copyOf(cards);
    }
  }

  public record Card(PublicCommission commission, UiRect rect, boolean selected) {}
}
