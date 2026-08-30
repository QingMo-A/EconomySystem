package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.mail.MailAttachmentSnapshot;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiVersionInfoLayout;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate layout for the full mailbox three-pane view. */
public final class DeliveryLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int CARD_WIDTH = 190;
  public static final int CARD_HEIGHT = 48;
  private static final int CONTENT_START_Y = 52;
  private static final int CATEGORY_WIDTH = 96;
  private static final int LIST_WIDTH = CARD_WIDTH;
  private static final int PANE_GAP = 8;
  private static final int CARD_GAP = 6;
  private static final int FOOTER_HEIGHT = 42;
  private static final int CATEGORY_TAB_HEIGHT = 25;

  private DeliveryLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, DeliveryState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 0);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, DeliveryState state, UiTextMetrics metrics) {
    return calculate(physicalWidth, physicalHeight, state, metrics, 0);
  }

  public static Layout calculate(
      int physicalWidth, int physicalHeight, DeliveryState state, UiTextMetrics metrics, int attachmentFirstIndex) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int contentBottom = Math.max(CONTENT_START_Y + 120, height - FOOTER_HEIGHT);
    int contentHeight = Math.max(120, contentBottom - CONTENT_START_Y);

    int categoryX = panel;
    int listX = categoryX + CATEGORY_WIDTH + PANE_GAP;
    int detailX = listX + LIST_WIDTH + PANE_GAP;
    int detailWidth = Math.max(140, width - panel - detailX);

    UiRect categoryPanel = new UiRect(categoryX, CONTENT_START_Y, CATEGORY_WIDTH, contentHeight);
    List<CategoryTab> categoryTabs = new ArrayList<>();
    int categoryY = CONTENT_START_Y + 9;
    for (DeliveryCategory category : DeliveryCategory.values()) {
      categoryTabs.add(new CategoryTab(category,
          new UiRect(categoryX + 8, categoryY, CATEGORY_WIDTH - 16, CATEGORY_TAB_HEIGHT)));
      categoryY += CATEGORY_TAB_HEIGHT + 4;
    }
    UiRect composeButton = new UiRect(categoryX + 8, contentBottom - 31, CATEGORY_WIDTH - 16, 22);

    UiRect search = new UiRect(listX, 20, LIST_WIDTH, 20);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2, search.width() + 8, search.height() + 4);

    int pagerHeight = 26;
    int cardsAreaHeight = Math.max(CARD_HEIGHT, contentHeight - pagerHeight - 6);
    int pageSize = Math.max(1, cardsAreaHeight / (CARD_HEIGHT + CARD_GAP));
    List<Card> cards = new ArrayList<>();
    List<DeliveryRow> visible = state.visibleRows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int y = CONTENT_START_Y + i * (CARD_HEIGHT + CARD_GAP);
      UiRect card = new UiRect(listX, y, LIST_WIDTH, CARD_HEIGHT);
      UiRect item = new UiRect(card.x() + 7, card.y() + 12, 24, 24);
      cards.add(new Card(visible.get(i), card, item, new UiRect(0, 0, 0, 0)));
    }

    int controlsY = contentBottom - 24;
    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    UiRect previous = new UiRect(listX, controlsY, 42, 22);
    UiRect page = new UiRect(listX + (LIST_WIDTH - pageTextWidth) / 2, controlsY + 6,
        pageTextWidth, Math.max(1, metrics.lineHeight()));
    UiRect next = new UiRect(listX + LIST_WIDTH - 42, controlsY, 42, 22);

    UiRect detailPanel = new UiRect(detailX, CONTENT_START_Y, detailWidth, contentHeight);
    int innerX = detailX + 14;
    int innerWidth = Math.max(1, detailWidth - 28);
    int lineHeight = Math.max(1, metrics.lineHeight());
    UiRect detailSender = new UiRect(innerX, CONTENT_START_Y + 12, innerWidth, lineHeight);
    UiRect detailSubject = new UiRect(innerX, CONTENT_START_Y + 31, innerWidth, lineHeight + 2);
    UiRect detailMeta = new UiRect(innerX, CONTENT_START_Y + 49, innerWidth, lineHeight);

    DeliveryRow selected = state.selectedRow();
    boolean hasCurrencyReward = selected != null && selected.mail().hasCurrencyReward();
    UiRect detailReward = hasCurrencyReward
        ? new UiRect(innerX, detailMeta.y() + lineHeight, innerWidth, lineHeight)
        : new UiRect(0, 0, 0, 0);
    UiRect detailBody = new UiRect(innerX, CONTENT_START_Y + 67 + (hasCurrencyReward ? lineHeight : 0),
        innerWidth, lineHeight * 4 + 12);
    List<AttachmentCard> attachmentCards = new ArrayList<>();
    int attachmentTop = CONTENT_START_Y + 157;
    UiRect attachmentLabel = new UiRect(innerX, attachmentTop - 19, innerWidth, lineHeight);
    UiRect attachmentStrip = new UiRect(innerX, attachmentTop, innerWidth, 38);
    int slotSize = 28;
    int slotGap = 4;
    int stripPadding = 5;
    int usableWidth = Math.max(slotSize, attachmentStrip.width() - stripPadding * 2);
    int attachmentCapacity = Math.max(1, (usableWidth + slotGap) / (slotSize + slotGap));
    int attachmentTotal = selected == null ? 0 : selected.mail().attachments().size();
    int attachmentMaxFirstIndex = Math.max(0, attachmentTotal - attachmentCapacity);
    int firstIndex = Math.max(0, Math.min(attachmentFirstIndex, attachmentMaxFirstIndex));
    if (selected != null) {
      List<MailAttachmentSnapshot> attachments = selected.mail().attachments();
      int end = Math.min(attachments.size(), firstIndex + attachmentCapacity);
      for (int i = firstIndex; i < end; i++) {
        int visibleIndex = i - firstIndex;
        UiRect card = new UiRect(attachmentStrip.x() + stripPadding + visibleIndex * (slotSize + slotGap),
            attachmentStrip.y() + 5, slotSize, slotSize);
        UiRect icon = new UiRect(card.x() + 6, card.y() + 6, 16, 16);
        attachmentCards.add(new AttachmentCard(attachments.get(i), card, icon, new UiRect(0, 0, 0, 0)));
      }
    }

    UiRect attachmentScrollTrack = new UiRect(0, 0, 0, 0);
    UiRect attachmentScrollThumb = new UiRect(0, 0, 0, 0);
    if (attachmentMaxFirstIndex > 0) {
      attachmentScrollTrack = new UiRect(attachmentStrip.x() + 5, attachmentStrip.bottom() + 4,
          Math.max(1, attachmentStrip.width() - 10), 5);
      int thumbWidth = Math.max(22,
          Math.min(attachmentScrollTrack.width(),
              Math.round(attachmentScrollTrack.width() * (attachmentCapacity / (float) attachmentTotal))));
      int travel = Math.max(0, attachmentScrollTrack.width() - thumbWidth);
      int thumbX = attachmentScrollTrack.x();
      if (travel > 0) {
        thumbX += Math.round(travel * (firstIndex / (float) attachmentMaxFirstIndex));
      }
      attachmentScrollThumb = new UiRect(thumbX, attachmentScrollTrack.y(), thumbWidth, attachmentScrollTrack.height());
    }

    int actionTop = contentBottom - 30;
    UiRect claimAllButton = new UiRect(innerX, actionTop, Math.max(72, innerWidth / 2 - 3), 22);
    UiRect deleteButton = new UiRect(claimAllButton.right() + 6, actionTop,
        Math.max(50, detailPanel.right() - 14 - (claimAllButton.right() + 6)), 22);

    UiRect empty = new UiRect(0, 0, 0, 0);
    UiRect firstAttachmentCard = attachmentCards.isEmpty() ? empty : attachmentCards.get(0).card();
    UiRect firstItemIcon = attachmentCards.isEmpty() ? empty : attachmentCards.get(0).itemIcon();
    UiRect firstClaim = attachmentCards.isEmpty() ? empty : attachmentCards.get(0).claimButton();
    UiRect firstName = attachmentCards.isEmpty() ? empty
        : new UiRect(firstItemIcon.right() + 4, firstAttachmentCard.y() + 6,
            Math.max(1, firstClaim.x() - firstItemIcon.right() - 8), lineHeight);
    UiRect source = new UiRect(innerX, detailMeta.y(), innerWidth, lineHeight);

    UiRect message = new UiRect(detailPanel.x(), detailPanel.y(), detailPanel.width(), detailPanel.height());
    UiVersionInfoLayout.Result versionInfo = UiVersionInfoLayout.calculate(metrics,
        "screen.delivery_box.title", List.of(), panel, height - panel, 150);
    UiRect title = versionInfo.card();
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight, 90, lineHeight);

    return new Layout(scale, title, versionInfo.contentScale(), esc, search, searchBackground,
        categoryPanel, List.copyOf(categoryTabs), composeButton, List.copyOf(cards), detailPanel,
        detailSender, detailSubject, detailMeta, detailReward, detailBody, attachmentLabel,
        attachmentStrip, attachmentScrollTrack, attachmentScrollThumb,
        List.copyOf(attachmentCards), firstAttachmentCard, firstItemIcon, firstName, source, firstClaim,
        claimAllButton, deleteButton, previous, page, next, message, pageSize, 1, pageSize,
        attachmentCapacity, attachmentMaxFirstIndex, firstIndex, metrics);
  }

  public record Layout(
      UiScale scale, UiRect title, float versionInfoScale, UiRect esc, UiRect search, UiRect searchBackground,
      UiRect categoryPanel, List<CategoryTab> categoryTabs, UiRect composeButton, List<Card> cards,
      UiRect detailPanel, UiRect detailSender, UiRect detailSubject, UiRect detailMeta, UiRect detailReward,
      UiRect detailBody,
      UiRect attachmentLabel, UiRect attachmentStrip, UiRect attachmentScrollTrack, UiRect attachmentScrollThumb,
      List<AttachmentCard> attachmentCards,
      UiRect attachmentCard, UiRect detailItemIcon, UiRect detailItemName, UiRect detailSource,
      UiRect detailClaimButton, UiRect claimAllButton, UiRect deleteButton,
      UiRect previousButton, UiRect pageText, UiRect nextButton, UiRect message,
      int pageSize, int columns, int rows,
      int attachmentVisibleCapacity, int attachmentMaxFirstIndex, int attachmentFirstIndex,
      UiTextMetrics metrics) {
    public Layout {
      categoryTabs = List.copyOf(categoryTabs);
      cards = List.copyOf(cards);
      attachmentCards = List.copyOf(attachmentCards);
    }
  }

  public record CategoryTab(DeliveryCategory category, UiRect rect) {}
  public record Card(DeliveryRow row, UiRect card, UiRect itemIcon, UiRect claimButton) {}
  public record AttachmentCard(MailAttachmentSnapshot attachment, UiRect card, UiRect itemIcon, UiRect claimButton) {}
}
