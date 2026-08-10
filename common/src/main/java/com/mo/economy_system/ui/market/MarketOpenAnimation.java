package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.animation.UiEasing;

/** Opening motion shared by the market list shell and target widgets. */
public final class MarketOpenAnimation {
  public static final long DURATION_NANOS = 420_000_000L;
  public static final int PANEL_OFFSET = 40;
  public static final int TOP_BUTTON_OFFSET = 30;
  public static final int SEARCH_OFFSET = 30;

  private MarketOpenAnimation() {}

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

  public static int topButtonOffset(float progress) {
    return (int) ((1.0f - clamp(progress)) * TOP_BUTTON_OFFSET);
  }

  public static int searchOffset(float progress) {
    return (int) ((1.0f - clamp(progress)) * SEARCH_OFFSET);
  }

  private static float clamp(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
}
