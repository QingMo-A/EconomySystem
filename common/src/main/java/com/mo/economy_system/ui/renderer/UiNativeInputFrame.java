package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;

/** Physical-pixel geometry for the frame surrounding a target-owned native input widget. */
public final class UiNativeInputFrame {
  private static final int HORIZONTAL_INSET = 4;
  private static final int VERTICAL_INSET = 2;

  private UiNativeInputFrame() {}

  /** Returns the exact legacy frame rectangle around a native widget rectangle. */
  public static UiRect frameRect(UiRect nativeWidgetRect) {
    if (nativeWidgetRect == null) throw new IllegalArgumentException("native widget rect");
    return new UiRect(nativeWidgetRect.x() - HORIZONTAL_INSET,
        nativeWidgetRect.y() - VERTICAL_INSET,
        nativeWidgetRect.width() + HORIZONTAL_INSET * 2,
        nativeWidgetRect.height() + VERTICAL_INSET * 2);
  }

  /** Paints the common frame while the target is still in physical coordinates. */
  public static void render(EconomyUiRenderer renderer, UiRect nativeWidgetRect,
                            UiInputFrameStyle style, boolean focused) {
    if (renderer == null || style == null) throw new IllegalArgumentException("frame renderer/style");
    renderer.inputFrame(frameRect(nativeWidgetRect), style, focused);
  }
}
