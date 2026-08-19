package com.mo.economy_system.ui.theme;

/** Shared semantic colors and native-widget policy for every active text input. */
public record UiInputStyle(int textColor, int disabledTextColor, int placeholderColor,
                           boolean textShadow, boolean hideNativeBorder) {
  public UiInputStyle {
    if ((textColor & 0xFF000000) == 0 || (disabledTextColor & 0xFF000000) == 0
        || (placeholderColor & 0xFF000000) == 0) {
      throw new IllegalArgumentException("input colors must be opaque ARGB");
    }
  }
}
