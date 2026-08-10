package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Stable virtual-coordinate layout for the market create forms. */
public final class MarketCreateLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private MarketCreateLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketCreateState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), pad = EconomyUiTheme.PANEL_PADDING;
    MarketCreateMode mode = state == null ? MarketCreateMode.SALES : state.mode();
    int panelY, panelHeight, leftX, rightX, leftWidth, rightWidth;
    if (mode == MarketCreateMode.DEMAND) {
      rightWidth = Math.min(420, Math.max(180, width - pad * 2));
      panelHeight = Math.min(238, Math.max(160, height - pad * 2));
      leftWidth = 0;
      leftX = 0;
      rightX = Math.max(pad, (width - rightWidth) / 2);
      panelY = Math.max(pad, (height - panelHeight) / 2);
    } else {
      leftWidth = Math.min(292, Math.max(160, (width - pad * 3) / 2));
      rightWidth = Math.min(300, Math.max(180, width - leftWidth - pad * 3));
      int contentWidth = leftWidth + pad + rightWidth;
      leftX = Math.max(pad, (width - contentWidth) / 2);
      rightX = leftX + leftWidth + pad;
      panelY = 52;
      panelHeight = Math.min(252, Math.max(160, height - 88));
    }
    List<Slot> slots = new ArrayList<>();
    if (mode == MarketCreateMode.SALES && state != null) {
      int slotSize = 24, gap = 4;
      for (MarketInventoryItem item : state.inventory()) {
        int index = slots.size(), col = index % 9, row = index / 9;
        int gridWidth = 9 * slotSize + 8 * gap;
        int gridX = leftX + Math.max(pad, (leftWidth - gridWidth) / 2);
        slots.add(new Slot(item, new UiRect(gridX + col * (slotSize + gap), panelY + 34 + row * (slotSize + gap), slotSize, slotSize)));
      }
    }
    int labelWidth = 72;
    int fieldWidth = Math.max(80, rightWidth - pad * 2 - labelWidth);
    int fieldX = rightX + pad + labelWidth;
    UiRect itemId = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 84, fieldWidth, 20) : new UiRect(fieldX, panelY + 52, 0, 0);
    UiRect quantity = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 112, Math.min(120, fieldWidth), 20)
        : new UiRect(fieldX, panelY + 82, Math.min(58, fieldWidth), 20);
    UiRect price = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 140, Math.min(140, fieldWidth), 20)
        : new UiRect(fieldX, panelY + 112, Math.min(150, fieldWidth), 20);
    int adjustY = quantity.y() - 1;
    UiRect decrement = mode == MarketCreateMode.SALES ? new UiRect(quantity.x() + quantity.width() + 8, adjustY, 30, 22) : new UiRect(0, 0, 0, 0);
    UiRect increment = mode == MarketCreateMode.SALES ? new UiRect(decrement.right() + 4, adjustY, 30, 22) : new UiRect(0, 0, 0, 0);
    UiRect all = mode == MarketCreateMode.SALES ? new UiRect(increment.right() + 4, adjustY, 48, 22) : new UiRect(0, 0, 0, 0);
    UiRect submit = new UiRect(rightX + (rightWidth - 120) / 2, panelY + panelHeight - 36, 120, 22);
    UiRect back = new UiRect(pad, height - pad - 18, 80, 18);
    UiRect title = new UiRect(pad, pad + 12, 240, 24);
    UiRect message = new UiRect(rightX + pad, panelY + (mode == MarketCreateMode.DEMAND ? 168 : 178), Math.max(1, rightWidth - pad * 2), 28);
    UiRect inventoryPanel = mode == MarketCreateMode.SALES ? new UiRect(leftX, panelY, leftWidth, panelHeight) : new UiRect(0, 0, 0, 0);
    return new Layout(scale, title, inventoryPanel,
        new UiRect(rightX, panelY, rightWidth, panelHeight), slots, itemId, quantity, price,
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
