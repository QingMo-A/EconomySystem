package com.mo.economy_system.ui.geometry;

/** Stable fit-to-viewport scale used by every target renderer. */
public record UiScale(float value, int virtualWidth, int virtualHeight) {
    public UiScale {
        if (!(value > 0.0f) || virtualWidth < 1 || virtualHeight < 1) {
            throw new IllegalArgumentException("Invalid UI scale");
        }
    }

    public static UiScale fit(int physicalWidth, int physicalHeight, int baseWidth, int baseHeight) {
        if (physicalWidth < 1 || physicalHeight < 1 || baseWidth < 1 || baseHeight < 1) {
            throw new IllegalArgumentException("UI dimensions must be positive");
        }
        float value = Math.min((float) physicalWidth / baseWidth, (float) physicalHeight / baseHeight);
        return new UiScale(value, Math.max(1, Math.round(physicalWidth / value)),
                Math.max(1, Math.round(physicalHeight / value)));
    }

    public int toVirtualX(double physicalX) {
        return Math.round((float) (physicalX / value));
    }

    public int toVirtualY(double physicalY) {
        return Math.round((float) (physicalY / value));
    }
}
