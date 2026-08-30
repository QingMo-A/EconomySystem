package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiVersionInfoLayout;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Shared virtual-coordinate layout for the player mail composer. */
public final class MailboxComposeLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int COMPLETION_ROW_HEIGHT = 18;
  public static final int COMPLETION_MAX_ROWS = 5;

  private MailboxComposeLayout() {}

  public static int completionWindowStart(int totalSuggestions, int selection) {
    if (totalSuggestions <= COMPLETION_MAX_ROWS || selection < 0) return 0;
    int maxStart = totalSuggestions - COMPLETION_MAX_ROWS;
    return Math.min(maxStart, Math.max(0, selection - COMPLETION_MAX_ROWS + 1));
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, UiTextMetrics metrics) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int contentWidth = Math.min(420, Math.max(260, width - panel * 2));
    int contentX = Math.max(panel, (width - contentWidth) / 2);
    int top = 52;

    // Compose stays vertical but no longer stretches across the whole viewport.
    UiRect formPanel = new UiRect(contentX, top, contentWidth, 116);
    UiRect inventoryPanel = new UiRect(contentX, top + 124, contentWidth, 124);
    int innerX = formPanel.x() + 14;
    int inputWidth = Math.max(160, formPanel.width() - 28);
    UiRect recipient = new UiRect(innerX, formPanel.y() + 8, inputWidth, 18);
    UiRect subject = new UiRect(innerX, formPanel.y() + 31, inputWidth, 18);
    UiRect body = new UiRect(innerX, formPanel.y() + 54, inputWidth, 18);
    UiRect money = new UiRect(innerX, formPanel.y() + 77, inputWidth, 18);
    UiRect completionDropdown = new UiRect(recipient.x(), recipient.bottom() + 2,
        recipient.width(), COMPLETION_ROW_HEIGHT * COMPLETION_MAX_ROWS);

    List<Slot> slots = new ArrayList<>();
    int slotSize = 22, slotGap = 3, cols = 9;
    int gridWidth = cols * slotSize + (cols - 1) * slotGap;
    int slotStartX = inventoryPanel.x() + Math.max(14, (inventoryPanel.width() - gridWidth) / 2);
    int slotStartY = inventoryPanel.y() + 23;
    for (int slot = 0; slot < 36; slot++) {
      int col = slot % cols, row = slot / cols;
      slots.add(new Slot(slot, new UiRect(slotStartX + col * (slotSize + slotGap),
          slotStartY + row * (slotSize + slotGap), slotSize, slotSize)));
    }

    int actionY = inventoryPanel.bottom() + 7;
    UiRect send = new UiRect(contentX + 14, actionY, 112, 22);
    UiRect back = new UiRect(contentX + contentWidth - 14 - 96, actionY, 96, 22);
    UiRect status = new UiRect(send.right() + 8, actionY + 4,
        Math.max(1, back.x() - send.right() - 16), 14);

    UiVersionInfoLayout.Result versionInfo = UiVersionInfoLayout.calculate(metrics,
        "screen.mailbox.compose.title", List.of(), panel, height - panel, 150);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90),
        height - panel - metrics.lineHeight(), 90, metrics.lineHeight());
    return new Layout(scale, versionInfo.card(), versionInfo.contentScale(), esc, formPanel, inventoryPanel,
        recipient, subject, body, money, completionDropdown, send, back, status, List.copyOf(slots), metrics);
  }

  public record Layout(UiScale scale, UiRect title, float versionInfoScale, UiRect esc,
                       UiRect formPanel, UiRect inventoryPanel,
                       UiRect recipient, UiRect subject, UiRect body, UiRect money, UiRect completionDropdown,
                       UiRect sendButton, UiRect backButton, UiRect status,
                       List<Slot> slots, UiTextMetrics metrics) {}
  public record Slot(int slot, UiRect rect) {}
}
