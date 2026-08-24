package com.mo.economy_system.ui.component;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Shared item-slot chrome and state rendering used by inventory-like EconomySystem screens. */
public final class UiItemSlot {
  public enum State {
    EMPTY,
    NORMAL,
    HOVERED,
    SELECTED,
    CLAIMED,
    DISABLED
  }

  private UiItemSlot() {}

  public static void render(EconomyUiRenderer renderer, UiRect slot, State state, int accent) {
    int background = switch (state) {
      case HOVERED -> EconomyUiTheme.Surface.ITEM_SLOT_HOVER;
      case DISABLED -> EconomyUiTheme.Surface.ITEM_SLOT_DISABLED;
      default -> EconomyUiTheme.Surface.ITEM_SLOT;
    };
    renderer.fill(slot, background);

    if (state == State.HOVERED) {
      border(renderer, slot, EconomyUiTheme.Surface.HAIRLINE_HOVER);
    } else if (state == State.SELECTED) {
      // Selection is chrome-only: keep the item/background untouched and use a restrained
      // three-step glow around the slot instead of a solid accent stripe.
      if (slot.width() > 2 && slot.height() > 2) {
        border(renderer, new UiRect(slot.x() - 1, slot.y() - 1,
            slot.width() + 2, slot.height() + 2), withAlpha(accent, 0x40));
      }
      border(renderer, slot, accent);
      if (slot.width() > 4 && slot.height() > 4) {
        border(renderer, new UiRect(slot.x() + 1, slot.y() + 1,
            slot.width() - 2, slot.height() - 2), withAlpha(accent, 0x66));
      }
    }
  }

  public static void renderItem(EconomyUiRenderer renderer, UiRect slot, UiRect itemRect,
                                String itemId, int count, State state, int accent) {
    render(renderer, slot, state, accent);
    renderer.itemWithCount(itemId, count, itemRect);
    if (state == State.CLAIMED) {
      renderer.claimedItemOverlay(new UiRect(
          slot.x() + 3, slot.y() + 3,
          Math.max(1, slot.width() - 6), Math.max(1, slot.height() - 6)));
    }
  }

  private static void border(EconomyUiRenderer renderer, UiRect rect, int color) {
    if (rect.width() <= 0 || rect.height() <= 0) return;
    renderer.fill(new UiRect(rect.x(), rect.y(), rect.width(), 1), color);
    renderer.fill(new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), color);
    renderer.fill(new UiRect(rect.x(), rect.y(), 1, rect.height()), color);
    renderer.fill(new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), color);
  }

  private static int withAlpha(int argbOrRgb, int alpha) {
    return ((alpha & 0xFF) << 24) | (argbOrRgb & 0x00FFFFFF);
  }
}
