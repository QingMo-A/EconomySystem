package com.mo.economy_system.ui.about;

import com.mo.economy_system.ui.animation.UiEasing;

/** Deterministic opening animation copied from the legacy About screen. */
public final class AboutOpenAnimation {
  public static final long DURATION_NANOS = 500_000_000L;
  public static final int PANEL_OFFSET = 50;

  private AboutOpenAnimation() {}

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

  public static int leftOffset(float easedProgress) {
    return (int) ((1.0f - clamp(easedProgress)) * -PANEL_OFFSET);
  }

  public static int rightOffset(float easedProgress) {
    return (int) ((1.0f - clamp(easedProgress)) * PANEL_OFFSET);
  }

  private static float clamp(float value) {
    return Math.max(0.0f, Math.min(1.0f, value));
  }
}
