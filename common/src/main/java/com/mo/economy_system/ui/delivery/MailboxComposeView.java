package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Set;

/** Shared semantic chrome for the player mail composer. Native widgets/items are drawn by targets. */
public final class MailboxComposeView {
  private MailboxComposeView() {}

  public static void render(EconomyUiRenderer renderer, MailboxComposeLayout.Layout layout,
                            Set<Integer> selectedSlots, boolean sending, MailboxSendStatus status,
                            int mouseX, int mouseY) {
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.DELIVERY, "screen.mailbox.compose.title", List.of(),
        layout.title().x() + 8, layout.title().y() + 5, layout.versionInfoScale(),
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.delivery_box.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);

    renderer.card(layout.formPanel(), EconomyUiTheme.DELIVERY_CARD, false);
    renderer.card(layout.inventoryPanel(), EconomyUiTheme.ABOUT_CARD, false);

    renderer.translatedText("screen.mailbox.compose.attachments",
        List.of(Integer.toString(selectedSlots.size()), Integer.toString(EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS)),
        layout.inventoryPanel().x() + 10, layout.inventoryPanel().y() + 12, EconomyUiTheme.TEXT_PRIMARY);

    for (MailboxComposeLayout.Slot slot : layout.slots()) {
      boolean selected = selectedSlots.contains(slot.slot());
      renderer.card(slot.rect(), selected ? EconomyUiTheme.DELIVERY_CARD : EconomyUiTheme.ABOUT_CARD,
          slot.rect().contains(mouseX, mouseY));
    }

    renderer.translatedButton(layout.sendButton(), EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
        "screen.mailbox.compose.send", List.of(), layout.sendButton().contains(mouseX, mouseY), !sending);
    renderer.translatedButton(layout.backButton(), EconomyUiTheme.DELIVERY_PAGE_BUTTON,
        "screen.mailbox.compose.back", List.of(), layout.backButton().contains(mouseX, mouseY), true);

    if (status != null && status != MailboxSendStatus.SUCCESS) {
      renderer.translatedTextInRect("message.mailbox.send." + status.id(), List.of(), layout.status(),
          EconomyUiTheme.TEXT_ERROR, UiTextAlignment.LEFT);
    }
  }

  public static void renderRecipientCompletion(EconomyUiRenderer renderer, MailboxComposeLayout.Layout layout,
                                               List<PlayerSummary> suggestions, int selection,
                                               int mouseX, int mouseY) {
    if (suggestions == null || suggestions.isEmpty()) return;
    int start = MailboxComposeLayout.completionWindowStart(suggestions.size(), selection);
    int count = Math.min(MailboxComposeLayout.COMPLETION_MAX_ROWS, suggestions.size() - start);
    var base = layout.completionDropdown();
    var dropdown = new com.mo.economy_system.ui.geometry.UiRect(
        base.x(), base.y(), base.width(), count * MailboxComposeLayout.COMPLETION_ROW_HEIGHT);
    renderer.card(dropdown, EconomyUiTheme.DELIVERY_CARD, false);
    for (int i = 0; i < count; i++) {
      int index = start + i;
      PlayerSummary player = suggestions.get(index);
      var row = new com.mo.economy_system.ui.geometry.UiRect(
          dropdown.x(), dropdown.y() + i * MailboxComposeLayout.COMPLETION_ROW_HEIGHT,
          dropdown.width(), MailboxComposeLayout.COMPLETION_ROW_HEIGHT);
      boolean hovered = row.contains(mouseX, mouseY);
      String text = player.playerName() + "  ·  " + player.playerId();
      var textRect = new com.mo.economy_system.ui.geometry.UiRect(
          row.x() + 8, row.y(), Math.max(1, row.width() - 12), row.height());
      renderer.textInRect(text, textRect,
          hovered || index == selection ? EconomyUiTheme.DELIVERY_ACCENT : EconomyUiTheme.TEXT_PRIMARY,
          UiTextAlignment.LEFT);
    }
  }
}
