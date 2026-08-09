package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Stable virtual-coordinate layout for the market create forms. */
public final class MarketCreateLayout {
  private MarketCreateLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketCreateState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), pad = EconomyUiTheme.PANEL_PADDING;
    int panelWidth = Math.max(180, Math.min(300, (width - pad * 3) / 2));
    int leftX = Math.max(pad, (width - panelWidth * 2 - pad) / 2);
    int rightX = leftX + panelWidth + pad;
    int panelY = 48, panelHeight = Math.max(160, height - 94);
    List<Slot> slots = new ArrayList<>();
    if (state.mode() == MarketCreateMode.SALES) {
      int slotSize = 24, gap = 5;
      for (MarketInventoryItem item : state.inventory()) {
        int index = slots.size(), col = index % 9, row = index / 9;
        slots.add(new Slot(item, new UiRect(leftX + pad + col * (slotSize + gap), panelY + 34 + row * (slotSize + gap), slotSize, slotSize)));
      }
    }
    UiRect itemId = new UiRect(rightX + 96, panelY + 52, Math.max(80, panelWidth - 108), 20);
    UiRect quantity = new UiRect(rightX + 96, panelY + 84, Math.max(52, panelWidth - 108), 20);
    UiRect price = new UiRect(rightX + 96, panelY + 116, Math.max(52, panelWidth - 108), 20);
    UiRect decrement = new UiRect(rightX + 96, panelY + 146, 28, 20);
    UiRect increment = new UiRect(rightX + 128, panelY + 146, 28, 20);
    UiRect all = new UiRect(rightX + 160, panelY + 146, 48, 20);
    UiRect submit = new UiRect(rightX + (panelWidth - 124) / 2, panelY + panelHeight - 30, 124, 22);
    UiRect back = new UiRect(pad, height - pad - 18, 80, 18);
    UiRect title = new UiRect(pad, pad, Math.max(1, width - pad * 2), 18);
    UiRect message = new UiRect(rightX + 8, panelY + 170, panelWidth - 16, 28);
    return new Layout(scale, title, new UiRect(leftX, panelY, panelWidth, panelHeight),
        new UiRect(rightX, panelY, panelWidth, panelHeight), slots, itemId, quantity, price,
        decrement, increment, all, submit, back, message);
  }

  public record Layout(UiScale scale, UiRect title, UiRect inventoryPanel, UiRect formPanel,
                       List<Slot> slots, UiRect itemId, UiRect quantity, UiRect price,
                       UiRect decrement, UiRect increment, UiRect all, UiRect submit,
                       UiRect back, UiRect message) {
    public Layout { slots = List.copyOf(slots); }
  }

  public record Slot(MarketInventoryItem item, UiRect rect) {}
}
