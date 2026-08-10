package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.animation.UiEasing;

/** Opening motion used by the legacy shop screen. */
public final class ShopOpenAnimation {
  public static final long DURATION_NANOS = 420_000_000L;
  public static final int PANEL_OFFSET = 40;
  public static final int SEARCH_OFFSET = 30;

  private ShopOpenAnimation() {}

  public static float progressAt(long startedAtNanos, long nowNanos) {
    if (startedAtNanos < 0L) return 1.0f;
    long elapsed = nowNanos - startedAtNanos;
    if (elapsed <= 0L) return 0.0f;
    if (elapsed >= DURATION_NANOS) return 1.0f;
    return (float) elapsed / (float) DURATION_NANOS;
  }

  public static float easedProgressAt(long startedAtNanos, long nowNanos) {
    return UiEasing.easeOutCubic(progressAt(startedAtNanos, nowNanos));
  }

  public static int contentOffset(float progress) {
    return (int) ((1.0f - clamp(progress)) * PANEL_OFFSET);
  }

  public static int searchOffset(float progress) {
    return (int) ((1.0f - clamp(progress)) * SEARCH_OFFSET);
  }

  private static float clamp(float value) {
    return Math.max(0.0f, Math.min(1.0f, value));
  }
}
