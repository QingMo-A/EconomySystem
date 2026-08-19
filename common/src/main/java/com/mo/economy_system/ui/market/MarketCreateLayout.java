package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiVersionInfoLayout;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Stable virtual-coordinate layout for the market create forms. */
public final class MarketCreateLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int COMPLETION_ROW_HEIGHT = 18;
  public static final int COMPLETION_MAX_ROWS = 5;
  private MarketCreateLayout() {}

  /** Returns the first absolute suggestion index visible for a highlighted selection. */
  public static int completionWindowStart(int totalSuggestions, int selection) {
    if (totalSuggestions <= COMPLETION_MAX_ROWS || selection < 0) return 0;
    int maxStart = totalSuggestions - COMPLETION_MAX_ROWS;
    return Math.min(maxStart, Math.max(0, selection - COMPLETION_MAX_ROWS + 1));
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketCreateState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketCreateState state,
                                 UiTextMetrics metrics) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), pad = EconomyUiTheme.PANEL_PADDING;
    MarketCreateMode mode = state == null ? MarketCreateMode.SALES : state.mode();

    int panelY;
    int formX;
    int formWidth;
    int formHeight;
    int inventoryX = 0;
    int inventoryY = 0;
    int inventoryWidth = 0;
    int inventoryHeight = 0;

    if (mode == MarketCreateMode.DEMAND) {
      formWidth = Math.min(360, Math.max(180, width - pad * 2));
      formHeight = Math.min(238, Math.max(160, height - pad * 2));
      formX = Math.max(pad, (width - formWidth) / 2);
      panelY = Math.max(pad, (height - formHeight) / 2);
    } else {
      // Sales follows the same compact centered vertical language as player mail.
      formWidth = Math.min(420, Math.max(260, width - pad * 2));
      formX = Math.max(pad, (width - formWidth) / 2);
      formHeight = 118;
      panelY = 52;
      inventoryX = formX;
      inventoryY = panelY + formHeight + 8;
      inventoryWidth = formWidth;
      inventoryHeight = 124;
    }

    UiRect formPanel = new UiRect(formX, panelY, formWidth, formHeight);
    UiRect inventoryPanel = mode == MarketCreateMode.SALES
        ? new UiRect(inventoryX, inventoryY, inventoryWidth, inventoryHeight)
        : new UiRect(0, 0, 0, 0);

    List<Slot> slots = new ArrayList<>();
    if (mode == MarketCreateMode.SALES && state != null) {
      int slotSize = 22, gap = 3, cols = 9;
      int gridWidth = cols * slotSize + (cols - 1) * gap;
      int gridX = inventoryPanel.x() + Math.max(pad, (inventoryPanel.width() - gridWidth) / 2);
      int gridY = inventoryPanel.y() + 23;
      for (MarketInventoryItem item : state.inventory()) {
        int index = slots.size(), col = index % cols, row = index / cols;
        slots.add(new Slot(item, new UiRect(gridX + col * (slotSize + gap),
            gridY + row * (slotSize + gap), slotSize, slotSize)));
      }
    }

    int fieldWidth = Math.max(80, formWidth - pad * 2);
    int fieldX = formX + pad;
    UiRect itemId = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 66, fieldWidth, 20)
        : new UiRect(0, 0, 0, 0);
    UiRect completionDropdown = mode == MarketCreateMode.DEMAND
        ? new UiRect(itemId.x(), itemId.bottom() + 2, itemId.width(),
            COMPLETION_ROW_HEIGHT * COMPLETION_MAX_ROWS)
        : new UiRect(0, 0, 0, 0);

    UiRect quantity = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 94, Math.min(120, fieldWidth), 20)
        : new UiRect(fieldX, panelY + 70, Math.min(72, fieldWidth), 20);
    UiRect price = mode == MarketCreateMode.DEMAND
        ? new UiRect(fieldX, panelY + 122, Math.min(140, fieldWidth), 20)
        : new UiRect(fieldX, panelY + 94, Math.min(150, fieldWidth), 20);

    int adjustY = quantity.y() - 1;
    UiRect decrement = mode == MarketCreateMode.SALES
        ? new UiRect(quantity.x() + quantity.width() + 8, adjustY, 30, 22)
        : new UiRect(0, 0, 0, 0);
    UiRect increment = mode == MarketCreateMode.SALES
        ? new UiRect(decrement.right() + 4, adjustY, 30, 22)
        : new UiRect(0, 0, 0, 0);
    UiRect all = mode == MarketCreateMode.SALES
        ? new UiRect(increment.right() + 4, adjustY, 48, 22)
        : new UiRect(0, 0, 0, 0);

    UiRect submit = mode == MarketCreateMode.SALES
        ? new UiRect(formPanel.right() - 14 - 120, inventoryPanel.bottom() + 7, 120, 22)
        : new UiRect(formX + (formWidth - 120) / 2, panelY + formHeight - 36, 120, 22);

    String footerKey = mode == MarketCreateMode.SALES
        ? "screen.market.create.sales_title" : "screen.market.create.demand_title";
    UiVersionInfoLayout.Result footer = UiVersionInfoLayout.calculate(
        metrics, footerKey, List.of(), pad, height - pad, 200);
    UiRect title = footer.card();
    UiRect esc = new UiRect(Math.max(pad, width - pad - 90),
        height - pad - Math.max(1, metrics.lineHeight()), 90, Math.max(1, metrics.lineHeight()));
    UiRect back = new UiRect(0, 0, 0, 0);
    UiRect message = mode == MarketCreateMode.SALES
        ? new UiRect(formPanel.x() + 14, submit.y() + 2,
            Math.max(1, submit.x() - formPanel.x() - 28), 18)
        : new UiRect(formX + pad, panelY + 168, Math.max(1, formWidth - pad * 2), 28);

    return new Layout(scale, title, footer.contentScale(), esc, inventoryPanel, formPanel, slots,
        itemId, completionDropdown, quantity, price, decrement, increment, all, submit, back, message);
  }

  public record Layout(UiScale scale, UiRect title, float versionInfoScale, UiRect esc,
                       UiRect inventoryPanel, UiRect formPanel,
                       List<Slot> slots, UiRect itemId, UiRect completionDropdown,
                       UiRect quantity, UiRect price,
                       UiRect decrement, UiRect increment, UiRect all, UiRect submit,
                       UiRect back, UiRect message) {
    public Layout { slots = List.copyOf(slots); }

    /** Compatibility constructor for callers that predate the completion dropdown geometry. */
    public Layout(UiScale scale, UiRect title, UiRect inventoryPanel, UiRect formPanel,
                  List<Slot> slots, UiRect itemId, UiRect quantity, UiRect price,
                  UiRect decrement, UiRect increment, UiRect all, UiRect submit,
                  UiRect back, UiRect message) {
      this(scale, title, 1.0f, new UiRect(0, 0, 0, 0), inventoryPanel, formPanel, slots, itemId,
          new UiRect(0, 0, 0, 0), quantity, price, decrement, increment, all, submit, back,
          message);
    }

    /** Compatibility constructor for callers using the pre-footer canonical geometry. */
    public Layout(UiScale scale, UiRect title, UiRect inventoryPanel, UiRect formPanel,
                  List<Slot> slots, UiRect itemId, UiRect completionDropdown,
                  UiRect quantity, UiRect price, UiRect decrement, UiRect increment, UiRect all,
                  UiRect submit, UiRect back, UiRect message) {
      this(scale, title, 1.0f, new UiRect(0, 0, 0, 0), inventoryPanel, formPanel, slots,
          itemId, completionDropdown, quantity, price, decrement, increment, all, submit, back,
          message);
    }
  }

  public record Slot(MarketInventoryItem item, UiRect rect) {}
}
