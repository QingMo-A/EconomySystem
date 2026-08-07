package com.mo.economy_system.ui.geometry;

public record UiInsets(int top, int right, int bottom, int left) {
    public UiInsets {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("UI insets cannot be negative");
        }
    }

    public static UiInsets all(int value) {
        return new UiInsets(value, value, value, value);
    }
}
