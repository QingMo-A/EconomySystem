package com.mo.economy_system.ui.home;

import com.mo.economy_system.ui.animation.UiEasing;

/** Deterministic opening animation shared by both Minecraft target shells. */
public final class HomeOpenAnimation {
    public static final long DURATION_NANOS = 500_000_000L;
    public static final int PANEL_OFFSET = 50;

    private HomeOpenAnimation() {
    }

    /** Returns the clamped linear progress for a monotonic clock sample. */
    public static float progressAt(long startedAtNanos, long nowNanos) {
        if (startedAtNanos < 0L) return 1.0f;
        long elapsed = nowNanos - startedAtNanos;
        if (elapsed <= 0L) return 0.0f;
        if (elapsed >= DURATION_NANOS) return 1.0f;
        return (float) elapsed / (float) DURATION_NANOS;
    }

    /** Cubic ease-out progress used by the legacy Home screen. */
    public static float easedProgressAt(long startedAtNanos, long nowNanos) {
        return UiEasing.easeOutCubic(progressAt(startedAtNanos, nowNanos));
    }

    public static int leftOffset(float easedProgress) {
        return (int) ((1.0f - Math.max(0.0f, Math.min(1.0f, easedProgress))) * -PANEL_OFFSET);
    }

    public static int rightOffset(float easedProgress) {
        return (int) ((1.0f - Math.max(0.0f, Math.min(1.0f, easedProgress))) * PANEL_OFFSET);
    }

    public static int leftOffsetAt(float progress) {
        return leftOffset(UiEasing.easeOutCubic(progress));
    }

    public static int rightOffsetAt(float progress) {
        return rightOffset(UiEasing.easeOutCubic(progress));
    }
}
