package com.mo.economy_system.ui.animation;

/** Loader-neutral easing functions used by deterministic UI animation specs. */
public final class UiEasing {
    private UiEasing() {
    }

    public static float easeOutCubic(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        return 1.0f - (float) Math.pow(1.0f - clamped, 3.0f);
    }
}
