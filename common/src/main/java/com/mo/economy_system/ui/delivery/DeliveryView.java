package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.component.UiEmptyState;
import com.mo.economy_system.ui.component.UiItemSlot;
import com.mo.economy_system.ui.component.UiPanel;
import com.mo.economy_system.ui.component.UiSection;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Semantic full mailbox renderer shared by Forge and NeoForge. */
public final class DeliveryView {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
      .withZone(ZoneId.systemDefault());

  private DeliveryView() {}

  public static void renderSearchFrame(EconomyUiRenderer renderer, UiRect nativeWidgetRect, boolean focused) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.DELIVERY_SEARCH_FRAME, focused);
  }

  public static void renderSearchFrame(EconomyUiRenderer renderer, UiRect nativeWidgetRect,
                                       boolean focused, boolean hovered) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.DELIVERY_SEARCH_FRAME,
        focused, hovered);
  }

  public static void render(EconomyUiRenderer renderer, DeliveryState state,
                            DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(com.mo.economy_system.ui.renderer.UiIcon.DELIVERY,
        "screen.delivery_box.title", List.of(), layout.title().x() + 8,
        layout.title().y() + 5, layout.versionInfoScale(), EconomyUiRenderer.ICON_SIZE,
        EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.delivery_box.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);

    UiPanel.render(renderer, layout.categoryPanel(), false);
    UiPanel.render(renderer, layout.detailPanel(), false);

    for (DeliveryLayout.CategoryTab tab : layout.categoryTabs()) {
      boolean active = tab.category() == state.category();
      UiSection.render(renderer, tab.rect(), active ? EconomyUiTheme.DELIVERY_ACCENT : 0,
          tab.rect().contains(mouseX, mouseY));
      String count = "  " + state.count(tab.category());
      renderer.translatedTextWithSuffix(tab.category().labelKey(), List.of(), count,
          new UiRect(tab.rect().x() + 7, tab.rect().y(), Math.max(1, tab.rect().width() - 14), tab.rect().height()),
          active ? EconomyUiTheme.TEXT_PRIMARY : EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    }
    renderer.translatedButton(layout.composeButton(), EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
        "screen.mailbox.compose", List.of(), layout.composeButton().contains(mouseX, mouseY),
        state.can(DeliveryAction.COMPOSE));

    if (state.screenState() == ScreenState.LOADING && state.rows().isEmpty()) {
      renderer.translatedTextInRect("screen.delivery_box.loading", List.of(), layout.message(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.delivery_box.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.EMPTY) {
      UiEmptyState.render(renderer, layout.message(),
          "screen.delivery_box.empty", "screen.delivery_box.empty_hint");
    } else if (state.filteredRows().isEmpty()) {
      UiEmptyState.render(renderer, layout.message(),
          "screen.mailbox.category_empty", "screen.mailbox.category_empty_hint");
    }

    DeliveryRow selected = state.selectedRow();
    for (DeliveryLayout.Card card : layout.cards()) {
      DeliveryRow row = card.row();
      boolean selectedCard = selected != null && row.mailId().equals(selected.mailId());
      UiSection.render(renderer, card.card(), selectedCard ? EconomyUiTheme.DELIVERY_ACCENT : 0,
          card.card().contains(mouseX, mouseY));
      var first = row.firstAttachment();
      if (first != null) renderer.itemWithCount(first.item().itemId(), first.item().count(), card.itemIcon());
      int infoX = first == null ? card.card().x() + 10 : card.itemIcon().right() + 12;
      int infoWidth = Math.max(1, card.card().right() - infoX - 7);
      int lineHeight = Math.max(1, layout.metrics().lineHeight());
      if (!row.mail().read()) {
        renderer.textInRect("●", new UiRect(card.card().right() - 12, card.card().y() + 5, 8, lineHeight),
            EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.RIGHT);
      }
      renderSubject(renderer, row,
          new UiRect(infoX, card.card().y() + 6, infoWidth, lineHeight + 4),
          EconomyUiTheme.DELIVERY_ACCENT, UiTextAlignment.LEFT);
      renderSender(renderer, row,
          new UiRect(infoX, card.card().bottom() - lineHeight - 5, infoWidth, lineHeight),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    }

    if (selected != null && state.screenState() != ScreenState.ERROR) {
      renderSelected(renderer, state, selected, layout, mouseX, mouseY);
    }

    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(),
          previousEnabled ? EconomyUiTheme.DELIVERY_PAGE_BUTTON : EconomyUiTheme.DELIVERY_PAGE_BUTTON_DISABLED,
          "<", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(),
          nextEnabled ? EconomyUiTheme.DELIVERY_PAGE_BUTTON : EconomyUiTheme.DELIVERY_PAGE_BUTTON_DISABLED,
          ">", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    }

    tooltipAt(state, layout, mouseX, mouseY).ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  private static void renderSelected(EconomyUiRenderer renderer, DeliveryState state, DeliveryRow row,
                                     DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    renderSender(renderer, row, layout.detailSender(), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderSubject(renderer, row, layout.detailSubject(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    String meta = formatTime(row.mail().createdAtEpochMillis());
    if (!row.mail().source().isBlank()) meta = meta.isEmpty() ? row.mail().source() : meta + " · " + row.mail().source();
    renderer.textInRect(meta, layout.detailMeta(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    if (row.mail().hasMoney()) {
      renderer.translatedTextInRect("screen.mailbox.money", List.of(Integer.toString(row.mail().moneyAmount())),
          layout.detailMeta(), EconomyUiTheme.TEXT_SUCCESS, UiTextAlignment.RIGHT);
    }
    if (!row.mail().body().isBlank()) {
      renderWrappedText(renderer, row.mail().body(), layout.detailBody(), layout,
          EconomyUiTheme.TEXT_SECONDARY);
    } else {
      renderer.translatedTextInRect(row.bodyKey(), List.of(), layout.detailBody(),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    }

    if (!row.mail().attachments().isEmpty()) {
      renderer.translatedTextInRect("screen.mailbox.attachments", List.of(Integer.toString(row.mail().attachments().size())),
          layout.attachmentLabel(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
      UiPanel.render(renderer, layout.attachmentStrip(), layout.attachmentStrip().contains(mouseX, mouseY));
      for (DeliveryLayout.AttachmentCard card : layout.attachmentCards()) {
        boolean claimed = card.attachment().claimed();
        UiItemSlot.State slotState = claimed
            ? UiItemSlot.State.CLAIMED
            : (card.card().contains(mouseX, mouseY) ? UiItemSlot.State.HOVERED : UiItemSlot.State.NORMAL);
        UiItemSlot.renderItem(renderer, card.card(), card.itemIcon(),
            card.attachment().item().itemId(), card.attachment().item().count(),
            slotState, EconomyUiTheme.DELIVERY_ACCENT);
      }
      if (layout.attachmentMaxFirstIndex() > 0) {
        renderer.fill(layout.attachmentScrollTrack(), 0x704A5568);
        renderer.fill(layout.attachmentScrollThumb(), EconomyUiTheme.DELIVERY_ACCENT);
      }
      if (row.mail().hasUnclaimedAttachments()) {
        renderer.translatedButton(layout.claimAllButton(), EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
            "screen.mailbox.claim_all", List.of(), layout.claimAllButton().contains(mouseX, mouseY),
            state.can(DeliveryAction.CLAIM_ALL) && state.screenState() == ScreenState.READY);
      }
    }

    boolean canDelete = !row.mail().hasUnclaimedAttachments() && state.can(DeliveryAction.DELETE)
        && state.screenState() == ScreenState.READY;
    renderer.translatedButton(layout.deleteButton(),
        canDelete ? EconomyUiTheme.DELIVERY_PAGE_BUTTON : EconomyUiTheme.DELIVERY_PAGE_BUTTON_DISABLED,
        row.mail().globalAnnouncement() ? "screen.mailbox.dismiss" : "screen.mailbox.delete",
        List.of(), layout.deleteButton().contains(mouseX, mouseY), canDelete);
  }

  private static void renderSender(EconomyUiRenderer renderer, DeliveryRow row, UiRect rect, int color,
                                   UiTextAlignment alignment) {
    if (!row.mail().senderName().isBlank()) renderer.textInRect(row.mail().senderName(), rect, color, alignment);
    else renderer.translatedTextInRect(row.senderKey(), List.of(), rect, color, alignment);
  }

  private static void renderSubject(EconomyUiRenderer renderer, DeliveryRow row, UiRect rect, int color,
                                    UiTextAlignment alignment) {
    if (!row.mail().subject().isBlank()) renderer.textInRect(row.mail().subject(), rect, color, alignment);
    else renderer.translatedTextInRect(row.subjectKey(), List.of(), rect, color, alignment);
  }

  private static void renderWrappedText(EconomyUiRenderer renderer, String text, UiRect rect,
                                        DeliveryLayout.Layout layout, int color) {
    int lineHeight = Math.max(1, layout.metrics().lineHeight());
    int maxLines = Math.max(1, rect.height() / lineHeight);
    int lineIndex = 0;
    for (String paragraph : text.replace("\r", "").split("\n", -1)) {
      if (lineIndex >= maxLines) break;
      if (paragraph.isEmpty()) {
        lineIndex++;
        continue;
      }
      StringBuilder line = new StringBuilder();
      for (int offset = 0; offset < paragraph.length();) {
        int codePoint = paragraph.codePointAt(offset);
        String glyph = new String(Character.toChars(codePoint));
        String candidate = line + glyph;
        if (line.length() > 0 && layout.metrics().width(candidate) > rect.width()) {
          renderer.text(line.toString(), rect.x(), rect.y() + lineIndex * lineHeight, color);
          lineIndex++;
          if (lineIndex >= maxLines) return;
          line.setLength(0);
        }
        line.append(glyph);
        offset += Character.charCount(codePoint);
      }
      if (lineIndex < maxLines && line.length() > 0) {
        renderer.text(line.toString(), rect.x(), rect.y() + lineIndex * lineHeight, color);
        lineIndex++;
      }
    }
  }

  private static String formatTime(long epochMillis) {
    return epochMillis <= 0 ? "" : TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis));
  }

  public static Optional<TooltipModel> tooltipAt(DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    for (DeliveryLayout.Card card : layout.cards()) {
      if (card.itemIcon().contains(mouseX, mouseY) && card.row().firstAttachment() != null) {
        return Optional.of(new TooltipModel(List.of(
            new TooltipLine.NativeItem(card.row().firstAttachment().item().itemId()))));
      }
    }
    return Optional.empty();
  }

  public static Optional<TooltipModel> tooltipAt(DeliveryState state, DeliveryLayout.Layout layout,
                                                 int mouseX, int mouseY) {
    for (DeliveryLayout.AttachmentCard card : layout.attachmentCards()) {
      if (card.card().contains(mouseX, mouseY)) {
        return Optional.of(new TooltipModel(List.of(new TooltipLine.NativeItem(card.attachment().item().itemId()))));
      }
    }
    return tooltipAt(layout, mouseX, mouseY);
  }
}
